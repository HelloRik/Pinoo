package com.pinoo.core.mybatis.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;

import com.pinoo.core.mybatis.datasource.MasterSalveKey;

@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
public class ExecutorUpdateMethodInterceptor extends AbstractExecutorInterceptor {

    @Override
    protected MasterSalveKey determineOption(MappedStatement mappedStatement) {
        return MasterSalveKey.MASTER;
    }

}
