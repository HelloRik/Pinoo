package com.pinoo.storage.mongodb.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.sf.cglib.proxy.Enhancer;

public class MethodProxy {

    public void copy(Object target, Object source) throws Exception {
        Class<?> clazz = source.getClass();
        while (clazz != null) {
            Field[] sourceFields = clazz.getDeclaredFields();
            for (Field sourceField : sourceFields) {
                boolean isStatic = Modifier.isStatic(sourceField.getModifiers());
                boolean isFinal = Modifier.isFinal(sourceField.getModifiers());
                if (!(isStatic && isFinal)) {
                    sourceField.setAccessible(true);
                    Object sourceValue = sourceField.get(source);
                    sourceField.set(target, sourceValue);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    public Object createCacheProxy(Class targetClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new CacheCommonMethodInterceptor());
        return enhancer.create();
    }

}
