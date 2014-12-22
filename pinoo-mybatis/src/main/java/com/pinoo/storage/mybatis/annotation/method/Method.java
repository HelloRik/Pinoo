package com.pinoo.storage.mybatis.annotation.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.mapping.SqlCommandType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Method {

    SqlCommandType sqlCommandType();

    String parameterType() default "";

    String parameterMap() default "";

    String resultMap() default "";

    String resultType() default "";

    String sql() default "";

}
