package com.pinoo.core.beans;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.pinoo.core.executor.ExecutorFactory;

public class ExecutorFactoryBean implements InitializingBean, FactoryBean<ExecutorFactory> {

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public ExecutorFactory getObject() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getObjectType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSingleton() {
        // TODO Auto-generated method stub
        return false;
    }

}
