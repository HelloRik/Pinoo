package com.pinoo.core.component.cache;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import com.pinoo.core.FrameworkContext;
import com.pinoo.core.component.Initialzing;
import com.pinoo.core.dao.IEntityInfo;

@Component
public class RedisCache implements Initialzing, ApplicationContextAware, ICacheData {

    private Logger logger = LoggerFactory.getLogger(RedisCache.class);

    @Resource(name = "redisTemplate")
    private RedisTemplate templet;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "redisTemplate")
    private RedisTemplate objectTemplet;

    protected ApplicationContext applicationContext;

    // private ValueOperations<String, T> objectOps;
    //
    // private ValueOperations<String, String> countOps;
    //
    // private ZSetOperations<String, Long> zsetOps;
    //
    // private ZSetOperations<String, ID> queryZsetOps;

    // =========================================================================================================

    /**
     * 默认过期时间2小时
     */
    protected long DEFAULT_EXPIRED_TIME = FrameworkContext.DEFAULT_EXPIRED_TIME;

    /**
     * 默认过期时间单位：秒
     */
    protected TimeUnit DEFAULT_TIME_UNIT = FrameworkContext.DEFAULT_TIME_UNIT;

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

    // =========================================================================================================

    @Override
    public void initialize(IEntityInfo entityInfo) throws Exception {
        Class<?> serialClz = entityInfo.getModelInfo().objectSerialization();
        if (!(serialClz.getName().equals(JdkSerializationRedisSerializer.class.getName()))) {
            RedisSerializer serializer = (RedisSerializer) this.applicationContext.getBean(serialClz);
            this.objectTemplet.setValueSerializer(serializer);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean has(String key) {
        return this.templet.hasKey(key);
    }

    @Override
    public <T> T getObject(String key, Class<T> clz) {
        if (this.objectTemplet.hasKey(key)) {
            return (T) this.objectTemplet.opsForValue().get(key);
        }
        return null;
    }

    @Override
    public boolean setObject(String key, Object obj) {
        if (obj != null) {
            this.objectTemplet.opsForValue().set(key, obj, OBJECT_EXPIRED_TIME, DEFAULT_TIME_UNIT);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String key) {
        this.templet.delete(key);
        return true;
    }

}
