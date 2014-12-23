package com.pinoo.storage.mongodb.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.pinoo.annotation.method.MethodProxy;
import com.pinoo.storage.mongodb.dao.RedisCacheDao;

/**
 * 缓存方法代理
 * 
 * @Filename: CacheCommonMethodInterceptor.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public class CacheCommonMethodInterceptor implements MethodInterceptor {

    static Logger logger = LoggerFactory.getLogger(CacheCommonMethodInterceptor.class);

    private String formatCacheKey(String cacheKey, RedisCacheDao<?, ?> dao, Method method, Object[] args) {
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

    private String getCacheKey(RedisCacheDao<?, ?> dao, Method method, Object[] args) {
        Map<Method, String> map = dao.getUnformatCacheKeys();
        String cacheKey = map.get(method);
        cacheKey = this.formatCacheKey(cacheKey, dao, method, args);
        return cacheKey;
    }

    private Query getQuery(RedisCacheDao<?, ?> dao, Method method, Object[] args) throws Throwable {
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

    private long getCursor(RedisCacheDao<?, ?> dao, Method method, Object[] args) {
        Integer cursor_index = dao.getCursorIndex().get(method);
        if (cursor_index != null) {
            return (Long) args[cursor_index];
        }
        return -1;
    }

    private int getPage(RedisCacheDao<?, ?> dao, Method method, Object[] args) {
        Integer page_index = dao.getPageIndex().get(method);
        if (page_index != null) {
            return (Integer) args[page_index];
        }
        return -1;
    }

    private int getSize(RedisCacheDao<?, ?> dao, Method method, Object[] args) {
        Integer size_index = dao.getSizeIndex().get(method);
        if (size_index != null) {
            return (Integer) args[size_index];
        }
        return 0;
    }

    private List<Object> getParams(RedisCacheDao<?, ?> dao, Method method, Object[] args) {
        List<Object> result = new ArrayList<Object>();
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        for (int i : params.keySet()) {
            result.add(args[i]);
        }
        return result;

    }

    private Object decideListReturnResult(RedisCacheDao<?, ?> dao, Method method, Object[] args, List ids)
            throws Exception {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) returnType).getActualTypeArguments();
            if (types[0].equals(dao.getEntityClass())) {
                return dao.loads(ids);
            }
        }
        return ids;
    }

    private Object handleGetList(RedisCacheDao<?, ?> dao, Method method, Object[] args) throws Throwable {
        String listCacheKey = this.getCacheKey(dao, method, args);
        int size = getSize(dao, method, args);
        long cursor = getCursor(dao, method, args);
        int page = getPage(dao, method, args);

        logger.debug(method.getDeclaringClass().getSimpleName()
                + ",【handleCacheGetList】listCacheKey:{},cursor:{},page:{},size:{}", new Object[] { listCacheKey,
                cursor, page, size });

        Query listQuery = getQuery(dao, method, args);
        List<?> ids = dao._queryForList(listCacheKey, listQuery, cursor, page, size);
        return decideListReturnResult(dao, method, args, ids);
    }

    private Object handleCacheGetCount(RedisCacheDao<?, ?> dao, Method method, Object[] args) throws Throwable {
        String countCacheKey = this.getCacheKey(dao, method, args);
        Query countQuery = getQuery(dao, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + ",countCacheKey:{},args:{}", new Object[] {
                countCacheKey, args });
        return dao._queryForListCount(countCacheKey, countQuery);
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, net.sf.cglib.proxy.MethodProxy methodProxy)
            throws Throwable {
        MethodProxy methodAnn = method.getAnnotation(MethodProxy.class);
        if (methodAnn != null) {
            RedisCacheDao<?, ?> dao = (RedisCacheDao<?, ?>) object;
            boolean isReturnMany = dao.isMethodReturnMany(method);
            boolean isReturnCount = dao.isMethodReturnCount(method);

            if (isReturnMany) {
                return this.handleGetList(dao, method, args);
            } else if (isReturnCount) {
                return this.handleCacheGetCount(dao, method, args);
            } else {
                return methodProxy.invokeSuper(object, args);
            }
        }
        return methodProxy.invokeSuper(object, args);
    }

}
