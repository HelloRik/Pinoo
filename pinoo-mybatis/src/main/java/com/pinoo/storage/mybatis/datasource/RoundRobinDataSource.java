package com.pinoo.storage.mybatis.datasource;

import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.pinoo.storage.mybatis.SessionDataSourceHelper;

/**
 * 轮询读不同从库
 * 
 * @Filename: RoundRobinDataSource.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public class RoundRobinDataSource extends AbstractMasterSlaveDataSource {

    private AtomicInteger count = new AtomicInteger();

    @Override
    protected MasterSalveKey determineCurrentLookupKey() {
        return SessionDataSourceHelper.getOptionKey();
    }

    @Override
    protected DataSource loadBalance() {
        int index = Math.abs(count.incrementAndGet()) % getSlaveSize();
        return this.slaveDataSources.get(index);
    }

}
