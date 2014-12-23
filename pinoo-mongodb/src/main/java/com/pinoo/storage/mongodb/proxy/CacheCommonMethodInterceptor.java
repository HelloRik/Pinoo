package com.pinoo.storage.mongodb.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.pinoo.storage.mongodb.annotation.dao.CacheDaoMethod;
import com.pinoo.storage.mongodb.annotation.dao.CacheMethodEnum;
import com.pinoo.storage.mongodb.dao.RedisCacheDao;

public class CacheCommonMethodInterceptor implements MethodInterceptor {

    static Logger logger = LoggerFactory.getLogger(CacheCommonMethodInterceptor.class);

    private String formatCacheKey(String cacheKey, Object object, Method method, Object[] args) {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        if (params != null) {
            for (int i : params.keySet()) {
                cacheKey = cacheKey.replaceAll(dao.generateCacheKey(params.get(i)),
                        dao.replaceCacheKeyValue(params.get(i), args[i]));
            }
        }
        logger.debug("==========formatCacheKey :" + cacheKey);
        return cacheKey;
    }

    private String getCacheKey(Object object, Method method, Object[] args) {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Map<Method, String> map = dao.getUnformatCacheKeys();
        String cacheKey = map.get(method);
        cacheKey = this.formatCacheKey(cacheKey, object, method, args);
        return cacheKey;
    }

    private Query getQuery(Object object, Method method, Object[] args) throws Throwable {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Query query = new Query();

        int cursorIndex = dao.getCursorIndex().get(method) != null ? dao.getCursorIndex().get(method) : -1;
        int sizeIndex = dao.getSizeIndex().get(method) != null ? dao.getSizeIndex().get(method) : -1;
        int pageIndex = dao.getPageIndex().get(method) != null ? dao.getPageIndex().get(method) : -1;

        for (int i = 0; i < args.length; i++) {
            if (i != cursorIndex && i != sizeIndex && i != pageIndex) {
                Object o = args[i];
                String key = dao.getParamsIndexMap().get(method).get(i);
                query.addCriteria(new Criteria(key).is(o));
            }
        }

        return query;
    }

    private long getCursor(Object object, Method method, Object[] args) {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Integer cursor_index = dao.getCursorIndex().get(method);
        if (cursor_index != null) {
            return (Long) args[cursor_index];
        }
        return -1;
    }

    private int getPage(Object object, Method method, Object[] args) {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Integer page_index = dao.getPageIndex().get(method);
        if (page_index != null) {
            return (Integer) args[page_index];
        }
        return 1;
    }

    private int getSize(Object object, Method method, Object[] args) {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Integer size_index = dao.getSizeIndex().get(method);
        if (size_index != null) {
            return (Integer) args[size_index];
        }
        return 10;
    }

    private List<Object> getparams(Object object, Method method, Object[] args) {
        List<Object> result = new ArrayList<Object>();
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        for (int i : params.keySet()) {
            result.add(args[i]);
        }
        return result;

    }

    private Object decideListReturnResult(Object object, Method method, Object[] args, List ids) throws Exception {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) returnType).getActualTypeArguments();
            if (types[0].equals(dao.getEntityClass())) {
                return dao.loads(ids);
            }
        }
        return ids;
    }

    private Object handleCacheGetList(Object object, Method method, Object[] args) throws Throwable {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        String listCacheKey = this.getCacheKey(object, method, args);
        int size = getSize(object, method, args);
        long cursor = getCursor(object, method, args);
        // int page = getPage(object, method, args);
        int page = 0;

        Query listQuery = getQuery(object, method, args);

        logger.debug(method.getDeclaringClass().getSimpleName()
                + ",【handleCacheGetList】listCacheKey:{},cursor:{},page:{},size:{}", new Object[] { listCacheKey,
                cursor, page, size });

        List<?> ids = dao._queryForList(listCacheKey, listQuery, cursor, page, size);
        return decideListReturnResult(object, method, args, ids);
    }

    private Object handleCacheGetCount(Object object, Method method, Object[] args) throws Throwable {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        String countCacheKey = this.getCacheKey(object, method, args);

        Query countQuery = getQuery(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + ",countCacheKey:{},args:{}", new Object[] {
                countCacheKey, args });
        return dao._queryForListCount(countCacheKey, countQuery);
    }

    private Object handleGetPageList(Object object, Method method, Object[] args) throws Throwable {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;

        String listCacheKey = this.getCacheKey(object, method, args);
        int size = getSize(object, method, args);
        long cursor = -1;
        int page = getPage(object, method, args);

        Query listQuery = getQuery(object, method, args);

        logger.debug(method.getDeclaringClass().getSimpleName()
                + ",【handleGetPageList】listCacheKey:{},cursor:{},page:{},size:{}", new Object[] { listCacheKey, cursor,
                page, size });

        List<?> ids = dao._queryForList(listCacheKey, listQuery, cursor, page, size);
        return decideListReturnResult(object, method, args, ids);
    }

    private Object handleGetAllList(Object object, Method method, Object[] args) throws Throwable {
        RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
        String listCacheKey = this.getCacheKey(object, method, args);
        int size = Integer.MAX_VALUE;
        long cursor = -1;
        int page = 1;
        Query listQuery = getQuery(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName()
                + ",【handleGetAllList】listCacheKey:{},cursor:{},page:{},size:{}", new Object[] { listCacheKey, cursor,
                page, size });
        List<?> ids = dao._queryForList(listCacheKey, listQuery, cursor, page, size);
        return decideListReturnResult(object, method, args, ids);
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        CacheDaoMethod cacheDaoMethod = method.getAnnotation(CacheDaoMethod.class);

        if (cacheDaoMethod != null) {
            CacheMethodEnum methodEnum = cacheDaoMethod.methodEnum();
            // if (methodEnum != null && args != null) {
            // logger.debug(method.getDeclaringClass().getSimpleName() + ","
            // + method.getName() + "==============args:"
            // + Arrays.asList(args));
            // }
            if (methodEnum == CacheMethodEnum.getList) {
                return this.handleCacheGetList(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getCount) {
                return this.handleCacheGetCount(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getPageList) {
                return this.handleGetPageList(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getAllList) {
                return this.handleGetAllList(object, method, args);
            } else {
                return methodProxy.invokeSuper(object, args);
            }
        }
        return methodProxy.invokeSuper(object, args);
    }

}
