package com.pinoo.storage.mybatis.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import com.pinoo.beans.FieldInfo;

public class RedisCache implements ICache, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(RedisCache.class);

    protected RedisTemplate templet;

    protected StringRedisTemplate stringRedisTemplate;

    protected ValueOperations<String, Object> objectOps;

    protected ValueOperations<String, String> countOps;

    protected ZSetOperations<String, Long> queryZsetOps;

    protected TimeUnit timeUnit;

    protected long timeout = -1;

    private final static String loggerTypeTag = "CACHE";

    @Override
    public void afterPropertiesSet() throws Exception {
        this.countOps = this.stringRedisTemplate.opsForValue();
        this.objectOps = this.templet.opsForValue();
        this.queryZsetOps = this.templet.opsForZSet();

        if (timeUnit == null)
            this.timeUnit = TimeUnit.SECONDS;
        if (this.timeout <= 0)
            this.timeout = 60 * 60 * 24;
    }

    protected void saveLog(String tag, String methodName, Object... objs) {
        saveLog(tag, methodName, "{}", objs);
    }

    protected void saveLog(String tag, String methodName, String content, Object... objs) {
        try {
            // if (this.logger.isDebugEnabled()) {
            this.logger.info("【" + tag + "】" + "【" + methodName + "】" + "【" + content + "】", objs);
            // }
        } catch (Exception e) {
        }
    }

    @Override
    public boolean hasKey(String key) {
        return this.templet.hasKey(key);
    }

    @Override
    public void setObject(String key, Object obj) {
        if (obj != null) {
            saveLog(this.loggerTypeTag, "setObject", "cache key:{}", key);
            this.objectOps.set(key, obj, timeout, timeUnit);
        }
    }

    @Override
    public Object getObject(String key) {
        if (this.templet.hasKey(key)) {
            saveLog(this.loggerTypeTag, "getObject", "hit cache,key:{}", key);
            return this.objectOps.get(key);
        }
        return null;
    }

    @Override
    public void removeObject(String key) {
        if (this.templet.hasKey(key)) {
            saveLog(this.loggerTypeTag, "removeObject", "cache key:{}", key);
            this.templet.delete(key);
        }
    }

    @Override
    public void setCount(String key, int count) {
        countOps.set(key, String.valueOf(count));
    }

    @Override
    public int getCount(String key) {
        if (this.stringRedisTemplate.hasKey(key)) {
            return Integer.parseInt(countOps.get(key));
        }
        return 0;
    }

    @Override
    public void increaseCount(String key, int count) {
        if (this.stringRedisTemplate.hasKey(key)) {
            countOps.increment(key, count);
        }
    }

    @Override
    public void decreaseCount(String key, int count) {
        if (this.stringRedisTemplate.hasKey(key)) {
            countOps.increment(key, 0 - count);
        }
    }

    @Override
    public <T> void setList(String listCacheKey, List<T> objects, FieldInfo primaryInfo, FieldInfo sortInfo)
            throws Exception {
        if (objects != null && objects.size() > 0) {
            for (T o : objects) {
                long score = (Long) sortInfo.getReadMethod().invoke(o);
                queryZsetOps.add(listCacheKey, (Long) primaryInfo.getReadMethod().invoke(o), score);
            }
            this.templet.expire(listCacheKey, this.timeout, this.timeUnit);
        }
    }

    @Override
    public List<Long> getList(String listCacheKey, long cursor, int page, int size) {
        Set<Long> set = null;
        if (cursor >= 0) {
            cursor = cursor <= 0 ? Long.MAX_VALUE : cursor - 1;
            set = this.queryZsetOps.reverseRangeByScore(listCacheKey, 0, cursor, 0, size);
        } else {
            page = page <= 0 ? 1 : page;
            set = this.queryZsetOps.reverseRangeByScore(listCacheKey, 0, Long.MAX_VALUE, (page - 1) * size, size);
        }
        if (set != null) {
            saveLog(this.loggerTypeTag, "getList", "hit cache,key:{}", listCacheKey);
            return new ArrayList<Long>(set);
        }
        return null;
    }

    @Override
    public void addToList(String listCacheKey, Object obj, FieldInfo primaryInfo, FieldInfo sortInfo) throws Exception {
        if (listCacheKey != null && this.templet.hasKey(listCacheKey)) {
            long score = (Long) sortInfo.getReadMethod().invoke(obj);
            saveLog(this.loggerTypeTag, "addToList", "add cache :{},sort:{},model:{}", new Object[] { listCacheKey,
                    score, obj });
            queryZsetOps.add(listCacheKey, (Long) primaryInfo.getReadMethod().invoke(obj), score);
        }
    }

    @Override
    public void removeToList(String listCacheKey, Object id) throws Exception {
        if (listCacheKey != null && this.templet.hasKey(listCacheKey)) {
            saveLog(this.loggerTypeTag, "removeToList", "remove cache :{},id:{}", new Object[] { listCacheKey, id });
            this.queryZsetOps.remove(listCacheKey, id);
        }
    }

    public void setTemplet(RedisTemplate templet) {
        this.templet = templet;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

}
