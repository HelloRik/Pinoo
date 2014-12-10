package com.pinoo.common.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ColumnKey {

    /**
     * 是否映射数据库
     * 
     * @return
     */
    boolean isDbColumn() default true;

    /**
     * 数据库的字段名，如果名字一样 这个可以不填。
     * 
     * @return
     */
    String column() default "";

    /**
     * 是否是
     * 
     * @return
     */
    boolean isListData() default false;

    /**
     * 是否使用主键ID作为数组的SCORE
     * 
     * @return
     */
    boolean isPrivateKeySort() default true;

    /**
     * 排序字段的DAO
     * 
     * @return
     */
    String sortDao() default "";

    /**
     * 排序字段名称
     */
    String sortName() default "";

}
