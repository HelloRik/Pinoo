package com.pinoo.storage.mongodb.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModelInfo {

    /**
     * 是否缓存对象
     * 
     * @return
     */
    boolean cacheObject() default true;

    /**
     * 数据库对应的表名
     * 
     * @return
     */
    String tableName() default "";

    /**
     * 序号自增表
     * 
     * @return
     */
    String seqTableName() default "sequence";

    /**
     * 主键自增策略
     * 
     * @return
     */
    IdentityType identityType() default IdentityType.identity;

    /**
     * spring data redis 序列化对象，默认为JDK序列化
     * 
     * @return
     */
    Class objectSerialization() default JdkSerializationRedisSerializer.class;

}
