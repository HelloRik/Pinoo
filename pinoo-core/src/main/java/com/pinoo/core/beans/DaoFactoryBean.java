package com.pinoo.core.beans;

import static org.springframework.util.Assert.notNull;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.dao.support.DaoSupport;

public class DaoFactoryBean<T> extends DaoSupport implements FactoryBean<T> {

    private Class<T> mapperInterface;

    private ExecutorFactoryBean executorFactoryBean;

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    protected void checkDaoConfig() throws IllegalArgumentException {

        notNull(this.mapperInterface, "Property 'mapperInterface' is required");

        // Configuration configuration = getSqlSession().getConfiguration();
        // if (this.addToConfig &&
        // !configuration.hasMapper(this.mapperInterface)) {
        // try {
        // configuration.addMapper(this.mapperInterface);
        // } catch (Throwable t) {
        // logger.error("Error while adding the mapper '" + this.mapperInterface
        // + "' to configuration.", t);
        // throw new IllegalArgumentException(t);
        // } finally {
        // ErrorContext.instance().reset();
        // }
        // }
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();

    }

    @Override
    public T getObject() throws Exception {
        return this.executorFactoryBean.getObject().getExecutor(mapperInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return this.mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
