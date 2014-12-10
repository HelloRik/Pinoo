package com.pinoo.common.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ListSizeKey {

    /**
     * 对应的数组字段名称
     * 
     * @return
     */
    String listName();

}
