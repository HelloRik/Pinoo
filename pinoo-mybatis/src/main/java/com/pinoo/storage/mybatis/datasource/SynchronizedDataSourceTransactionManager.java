package com.pinoo.storage.mybatis.datasource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import com.pinoo.storage.mybatis.SessionDataSourceHelper;

/**
 * 事务管理器--用于同步当前线程读写的标识
 * 
 * @Filename: SynchronizedReadWriteDataSourceTransactionManager.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public class SynchronizedDataSourceTransactionManager extends DataSourceTransactionManager {

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {

        SessionDataSourceHelper.setOptionKey(MasterSalveKey.TRANSACTION);
        super.doBegin(transaction, definition);

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】 is begin transaction!");
        }
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        try {
            super.doCleanupAfterCompletion(transaction);
        } finally {

            if (this.logger.isDebugEnabled()) {
                MasterSalveKey key = SessionDataSourceHelper.getOptionKey();
                this.logger.debug("Thread ID : 【" + Thread.currentThread().getId()
                        + "】 is completion transaction! remove key : " + key);
            }

            // 事务结束后取消标记
            SessionDataSourceHelper.removeOptionKey();
        }
    }
}
