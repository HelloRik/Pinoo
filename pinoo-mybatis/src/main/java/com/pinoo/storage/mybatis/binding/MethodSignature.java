package com.pinoo.storage.mybatis.binding;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinoo.beans.FieldInfo;
import com.pinoo.common.utils.AnnotationScaner;
import com.pinoo.common.utils.ReflectionUtil;
import com.pinoo.storage.mybatis.annotation.method.MethodParam;
import com.pinoo.storage.mybatis.annotation.method.Page;
import com.pinoo.storage.mybatis.annotation.method.PageCursor;
import com.pinoo.storage.mybatis.annotation.method.PageSize;
import com.pinoo.storage.mybatis.annotation.model.Column;
import com.pinoo.storage.mybatis.annotation.model.ModelInfo;
import com.pinoo.storage.mybatis.annotation.model.PrimaryKey;
import com.pinoo.storage.mybatis.annotation.model.Sort;
import com.pinoo.storage.mybatis.binding.MapperMethod.ParamMap;
import com.pinoo.storage.mybatis.builder.SqlMapperXmlBuilder;

/**
 * 为每个方法定义一个解析
 * 
 * @Filename: MethodSignature.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public class MethodSignature {

    private Logger logger = LoggerFactory.getLogger(MethodSignature.class);

    private final Class<?> mapperInterface;

    private final Method method;

    private Configuration configuration;

    private final boolean returnsMany;

    private final boolean returnsMap;

    private final boolean returnsVoid;

    private final boolean returnsCount;

    private final boolean returnsObject;

    private final boolean returnsObjectList;

    private final Class<?> returnType;

    private boolean paramsId;

    private boolean paramsObject;

    // private boolean paramsMany;

    private boolean paramsMap;

    private int paramSize;

    private final String mapKey;

    private final Integer resultHandlerIndex;

    private final Integer rowBoundsIndex;

    private final SortedMap<Integer, String> params;

    private final boolean hasNamedParameters;

    protected static final String listCacheKeySign = "_list_";

    protected static final String countCacheKeySign = "_count_";

    protected static final String listCountCacheKeySign = "_list_count_%s_%s";

    protected static final String objectCacheKeySign = "_object_%s";

    // protected static final String combinedCacheKeySign = "_combined_";

    protected PropertyDescriptor[] propertyDescriptors;

    protected Class<?> entityClass;

    protected String tableName;

    private String methodName;

    // 所有的列信息
    private List<FieldInfo> fields = new ArrayList<FieldInfo>();

    private FieldInfo primaryFieldInfo;

    private FieldInfo sortFieldInfo;

    private String loadMappedStatementId;

    private String insertMappedStatementId;

    private String updateMappedStatementId;

    private String deleteMappedStatementId;

    private String objectFormat;

    private String listFormat;

    private String countFormat;

    private int cursorIndex;

    private int pageIndex;

    private int sizeIndex;

    private final static Map<Class<?>, String> syncMapperStatementInitMap = new ConcurrentHashMap<Class<?>, String>();

    // 未被格式化的CacheKey。
    private final static Map<Class<?>, List<String>> unformatCacheKeys = new ConcurrentHashMap<Class<?>, List<String>>();

    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method)
            throws BindingException {
        this.mapperInterface = mapperInterface;
        this.method = method;
        this.methodName = method.getName();
        this.configuration = configuration;
        this.returnType = method.getReturnType();
        this.returnsVoid = void.class.equals(this.returnType);
        this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
        this.returnsCount = ReflectionUtil.isMethodReturnCount(method);
        this.mapKey = getMapKey(method);
        this.returnsMap = (this.mapKey != null);
        this.returnsObject = (!this.returnsMany && !this.returnsCount && !this.returnsMap);
        Class<?> returnGenricType = ReflectionUtil.getMethodReturnGenricType(method, 0);
        this.returnsObjectList = (returnGenricType != null && !returnGenricType.equals(Object.class) && this.returnsMany);
        this.hasNamedParameters = hasNamedParams(method);
        this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
        this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
        this.params = Collections.unmodifiableSortedMap(getParams(method, this.hasNamedParameters));
        init();
    }

    public String convertArgsToCacheKey(Object[] args) {
        final int paramCount = params.size();

        String cacheKey = null;
        if (returnsVoid() && hasResultHandler()) {

        } else if (returnsMany()) {
            cacheKey = this.listFormat;
        } else if (returnsMap()) {
        } else if (returnsCount()) {
            cacheKey = this.countFormat;
        } else {
            return String.format(this.objectFormat, args[0]);
        }

        if (!hasNamedParameters && paramCount == 1) {
            int index = params.keySet().iterator().next();
            cacheKey += index + "_" + args[index];
        } else {
            for (Map.Entry<Integer, String> entry : params.entrySet()) {
                int index = entry.getKey();
                if (index != this.cursorIndex && index != this.pageIndex && index != this.sizeIndex)
                    cacheKey += "_" + entry.getValue() + "_" + args[entry.getKey()];
            }
        }
        return cacheKey;
    }

    public Object convertArgsToSqlCommandParam(Object[] args) {
        final int paramCount = params.size();
        if (args == null || paramCount == 0) {
            return null;
        } else if (!hasNamedParameters && paramCount == 1) {
            return args[params.keySet().iterator().next()];
        } else {
            final Map<String, Object> param = new ParamMap<Object>();
            int i = 0;
            for (Map.Entry<Integer, String> entry : params.entrySet()) {

                param.put(entry.getValue(), args[entry.getKey()]);
                // issue #71, add param names as param1, param2...but ensure
                // backward compatibility
                final String genericParamName = "param" + String.valueOf(i + 1);
                if (!param.containsKey(genericParamName)) {
                    param.put(genericParamName, args[entry.getKey()]);
                }
                i++;
            }
            return param;
        }
    }

    private void init() {
        try {
            initModelInfo();
            initFields();
            initPrimaryField();
            initSortField();
            initMethodParams();
            initCacheFormat();
            initMapperStatement();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("init mapper method error!!!");
        }
    }

    private void initFields() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    String name = fieldName;
                    String dbName = name;
                    Method writeMethod = one.getWriteMethod();
                    Method readMethod = one.getReadMethod();

                    Column columnKey = field.getAnnotation(Column.class);
                    if (columnKey != null && StringUtils.isNotEmpty(columnKey.dbName())) {
                        dbName = columnKey.dbName();
                    }

                    FieldInfo fieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                    fields.add(fieldInfo);
                }
            }
        }
    }

    private void initMethodParams() {
        this.paramSize = method.getParameterTypes().length;
        // if (paramSize == 1) {
        // Annotation[] annotations = method.getParameterAnnotations()[0];
        // for (int i = 0; i < annotations.length; i++) {
        // Annotation one = annotations[i];
        // if (one instanceof ParamType) {
        // ParamType type = (ParamType) one;
        // EnumParamType paramType = type.type();
        // if (paramType.equals(EnumParamType.ID)) {
        // this.paramsId = true;
        // } else if (paramType.equals(EnumParamType.OBJECT)) {
        // this.paramsObject = true;
        // } else {
        // this.paramsMap = true;
        // }
        // }
        // }
        // } else if (this.paramSize > 1) {
        // this.paramsMany = true;
        //
        // for (Map.Entry<Integer, String> entry : params.entrySet()) {
        // int index = entry.getKey();
        // if (index != this.cursorIndex && index != this.pageIndex && index
        // != this.sizeIndex)
        // cacheKey += "_" + entry.getValue() + "_" + args[entry.getKey()];
        // }
        // }
    }

    private void initMapperStatement() throws Exception {

        // MappedStatement ms =
        // this.configuration.getMappedStatement("com.pinoo.demo.dao.MessageDao.load");
        this.loadMappedStatementId = this.mapperInterface.getName() + ".load";
        this.insertMappedStatementId = this.mapperInterface.getName() + ".insert";
        this.updateMappedStatementId = this.mapperInterface.getName() + ".update";
        this.deleteMappedStatementId = this.mapperInterface.getName() + ".delete";

        synchronized (this.entityClass) {
            if (this.syncMapperStatementInitMap.get(this.entityClass) == null) {

                // MappedStatement ms = this.configuration
                // .getMappedStatement("com.pinoo.demo.dao.MessageDao.getMsgListByStatus");

                SqlMapperXmlBuilder sqlMapperXmlBuilder = new SqlMapperXmlBuilder(this);

                String xml = sqlMapperXmlBuilder.buildXml();

                System.out.println("======================");
                System.out.println(xml);
                System.out.println("======================");

                InputStream input = new ByteArrayInputStream(xml.getBytes());
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(input, configuration, "",
                        configuration.getSqlFragments());
                xmlMapperBuilder.parse();

                this.syncMapperStatementInitMap.put(this.entityClass, this.loadMappedStatementId);
            }
        }
    }

    private void initModelInfo() {
        ModelInfo info = AnnotationScaner.scanAnnotation(mapperInterface, ModelInfo.class);
        this.tableName = info.tableName();
        this.entityClass = info.entityClass();

        if (StringUtils.isEmpty(this.tableName)) {
            throw new IllegalArgumentException(this.mapperInterface.getName() + " table name is empty!!!");
        }
        if (entityClass == null) {
            throw new IllegalClassException(this.mapperInterface.getName() + " EntityClass is error");
        }
        try {
            propertyDescriptors = Introspector.getBeanInfo(entityClass).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new RuntimeException("get CLZ:" + entityClass.getName() + " property is error!");
        }
    }

    private void initPrimaryField() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                    if (primaryKey != null) {
                        String name = one.getName();
                        String dbName = one.getName();
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();
                        this.primaryFieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                        break;
                    }
                }
            }
        }
    }

    private void initSortField() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    Sort sort = field.getAnnotation(Sort.class);
                    if (null != sort) {
                        String name = one.getName();
                        String dbName = name;
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();
                        this.sortFieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                        break;
                    }
                }
            }
        }
    }

    private void initCacheFormat() {
        this.objectFormat = entityClass.getName() + objectCacheKeySign;
        this.listFormat = entityClass.getName() + listCacheKeySign;
        this.countFormat = entityClass.getName() + countCacheKeySign;
        // 找出size ,cursor对应的参数序列
        this.cursorIndex = AnnotationScaner.scanMethodParamIndex(method, PageCursor.class);
        this.pageIndex = AnnotationScaner.scanMethodParamIndex(method, Page.class);
        this.sizeIndex = AnnotationScaner.scanMethodParamIndex(method, PageSize.class);
        // 生成unformatCacheKey

        synchronized (this.entityClass) {
            if (unformatCacheKeys.get(this.entityClass) == null) {
                Method[] methods = this.mapperInterface.getMethods();
                List<String> unformatCacheKeyList = new ArrayList<String>();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.equals("load") || methodName.equals("insert") || methodName.equals("update")
                            || methodName.equals("delete"))
                        continue;

                    Class<?> returnType = method.getReturnType();
                    boolean returnsMany = (configuration.getObjectFactory().isCollection(returnType) || returnType
                            .isArray());
                    boolean returnCount = ReflectionUtil.isMethodReturnCount(method);
                    String cacheKey = entityClass.getName();
                    if (returnsMany) {
                        cacheKey = cacheKey + listCacheKeySign;
                    } else if (returnCount) {
                        cacheKey = cacheKey + countCacheKeySign;
                    } else {
                        continue;
                    }
                    // else if (methodEnum == CacheMethodEnum.getCount) {
                    // cacheKey = cacheKey + queryCountCacheKeySign;
                    // }

                    LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                    for (int i : fieldNames.keySet()) {
                        String fieldName = fieldNames.get(i);
                        cacheKey = cacheKey + generateCacheKey(fieldName);
                    }

                    if (!unformatCacheKeyList.contains(cacheKey))
                        unformatCacheKeyList.add(cacheKey);

                    logger.info(this.mapperInterface + "class:{},method:{},cacheKey:{}",
                            new Object[] { entityClass.getName(), method.getName(), cacheKey });
                }
                unformatCacheKeys.put(this.entityClass, unformatCacheKeyList);
            }
        }
    }

    private String generateCacheKey(String name) {
        return "_" + name + "_@" + name;
    }

    private String replaceCacheKeyValue(String name, Object value) {
        if (null == value) {
            value = "NULL";
        }
        return "_" + name + "_" + value;
    }

    public String formartCacheKey(Object model, String cacheKey) {
        for (FieldInfo fieldInfo : fields) {
            try {
                cacheKey = cacheKey.replaceAll(generateCacheKey(fieldInfo.getName()),
                        replaceCacheKeyValue(fieldInfo.getName(), fieldInfo.getReadMethod().invoke(model)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cacheKey;
    }

    /**
     * 获取参数字段
     * 
     * @param method
     * @return
     */
    private LinkedHashMap<Integer, String> getParamsFields(Method method) {
        LinkedHashMap<Integer, String> list = new LinkedHashMap<Integer, String>();
        String[] result = new String[method.getParameterTypes().length];
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                for (int n = 0; n < annotations[i].length; n++) {
                    Annotation one = annotations[i][n];
                    if (one instanceof MethodParam) {
                        MethodParam p = (MethodParam) one;
                        result[i] = p.value();
                    }
                }
            }
        }

        for (int i = 0; i < result.length; i++) {
            String s = result[i];
            if (StringUtils.isNotBlank(s)) {
                list.put(i, s);
            }
        }
        return list;
    }

    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
        Integer index = null;
        final Class<?>[] argTypes = method.getParameterTypes();
        for (int i = 0; i < argTypes.length; i++) {
            if (paramType.isAssignableFrom(argTypes[i])) {
                if (index == null) {
                    index = i;
                } else {
                    throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName()
                            + " parameters");
                }
            }
        }
        return index;
    }

    private String getMapKey(Method method) {
        String mapKey = null;
        if (Map.class.isAssignableFrom(method.getReturnType())) {
            final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
            if (mapKeyAnnotation != null) {
                mapKey = mapKeyAnnotation.value();
            }
        }
        return mapKey;
    }

    private SortedMap<Integer, String> getParams(Method method, boolean hasNamedParameters) {
        final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
        final Class<?>[] argTypes = method.getParameterTypes();
        for (int i = 0; i < argTypes.length; i++) {
            if (!RowBounds.class.isAssignableFrom(argTypes[i]) && !ResultHandler.class.isAssignableFrom(argTypes[i])) {
                String paramName = String.valueOf(params.size());
                if (hasNamedParameters) {
                    paramName = getParamNameFromAnnotation(method, i, paramName);
                }
                params.put(i, paramName);
            }
        }
        return params;
    }

    private String getParamNameFromAnnotation(Method method, int i, String paramName) {
        final Object[] paramAnnos = method.getParameterAnnotations()[i];
        for (Object paramAnno : paramAnnos) {
            if (paramAnno instanceof MethodParam) {
                paramName = ((MethodParam) paramAnno).value();
            }
        }
        return paramName;
    }

    private boolean hasNamedParams(Method method) {
        boolean hasNamedParams = false;
        final Object[][] paramAnnos = method.getParameterAnnotations();
        for (Object[] paramAnno : paramAnnos) {
            for (Object aParamAnno : paramAnno) {
                if (aParamAnno instanceof MethodParam) {
                    hasNamedParams = true;
                    break;
                }
            }
        }
        return hasNamedParams;
    }

    public boolean hasRowBounds() {
        return (rowBoundsIndex != null);
    }

    public RowBounds extractRowBounds(Object[] args) {
        return (hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null);
    }

    public boolean hasResultHandler() {
        return (resultHandlerIndex != null);
    }

    public ResultHandler extractResultHandler(Object[] args) {
        return (hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null);
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getSizeIndex() {
        return sizeIndex;
    }

    public FieldInfo getPrimaryFieldInfo() {
        return primaryFieldInfo;
    }

    public FieldInfo getSortFieldInfo() {
        return sortFieldInfo;
    }

    public String getLoadMappedStatementId() {
        return loadMappedStatementId;
    }

    public String getInsertMappedStatementId() {
        return insertMappedStatementId;
    }

    public String getUpdateMappedStatementId() {
        return updateMappedStatementId;
    }

    public String getDeleteMappedStatementId() {
        return deleteMappedStatementId;
    }

    public String getMapKey() {
        return mapKey;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public boolean returnsCount() {
        return returnsCount;
    }

    public boolean returnsMany() {
        return returnsMany;
    }

    public boolean returnsMap() {
        return returnsMap;
    }

    public boolean returnsVoid() {
        return returnsVoid;
    }

    public boolean returnsObjectList() {
        return returnsObjectList;
    }

    public String getObjectFormat() {
        return objectFormat;
    }

    public boolean isParamsId() {
        return paramsId;
    }

    public boolean isParamsObject() {
        return paramsObject;
    }

    public String getListFormat() {
        return listFormat;
    }

    public List<String> getUnformatCacheKeys() {
        return unformatCacheKeys.get(this.entityClass);
    }

    public int getParamSize() {
        return paramSize;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getTableName() {
        return tableName;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Class<?> getMapperInterface() {
        return mapperInterface;
    }
}
