package com.pinoo.storage.mybatis.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.pinoo.storage.mybatis.datasource.MasterSalveKey;

@Intercepts({ @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
        RowBounds.class, ResultHandler.class }) })
public class ExecutorQueryMethodInterceptor extends AbstractExecutorInterceptor {

    @Override
    protected MasterSalveKey determineOption(MappedStatement mappedStatement) {
        return MasterSalveKey.SLAVE;
    }

}
