package com.pinoo.storage.mongodb.proxy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.pinoo.storage.mongodb.annotation.dao.ProxyDao;

@Component
public class DaoBeanPostProcessor implements BeanPostProcessor {

    private MethodProxy methodProxy = new MethodProxy();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ProxyDao dao = bean.getClass().getAnnotation(ProxyDao.class);

        if (dao != null) {
            Object newBean = methodProxy.createCacheProxy(bean.getClass());
            if (null != newBean) {
                try {
                    methodProxy.copy(newBean, bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return newBean;
            } else {
                return bean;
            }
        }

        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
