package com.pinoo.core.mybatis.interceptor;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

import com.pinoo.core.mybatis.SessionDataSourceHelper;
import com.pinoo.core.mybatis.datasource.MasterSalveKey;

/**
 * Executor 拦截器
 * 
 * @Filename: AbstractExecutorInterceptor.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public abstract class AbstractExecutorInterceptor implements Interceptor {

    protected transient Log logger = LogFactory.getLog(getClass());

    public Object intercept(Invocation invocation) throws Throwable {

        try {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            MasterSalveKey key = SessionDataSourceHelper.getOptionKey();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】,Interceptor current key is "
                        + key + "!");
            }

            if (key != MasterSalveKey.FORCE && key != MasterSalveKey.TRANSACTION) {
                key = determineOption(mappedStatement);
                SessionDataSourceHelper.setOptionKey(key);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】,Interceptor set key " + key
                            + "!");
                }
            }
        } catch (Exception e) {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Thread ID : 【" + Thread.currentThread().getId() + "】,Interceptor set key error!", e);
            }
            SessionDataSourceHelper.setOptionKey(MasterSalveKey.FORCE);
        }

        return invocation.proceed();
    }

    protected abstract MasterSalveKey determineOption(MappedStatement mappedStatement);

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub

    }

}
