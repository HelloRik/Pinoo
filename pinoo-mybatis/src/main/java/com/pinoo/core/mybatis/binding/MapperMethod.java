package com.pinoo.core.mybatis.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinoo.core.mybatis.ConfigurationExtend;
import com.pinoo.core.mybatis.cache.ICache;

/**
 * mybatis MapperMethod 扩展
 * 
 * @Filename: MapperMethod.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public class MapperMethod {

    private Logger logger = LoggerFactory.getLogger(MapperMethod.class);

    private final Class<?> mapperInterface;

    private final SqlCommand command;

    private final MethodSignature method;

    private final ICache cache;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.mapperInterface = mapperInterface;
        this.method = new MethodSignature(config, mapperInterface, method);
        this.command = new SqlCommand(config, mapperInterface, method);
        this.cache = ((ConfigurationExtend) config).getCache();
    }

    public Object execute(SqlSession sqlSession, Object[] args) throws Exception {

        Object result;
        if (SqlCommandType.INSERT == command.getType()) {
            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.insert(command.getName(), param));
            if (checkResult(result)) {
                long id = (Long) method.getPrimaryFieldInfo().getReadMethod().invoke(args[0]);
                if (id > 0) {
                    cache.setObject(formatObjectKey(id), args[0]);
                    addToList(args[0]);
                }
            }
        } else if (SqlCommandType.UPDATE == command.getType()) {
            Object id = method.getPrimaryFieldInfo().getReadMethod().invoke(args[0]);
            String objKey = formatObjectKey(id);
            Object oldModel = getObject(objKey, sqlSession, id);

            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.update(command.getName(), param));

            if (checkResult(result)) {
                if (cache.hasKey(objKey)) {
                    cache.setObject(objKey, args[0]);
                }
                removeToList(oldModel);
                addToList(args[0]);
            }
        } else if (SqlCommandType.DELETE == command.getType()) {
            Object id = args[0];
            String objKey = formatObjectKey(id);
            Object oldModel = getObject(objKey, sqlSession, id);

            Object param = method.convertArgsToSqlCommandParam(args);
            result = rowCountResult(sqlSession.delete(command.getName(), param));
            if (checkResult(result)) {
                cache.removeObject(objKey);
                removeToList(oldModel);
            }
        } else if (SqlCommandType.SELECT == command.getType()) {
            String cacheKey = method.convertArgsToCacheKey(args);
            System.out.println("@@@@@@cache key :" + cacheKey);
            if (method.returnsVoid() && method.hasResultHandler()) {
                executeWithResultHandler(sqlSession, args);
                result = null;
            } else if (method.returnsMany()) {
                if (!cache.hasKey(cacheKey)) {
                    System.out.println("###load list from db");
                    result = executeForMany(sqlSession, args);
                    cache.setList(cacheKey, (List) result, method.getPrimaryFieldInfo(), method.getSortFieldInfo());
                }
                long cursor = method.getCursorIndex() >= 0 ? (Long) args[method.getCursorIndex()] : -1;
                int page = method.getPageIndex() >= 0 ? (Integer) args[method.getPageIndex()] : -1;
                int pageSize = method.getSizeIndex() >= 0 ? (Integer) args[method.getSizeIndex()] : -1;
                result = cache.getList(cacheKey, cursor, page, pageSize);
                if (method.returnsObjectList()) {
                    List<Long> ids = (List<Long>) result;
                    List<Object> objs = new ArrayList<Object>();
                    for (Long id : ids) {
                        Object o = getObject(formatObjectKey(id), sqlSession, id);
                        objs.add(o);
                    }
                    result = objs;
                }
            } else if (method.returnsMap()) {
                result = executeForMap(sqlSession, args);
            } else if (method.returnsCount()) {
                int count = cache.getCount(cacheKey);
                if (count <= 0) {
                    Object param = method.convertArgsToSqlCommandParam(args);
                    count = sqlSession.selectOne(command.getName(), param);
                    if (count > 0)
                        cache.setCount(cacheKey, count);
                }
                result = count;
            } else {
                if (method.getMethodName().equals("load") && method.getParamSize() == 1) {
                    result = getObject(cacheKey, sqlSession, args[0]);
                } else {
                    result = cache.getObject(cacheKey);
                    if (result == null) {
                        Object param = method.convertArgsToSqlCommandParam(args);
                        result = sqlSession.selectOne(command.getName(), param);
                        cache.setObject(cacheKey, result);
                    }
                }
            }
        } else {
            throw new BindingException("Unknown execution method for: " + command.getName());
        }
        if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
            throw new BindingException("Mapper method '" + command.getName()
                    + " attempted to return null from a method with a primitive return type (" + method.getReturnType()
                    + ").");
        }
        return result;
    }

    private String formatObjectKey(Object id) {
        return String.format(method.getObjectFormat(), id);
    }

    private void addToList(Object obj) throws Exception {
        List<String> unformatKeys = method.getUnformatCacheKeys();
        for (String key : unformatKeys) {
            String cacheKey = method.formartCacheKey(obj, key);
            if (cacheKey.startsWith(method.getListFormat()))
                cache.addToList(cacheKey, obj, method.getPrimaryFieldInfo(), method.getSortFieldInfo());
            else
                cache.increaseCount(cacheKey, 1);
        }
    }

    private void removeToList(Object obj) throws Exception {
        List<String> unformatKeys = method.getUnformatCacheKeys();
        for (String key : unformatKeys) {
            String cacheKey = method.formartCacheKey(obj, key);
            if (cacheKey.startsWith(method.getListFormat()))
                cache.removeToList(cacheKey, method.getPrimaryFieldInfo().getReadMethod().invoke(obj));
            else
                cache.decreaseCount(cacheKey, 1);
        }
    }

    private Object getObject(String cacheKey, SqlSession sqlSession, Object id) {
        Object result = cache.getObject(cacheKey);
        if (result == null) {
            result = sqlSession.selectOne(method.getLoadMappedStatementId(), id);
            cache.setObject(cacheKey, result);
        }
        return result;
    }

    private boolean checkResult(Object result) {
        final boolean flag;
        if (method.returnsVoid()) {
            flag = true;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            flag = ((Integer) result) > 0 ? true : false;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            flag = ((Long) result) > 0 ? true : false;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            flag = ((Boolean) result);
        } else {
            throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: "
                    + method.getReturnType());
        }
        return flag;
    }

    private Object rowCountResult(int rowCount) {
        final Object result;
        if (method.returnsVoid()) {
            result = null;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            result = rowCount;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            result = (long) rowCount;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            result = (rowCount > 0);
        } else {
            throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: "
                    + method.getReturnType());
        }
        return result;
    }

    private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
        if (void.class.equals(ms.getResultMaps().get(0).getType())) {
            throw new BindingException("method " + command.getName()
                    + " needs either a @ResultMap annotation, a @ResultType annotation,"
                    + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
        }
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
        } else {
            sqlSession.select(command.getName(), param, method.extractResultHandler(args));
        }
    }

    private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
        List<E> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.<E> selectList(command.getName(), param, rowBounds);
        } else {
            result = sqlSession.<E> selectList(command.getName(), param);
        }
        // issue #510 Collections & arrays support
        if (!method.getReturnType().isAssignableFrom(result.getClass())) {
            if (method.getReturnType().isArray()) {
                return convertToArray(result);
            } else {
                return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
            }
        }
        return result;
    }

    private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
        Object collection = config.getObjectFactory().create(method.getReturnType());
        MetaObject metaObject = config.newMetaObject(collection);
        metaObject.addAll(list);
        return collection;
    }

    @SuppressWarnings("unchecked")
    private <E> E[] convertToArray(List<E> list) {
        E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
        array = list.toArray(array);
        return array;
    }

    private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
        Map<K, V> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.<K, V> selectMap(command.getName(), param, method.getMapKey(), rowBounds);
        } else {
            result = sqlSession.<K, V> selectMap(command.getName(), param, method.getMapKey());
        }
        return result;
    }

    public static class ParamMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -2212268410512043556L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }

    }

    public static class SqlCommand {

        private final String name;

        private final SqlCommandType type;

        public SqlCommand(Configuration configuration, Class<?> declaringInterface, Method method)
                throws BindingException {
            name = declaringInterface.getName() + "." + method.getName();
            final MappedStatement ms;
            try {
                ms = configuration.getMappedStatement(name);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BindingException("Invalid bound statement (not found): " + name, e);
            }
            type = ms.getSqlCommandType();
            if (type == SqlCommandType.UNKNOWN) {
                throw new BindingException("Unknown execution method for: " + name);
            }
        }

        public String getName() {
            return name;
        }

        public SqlCommandType getType() {
            return type;
        }
    }

}