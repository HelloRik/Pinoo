package com.pinoo.storage.mongodb.dao;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.mongodb.DBObject;
import com.pinoo.common.utils.ReflectionUtil;
import com.pinoo.common.utils.lock.ZMutexLock;
import com.pinoo.storage.mongodb.annotation.dao.CacheDaoMethod;
import com.pinoo.storage.mongodb.annotation.dao.CacheMethodEnum;
import com.pinoo.storage.mongodb.annotation.dao.CacheMethodParam;
import com.pinoo.storage.mongodb.annotation.dao.CacheMethodParamEnum;
import com.pinoo.storage.mongodb.annotation.model.ColumnKey;
import com.pinoo.storage.mongodb.annotation.model.FieldInfo;
import com.pinoo.storage.mongodb.annotation.model.ListFieldInfo;

/**
 * 针对MONGO + REDIS的DAO包装
 * 
 * @Filename: RedisCacheDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 * @param <T>实体类型
 * @param <ID>主键类型
 */
public abstract class RedisCacheDao<T, ID> extends MongoDbDao<T, ID> implements IRedisCacheDao<T, ID>, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(RedisCacheDao.class);

    private final static String loggerTypeTag = "CACHE";

    @Resource(name = "redisTemplate")
    protected RedisTemplate templet;

    @Resource(name = "stringRedisTemplate")
    protected StringRedisTemplate stringRedisTemplate;

    @Resource(name = "redisTemplate")
    protected RedisTemplate objectTemplet;

    protected ValueOperations<String, T> objectOps;

    protected ValueOperations<String, String> countOps;

    protected ZSetOperations<String, Long> zsetOps;

    protected ZSetOperations<String, ID> queryZsetOps;

    /**
     * 默认过期时间2小时
     */
    protected long DEFAULT_EXPIRED_TIME = 60 * 60 * 2;

    /**
     * 默认过期时间单位：秒
     */
    protected TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 默认对象过期时间
     */
    protected long OBJECT_EXPIRED_TIME = DEFAULT_EXPIRED_TIME;

    /**
     * 默认数组字段对象过期时间
     */
    protected long LIST_EXPIRED_TIME = DEFAULT_EXPIRED_TIME;

    /**
     * 默认通过QUERY查询到的对象的过期时间
     */
    protected long QUERY_OBJECT_EXPIRED_TIME = DEFAULT_EXPIRED_TIME;

    protected long TEMP_EXPIRED_TIME = 60;

    /**
     * 默认对象列表过期时间
     */
    protected long QUERY_LIST_EXPIRED_TIME = DEFAULT_EXPIRED_TIME;

    protected static final String listCacheKeySign = "_list_%s_%s";

    protected static final String listCountCacheKeySign = "_list_count_%s_%s";

    protected static final String objectCacheKeySign = "_object_%s";

    protected static final String combinedCacheKeySign = "_combined_";

    protected String objectFormat;

    /**
     * 是否保存对象缓存
     */
    protected boolean enableObjectCache;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        initObjectCache();
        logger.info(daoClassName + " initObjectCache!");
        initQueryCache();
        logger.info(daoClassName + " initQueryCache!");
        initSpringDataForRedis();
        logger.info(daoClassName + " initSpringDataForRedis! default serializer:{},object serializer:{}", new Object[] {
                this.templet.getValueSerializer(), this.objectTemplet.getValueSerializer() });
    }

    private void initSpringDataForRedis() throws InstantiationException, IllegalAccessException {
        Class<?> serialClz = modelInfo.objectSerialization();

        if (!(serialClz.getName().equals(JdkSerializationRedisSerializer.class.getName()))) {
            RedisSerializer<T> serializer = (RedisSerializer<T>) this.applicationContext.getBean(serialClz);
            this.objectTemplet.setValueSerializer(serializer);
        }

        this.countOps = this.stringRedisTemplate.opsForValue();
        this.zsetOps = this.templet.opsForZSet();
        this.objectOps = this.objectTemplet.opsForValue();
        this.queryZsetOps = this.templet.opsForZSet();
    }

    private void initObjectCache() throws Exception {
        this.enableObjectCache = modelInfo.cacheObject();

        this.objectFormat = this.entityClass.getName() + objectCacheKeySign;
        this.existFormat = this.entityClass.getName() + "_exist_";

    }

    protected void initListInfo() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    ColumnKey columnKey = field.getAnnotation(ColumnKey.class);
                    if (columnKey != null && columnKey.isListData()) {
                        String listFormat = this.entityClass.getName() + listCacheKeySign;
                        String listCountFormat = this.entityClass.getName() + listCountCacheKeySign;
                        boolean isPrivateKeySort = columnKey.isPrivateKeySort();
                        String sortDao = columnKey.sortDao();
                        String sortName = columnKey.sortName();
                        ListFieldInfo info = new ListFieldInfo(fieldName, listFormat, listCountFormat,
                                isPrivateKeySort, sortDao, sortName);
                        this.listFieldInfos.put(fieldName, info);
                        logger.info(this.entityClass + " find list data, list data info :{}", new Object[] { info });
                    }
                }
            }
        }
    }

    private String formatListCountCacheKey(String listName, Object id) {
        ListFieldInfo info = this.listFieldInfos.get(listName);
        return info != null ? formatCacheKey(info.getCountFormat(), listName, id) : null;
    }

    private String formatListCacheKey(String listName, Object id) {
        ListFieldInfo info = this.listFieldInfos.get(listName);
        return info != null ? formatCacheKey(info.getFormat(), listName, id) : null;
    }

    private String formatCacheKey(String format, Object... objs) {
        return format != null ? String.format(format, objs) : null;
    }

    private String getExistKey(ID id, String listName, List<Long> objs) {
        DBObject query = doPrimaryKeyQuery(id);
        query.put(listName, objs);
        return formatQueryKey(existFormat, query.toMap());
    }

    private String existFormat;

    private String formatQueryKey(String format, Map<String, Object> map) {
        for (String key : map.keySet()) {
            format += key + "_";
            if (map.get(key) instanceof List) {
                List list = (List) map.get(key);
                for (Object v : list) {
                    format += v + "_";
                }
            } else {
                format += map.get(key) + "_";
            }
        }
        // saveLog(this.loggerTypeTag, "formatQueryKey", "format:{}", format);
        return format;
    }

    @Override
    public T load(ID id) throws Exception {

        // long startTime = System.currentTimeMillis();

        String key = formatCacheKey(this.objectFormat, id);
        T object = null;

        // 如果缓存中没有标识要刷新才直接取缓存中数据
        if (this.templet.hasKey(key) && this.enableObjectCache) {
            object = (T) objectOps.get(key);
            saveLog(this.loggerTypeTag, "load", "hit cache,key:{}", key);
        } else {
            saveLog(this.loggerTypeTag, "load", "not hit cache,load by db,key:{}", key);
            object = super.load(id);
            if (object != null && this.enableObjectCache) {
                this.objectOps.set(key, object, OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
            }
        }
        // saveLog(this.loggerTypeTag, "load", "load obj time:{}",
        // (System.currentTimeMillis() - startTime));

        return object;
    }

    @Override
    public List<T> loads(List<ID> objs) throws Exception {
        List<T> results = new ArrayList<T>();
        for (ID id : objs) {
            results.add(load(id));
        }
        return results;
    }

    @Override
    public T insert(T model) throws Exception {
        model = super.insert(model);
        if (this.enableObjectCache) {
            ID id = getId(model);
            String key = formatCacheKey(this.objectFormat, id);
            objectOps.set(key, model, OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
        }
        addQueryCache(model);
        saveLog(this.loggerTypeTag, "insert", model);
        return model;
    }

    /**
     * 不建议直接修改对象
     */
    @Override
    public boolean update(T model) throws Exception {
        ID id = getId(model);
        T oldModel = load(id);
        boolean flag = super.update(model);
        if (flag) {
            String key = formatCacheKey(this.objectFormat, id);
            if (this.templet.hasKey(key) && this.enableObjectCache) {
                model = super.load(id);
                objectOps.set(key, model, OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
                // saveLog(this.loggerTypeTag, "update",
                // "update object cache,key:{},{}", key, model);
            }
            deleteQueryCache(oldModel);
            addQueryCache(model);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(ID id) throws Exception {
        T o = this.load(id);
        boolean flag = super.delete(id);
        if (flag) {
            String key = formatCacheKey(this.objectFormat, id);
            if (this.templet.hasKey(key) && this.enableObjectCache) {
                this.templet.delete(key);
                // saveLog(this.loggerTypeTag, "delete",
                // "delete object cache,key:{},{}", key);
            }
            deleteQueryCache(o);
            return true;
        }
        return false;
    }

    @Override
    public int getListCount(ID id, String listName) throws Exception {
        String key = formatListCountCacheKey(listName, id);
        if (this.stringRedisTemplate.hasKey(key)) {
            // saveLog(this.loggerTypeTag, "getListCount", "hit cache,key:{}",
            // key);
            return Integer.parseInt(countOps.get(key));
        } else {
            int count = super.getListCount(id, listName);
            countOps.set(key, String.valueOf(count), DEFAULT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
            // saveLog(this.loggerTypeTag, "getListCount",
            // "NOT hit cache,key:{}", key);
            return count;
        }
    }

    private final static boolean INCR_STEP = true;

    private final static boolean DECR_STEP = false;

    private final static int DEFAULT_INCR_SETP = 1;

    // private final static int DEFAULT_DECR_SETP = -1;

    private void updateCountCache(String key, boolean step, int count) {
        if (this.stringRedisTemplate.hasKey(key)) {
            if (step)
                countOps.increment(key, count);
            else
                countOps.increment(key, 0 - count);
            // saveLog(this.loggerTypeTag, "updateCountCache",
            // "updateCountCache,key:{},step:{}", key, step);
        }
    }

    private void updateListCache(String listName, String key, boolean step, List<Long> values) throws Exception {
        if (this.templet.hasKey(key)) {
            if (step) {
                Set<TypedTuple<Long>> tuples = new HashSet<ZSetOperations.TypedTuple<Long>>();
                for (Long v : values) {
                    long score = this.getSort(listName, v);
                    tuples.add(new DefaultTypedTuple(v, (double) score));
                }
                zsetOps.add(key, tuples);
            } else {
                zsetOps.remove(key, values.toArray());
            }
            // saveLog(this.loggerTypeTag, "updateListCache",
            // "updateListCache,key:{},step:{},member:{},score:{}", key,
            // step, values);
        }
    }

    @Override
    public boolean pushToList(ID id, String listName, long value) throws Exception {
        boolean flag = super.pushToList(id, listName, value);
        // long startTime = System.currentTimeMillis();
        if (flag) {
            String listCountKey = formatListCountCacheKey(listName, id);
            updateCountCache(listCountKey, INCR_STEP, DEFAULT_INCR_SETP);

            String listKey = formatListCacheKey(listName, id);
            updateListCache(listName, listKey, INCR_STEP, Arrays.asList(value));
            // saveLog(this.loggerTypeTag, "pushToList", "time:{}",
            // (System.currentTimeMillis() - startTime));

            if (this.utimeFieldInfo != null)
                refreshObjectCache(id);
            // saveLog(this.loggerTypeTag, "pushToList",
            // "id:{},listName:{},member:{},score:{}", id, listName, value);

            return true;
        }
        return false;
    }

    @Override
    public boolean removeToList(ID id, String listName, long value) throws Exception {
        boolean flag = super.removeToList(id, listName, value);
        if (flag) {
            String listCountKey = formatListCountCacheKey(listName, id);
            updateCountCache(listCountKey, DECR_STEP, DEFAULT_INCR_SETP);

            String listKey = formatListCacheKey(listName, id);
            updateListCache(listName, listKey, DECR_STEP, Arrays.asList(value));

            String existKey = getExistKey(id, listName, Arrays.asList(value));
            // saveLog(this.loggerTypeTag, "removeToList", "remove existKey:{}",
            // existKey);
            this.stringRedisTemplate.delete(existKey);

            // refreshObjectCache(id);
            // saveLog(this.loggerTypeTag, "removeToList",
            // "id:{},listName:{},member:{}", id, listName, value);

            return true;
        }
        return false;
    }

    @Override
    public boolean removeAllToList(ID id, String listName, List<Long> values) throws Exception {
        boolean flag = super.removeAllToList(id, listName, values);
        if (flag) {
            String listCountKey = formatListCountCacheKey(listName, id);
            updateCountCache(listCountKey, DECR_STEP, values.size());

            String listKey = formatListCacheKey(listName, id);
            updateListCache(listName, listKey, DECR_STEP, values);

            for (long v : values) {
                String existKey = getExistKey(id, listName, Arrays.asList(v));
                // saveLog(this.loggerTypeTag, "removeToList",
                // "remove existKey:{}", existKey);
                this.stringRedisTemplate.delete(existKey);
            }

            refreshObjectCache(id);
            return true;
        }
        return false;
    }

    private String formatRefreshCacheKey(ID id) {
        return formatCacheKey("refresh_" + this.objectFormat, id);
    }

    /**
     * 刷新对象缓存
     * 
     * @param id
     * @throws Exception
     */
    private void refreshObjectCache(final ID id) {
        ZMutexLock lock = null;
        try {
            long start = System.currentTimeMillis();
            lock = new ZMutexLock();
            String refreshKey = this.formatRefreshCacheKey(id);

            if (lock.lock(refreshKey)) {
                String key = formatCacheKey(this.objectFormat, id);
                T object = super.load(id);

                if (object != null && this.enableObjectCache) {
                    this.objectOps.set(key, object, OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
                }
                // saveLog(this.loggerTypeTag, "refreshObjectCache",
                // "object key:{}", key);
            }
            saveLog(this.loggerTypeTag, "refreshObjectCache", "time:{}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            this.logger.info(this.entityClass.getSimpleName() + " refreshObjectCache error! obj id : " + id);
        } finally {
            if (lock != null) {
                lock.unlock();
                lock = null;
            }
        }
    }

    public List<Long> getPageList(ID id, String listName, int page, int pageSize) throws Exception {
        String listKey = formatListCacheKey(listName, id);
        if (!this.templet.hasKey(listKey)) {
            // saveLog(this.loggerTypeTag, "getPageList",
            // "not hit cache,key:{}", listKey);
            getList(id, listName);
        }
        page = page <= 0 ? 1 : page;

        Set<Long> set = zsetOps.reverseRangeByScore(listKey, 0, Long.MAX_VALUE, (page - 1) * pageSize, pageSize);

        if (set != null) {
            // saveLog(this.loggerTypeTag, "getPageList", "hit cache,key:{}",
            // listKey);
            return new ArrayList<Long>(set);
        }
        return null;
    }

    public List<Long> getList(ID id, String listName, long cursor, int pageSize) throws Exception {
        String listKey = formatListCacheKey(listName, id);
        if (!this.templet.hasKey(listKey)) {
            // saveLog(this.loggerTypeTag, "getList", "not hit cache,key:{}",
            // listKey);
            getList(id, listName);
        }
        cursor = cursor <= 0 ? Long.MAX_VALUE : cursor - 1;
        Set<Long> set = zsetOps.reverseRangeByScore(listKey, 0, cursor, 0, pageSize);

        if (set != null) {
            // saveLog(this.loggerTypeTag, "getList", "hit cache,key:{}",
            // listKey);
            return new ArrayList<Long>(set);
        }
        return null;
    }

    private long getSort(String listName, long value) throws Exception {
        ListFieldInfo info = this.listFieldInfos.get(listName);

        long score = 0;
        if (info.isPrivateKeySort()) {
            score = value;
        } else {
            MongoDbDao relationDao = (MongoDbDao) this.applicationContext.getBean(info.getSortDao());
            Object obj = relationDao.load(value);
            FieldInfo sortInfo = (FieldInfo) relationDao.getFieldsMap().get(info.getSortName());
            Method method = sortInfo.getReadMethod();
            score = (Long) method.invoke(obj);
        }

        return score;
    }

    @Override
    public List<Long> getList(ID id, String listName) throws Exception {
        String listKey = formatListCacheKey(listName, id);
        if (!this.templet.hasKey(listKey)) {
            // saveLog(this.loggerTypeTag, "getList", "not hit cache,key:{}",
            // listKey);
            ListFieldInfo info = this.listFieldInfos.get(listName);
            if (info == null)
                return null;

            List<Long> ids = super.getList(id, listName);

            if (ids == null)
                return null;

            for (long o : ids) {
                long score = getSort(listName, o);
                zsetOps.add(listKey, o, score);
            }

            this.templet.expire(listKey, LIST_EXPIRED_TIME, DEFAULT_TIME_UNIT);
        }

        Set<Long> set = zsetOps.reverseRangeByScore(listKey, 0, Long.MAX_VALUE);

        if (set != null) {
            // saveLog(this.loggerTypeTag, "getList", "hit cache,key:{}",
            // listKey);
            return new ArrayList<Long>(set);
        }
        return null;
    }

    @Override
    public boolean existList(ID id, String listName, List<Long> objs) throws Exception {
        String key = getExistKey(id, listName, objs);
        if (this.stringRedisTemplate.hasKey(key)) {
            // saveLog(this.loggerTypeTag, "existList", "hit cache,key:{}",
            // key);
            return true;
        } else {
            // saveLog(this.loggerTypeTag, "existList", "NOT hit cache,key:{}",
            // key);
            boolean flag = super.existList(id, listName, objs);
            if (flag) {
                this.countOps.set(key, "1", QUERY_OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
                return flag;
            } else {
                return false;
            }
        }
    }

    public Map<String, ListFieldInfo> getListFieldInfos() {
        return listFieldInfos;
    }

    // ============================================query==========================================================

    public static final String queryListCacheKeySign = "_query_list_";

    public static final String queryCountCacheKeySign = "_query_count_";

    private Map<Method, Integer> sizeIndex = new HashMap<Method, Integer>();

    private Map<Method, Integer> cursorIndex = new HashMap<Method, Integer>();

    private Map<Method, Integer> pageIndex = new HashMap<Method, Integer>();

    private Map<Method, Map<Integer, String>> paramsIndexMap = new HashMap<Method, Map<Integer, String>>();

    // 未被格式化的CacheKey。
    private Map<Method, String> unformatCacheKeys = new HashMap<Method, String>();

    private void initQueryCache() throws Exception {
        // 生成 cacheKey
        Method[] methods = this.getClass().getMethods();
        logger.info("==========================");
        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                CacheMethodEnum methodEnum = daoMethod.methodEnum();
                String cacheKey = entityClass.getName();

                if (methodEnum == CacheMethodEnum.getList || methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getAllList) {
                    cacheKey = cacheKey + queryListCacheKeySign;
                } else if (methodEnum == CacheMethodEnum.getCount) {
                    cacheKey = cacheKey + queryCountCacheKeySign;
                }

                LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                for (int i : fieldNames.keySet()) {
                    String fieldName = fieldNames.get(i);
                    cacheKey = cacheKey + generateCacheKey(fieldName);
                }
                unformatCacheKeys.put(method, cacheKey);
                logger.info(daoClassName + "class:{},method:{},cacheKey:{}", new Object[] { entityClass.getName(),
                        method.getName(), cacheKey });
            }
        }
        logger.info("==========================");

        // 找出size ,cursor对应的参数序列

        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                int sizeIdx = getParamsIndex(method, CacheMethodParamEnum.SIZE);
                if (sizeIdx >= 0) {
                    sizeIndex.put(method, sizeIdx);
                    logger.info(daoClassName + "class:{},method:{},sizeIdx:{}", new Object[] { entityClass.getName(),
                            method.getName(), sizeIdx });
                }
                int cursorIdx = getParamsIndex(method, CacheMethodParamEnum.CURSOR);
                if (cursorIdx >= 0) {
                    cursorIndex.put(method, cursorIdx);
                    logger.info(daoClassName + "class:{},method:{},cursorIdx:{}", new Object[] { entityClass.getName(),
                            method.getName(), cursorIdx });
                }
                int pageIdx = getParamsIndex(method, CacheMethodParamEnum.PAGE);
                if (pageIdx >= 0) {
                    pageIndex.put(method, pageIdx);
                    logger.info(daoClassName + "class:{},method:{},pageIdx:{}", new Object[] { entityClass.getName(),
                            method.getName(), pageIdx });
                }
            }
        }
        logger.info("==========================");

        // 生成query对象
        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (daoMethod != null) {
                CacheMethodEnum methodEnum = daoMethod.methodEnum();
                if (methodEnum != CacheMethodEnum.getList && methodEnum != CacheMethodEnum.getPageList
                        && methodEnum != CacheMethodEnum.getAllList && methodEnum != CacheMethodEnum.getCount) {
                    continue;
                }

                LinkedHashMap<Integer, String> methodParamsNames = getParamsFields(method);
                for (int i : methodParamsNames.keySet()) {
                    String fieldName = methodParamsNames.get(i);

                    FieldInfo fieldInfo = this.fieldsMap.get(fieldName);
                    if (fieldInfo == null) {
                        throw new RuntimeException("方法参数不正确,必须和对应的domain名字一样，或者加上注解标识：class:"
                                + method.getDeclaringClass().getName() + ",method:" + method.getName() + ",field:"
                                + fieldName);
                    }
                    logger.info(daoClassName + "class:{},method:{},paramsName:{}", new Object[] {
                            entityClass.getName(), method.getName(), fieldName });
                }
                paramsIndexMap.put(method, methodParamsNames);
                logger.info("==========================");
            }
        }

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
        // String[] result = ReflectUtil.getMethodParamNames(method);
        // for (int i = 0; i < result.length; i++) {
        // String paramName = result[i];
        // if (paramName.equals("size") || paramName.equals("cursor")) {
        // result[i] = "";
        // }
        // }
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof CacheMethodParam) {
                    CacheMethodParam param = (CacheMethodParam) one;
                    if (param.paramEnum() == CacheMethodParamEnum.NORMAL) {
                        result[i] = param.field();
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

    /**
     * 获取参数下标
     * 
     * @param method
     * @param paramEnum
     * @return
     */
    private int getParamsIndex(Method method, CacheMethodParamEnum paramEnum) {
        int index = -1;
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof CacheMethodParam) {
                    CacheMethodParam param = (CacheMethodParam) one;
                    if (param.paramEnum() == paramEnum) {
                        index = i;
                    }
                }
            }
        }
        return index;
    }

    public String generateCacheKey(String name) {
        return "_" + name + "_@" + name;
    }

    public String replaceCacheKeyValue(String name, Object value) {
        if (null == value) {
            value = "NULL";
        }
        return "_" + name + "_" + value;
    }

    private String formartCacheKey(T model, String cacheKey) {
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

    // ==============================================================================

    public long getObjectSort(T o) throws Exception {
        long score;
        if (this.sortFieldInfo != null) {
            score = (Long) this.sortFieldInfo.getReadMethod().invoke(o);
        } else {
            score = (Long) this.primaryFieldInfo.getReadMethod().invoke(o);
        }
        return score;
    }

    public List<ID> _queryForList(String listCacheKey, Query listQuery, long cursor, int page, int size)
            throws Exception {
        // String decidedCacheKey = decideListCacheKey(unDecidedCacheKey,
        // order);

        if (!this.templet.hasKey(listCacheKey)) {
            // saveLog(this.loggerTypeTag, "getList", "not hit cache,key:{}",
            // listKey);
            List<T> objects = super.queryForObjectList(listQuery);
            if (objects == null)
                return null;

            for (T o : objects) {
                long score = getObjectSort(o);
                queryZsetOps.add(listCacheKey, (ID) this.primaryFieldInfo.getReadMethod().invoke(o), score);
            }

            this.templet.expire(listCacheKey, LIST_EXPIRED_TIME, DEFAULT_TIME_UNIT);
        }

        Set<ID> set = null;

        if (cursor >= 0) {
            cursor = cursor <= 0 ? Long.MAX_VALUE : cursor - 1;
            set = this.queryZsetOps.reverseRangeByScore(listCacheKey, 0, cursor, 0, size);
        } else {
            page = page <= 0 ? 1 : page;
            set = this.queryZsetOps.reverseRangeByScore(listCacheKey, 0, Long.MAX_VALUE, (page - 1) * size, size);
        }

        if (set != null) {
            // saveLog(this.loggerTypeTag, "getList", "hit cache,key:{}",
            // listKey);
            return new ArrayList<ID>(set);
        }
        return null;
    }

    public long _queryForListCount(String countCacheKey, Query countQuery) throws Exception {
        if (!this.templet.hasKey(countCacheKey)) {
            long count = super.queryForListCount(countQuery);
            this.countOps.set(countCacheKey, String.valueOf(count), QUERY_OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
        }

        String count = this.countOps.get(countCacheKey);

        if (StringUtils.isNotEmpty(count))
            return Long.parseLong(count);
        return 0;
    }

    // ===============================================================

    public void addQueryCache(T model) {
        String listKey = entityClass.getName() + queryListCacheKeySign;
        String countKey = entityClass.getName() + queryCountCacheKeySign;
        for (String cacheKey : unformatCacheKeys.values()) {
            cacheKey = formartCacheKey(model, cacheKey);
            if (cacheKey.startsWith(listKey)) {
                addQueryCache(cacheKey, model);
            } else if (cacheKey.startsWith(countKey)) {
                updateCountCache(cacheKey, INCR_STEP, DEFAULT_INCR_SETP);
            }
        }
    }

    public void addQueryCache(String listCacheKey, T model) {
        try {
            if (listCacheKey != null && this.templet.hasKey(listCacheKey)) {
                long score = this.getObjectSort(model);
                logger.info(daoClassName + "addQueryCache cache :{},sort:{},model:{}", new Object[] { listCacheKey,
                        score, model });
                this.queryZsetOps.add(listCacheKey, (ID) this.primaryFieldInfo.getReadMethod().invoke(model), score);
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    public void deleteQueryCache(T model) {
        String listKey = entityClass.getName() + queryListCacheKeySign;
        String countKey = entityClass.getName() + queryCountCacheKeySign;
        for (String cacheKey : unformatCacheKeys.values()) {
            cacheKey = formartCacheKey(model, cacheKey);
            if (cacheKey.startsWith(listKey)) {
                deleteQueryCache(cacheKey, model);
            } else if (cacheKey.startsWith(countKey)) {
                updateCountCache(cacheKey, DECR_STEP, DEFAULT_INCR_SETP);
            }
        }
    }

    public void deleteQueryCache(String listCacheKey, T model) {
        try {
            if (listCacheKey != null && this.templet.hasKey(listCacheKey)) {
                logger.info(daoClassName + "deleteQueryCache cache :{},model:{}", listCacheKey, model);
                this.queryZsetOps.remove(listCacheKey, (ID) this.primaryFieldInfo.getReadMethod().invoke(model));
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    public Map<Method, String> getUnformatCacheKeys() {
        return unformatCacheKeys;
    }

    public Map<Method, Map<Integer, String>> getParamsIndexMap() {
        return paramsIndexMap;
    }

    public Map<Method, Integer> getSizeIndex() {
        return sizeIndex;
    }

    public Map<Method, Integer> getCursorIndex() {
        return cursorIndex;
    }

    public Map<Method, Integer> getPageIndex() {
        return pageIndex;
    }

}
