package com.pinoo.storage.mongodb.annotation.dao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CacheMethodParam {

    CacheMethodParamEnum paramEnum() default CacheMethodParamEnum.NORMAL;

    /**
     * 如果参数名称和MODEL对象名称不一样的话在这里定义
     * 
     * @return
     */
    String field();
}
