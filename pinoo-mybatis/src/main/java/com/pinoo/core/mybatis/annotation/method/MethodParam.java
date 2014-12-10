package com.pinoo.core.mybatis.annotation.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存参数-对应MODEL字段
 * 
 * @Filename: CacheParam.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MethodParam {
    String value();

}
