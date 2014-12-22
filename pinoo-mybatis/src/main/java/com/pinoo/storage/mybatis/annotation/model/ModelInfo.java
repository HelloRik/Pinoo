package com.pinoo.storage.mybatis.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MODEL对象信息
 * 
 * @Filename: ModelInfo.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
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
     * 主键自增策略
     * 
     * @return
     */
    IdentityType identityType() default IdentityType.origin_indentity;

    /**
     * 领域对象模型CLZ
     * 
     * @return
     */
    Class<?> entityClass();

    /**
     * 数据库对应的表名
     * 
     * @return
     */
    String tableName();

    // /**
    // * 序号自增表
    // *
    // * @return
    // */
    // String seqTableName() default "sequence";

    // /**
    // * spring data redis 序列化对象，默认为JDK序列化
    // *
    // * @return
    // */
    // Class objectSerialization() default
    // JdkSerializationRedisSerializer.class;

}
