package com.pinoo.annotation.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.pinoo.mapping.MethodType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodProxy {

    MethodType type() default MethodType.SELECT;

    String sql() default "";
}
