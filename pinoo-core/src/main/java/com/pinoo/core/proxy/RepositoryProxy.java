package com.pinoo.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RepositoryProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("@@@@@@@@@" + method);
        return null;
    }

}
