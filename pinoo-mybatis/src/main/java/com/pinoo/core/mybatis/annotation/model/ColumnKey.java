package com.pinoo.core.mybatis.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标识一个字段的数据库名称
 * 
 * @Filename: ColumnKey.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ColumnKey {

    /**
     * 数据库的字段名，如果名字一样 这个可以不填。
     * 
     * @return
     */
    String column() default "";

}
