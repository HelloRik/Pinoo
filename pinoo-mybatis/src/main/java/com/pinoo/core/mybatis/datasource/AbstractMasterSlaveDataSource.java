package com.pinoo.core.mybatis.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;

/**
 * 读写分离数据源，支持一主多从，不配置读库，默认读库为主库，自定义实现负载均衡
 * 
 * @Filename: AbstractMasterSlaveDataSource.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public abstract class AbstractMasterSlaveDataSource extends AbstractDataSource implements InitializingBean {

    protected Log logger = LogFactory.getLog(getClass());

    /**
     * 写数据源
     */
    protected DataSource masterDataSource;

    protected List<DataSource> slaveDataSources;

    protected int slaveSize;

    @Override
    public Connection getConnection() throws SQLException {
        return determineTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineTargetDataSource().getConnection(username, password);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (masterDataSource == null) {
            throw new IllegalArgumentException("Property 'masterDataSource' is required");
        }

        if (slaveDataSources == null || slaveDataSources.size() == 0) {
            this.slaveDataSources = new ArrayList<DataSource>();
            this.slaveDataSources.add(this.masterDataSource);
        }

        this.slaveSize = this.slaveDataSources.size();
    }

    protected DataSource determineTargetDataSource() {

        DataSource dataSource;

        MasterSalveKey lookupKey = determineCurrentLookupKey();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】,datasource current lookupKey is "
                    + lookupKey + "!");
        }

        // 如果开始事务，都走主库
        if (MasterSalveKey.TRANSACTION.equals(lookupKey)) {
            dataSource = this.masterDataSource;
        } else {
            if (MasterSalveKey.FORCE.equals(lookupKey) || MasterSalveKey.MASTER.equals(lookupKey)) {
                dataSource = this.masterDataSource;
            } else {
                dataSource = loadBalance();
            }
        }

        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
        }

        if (this.logger.isDebugEnabled()) {

            if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
                org.apache.tomcat.jdbc.pool.DataSource d = (org.apache.tomcat.jdbc.pool.DataSource) dataSource;
                this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】,datasource : " + d.getUrl()
                        + "!");
            }
        }

        return dataSource;
    }

    protected abstract MasterSalveKey determineCurrentLookupKey();

    protected abstract DataSource loadBalance();

    protected int getSlaveSize() {
        return slaveSize;
    }

    public void setMasterDataSource(DataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    public void setSlaveDataSources(List<DataSource> slaveDataSources) {
        this.slaveDataSources = slaveDataSources;
    }

}
