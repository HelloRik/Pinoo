package com.pinoo.core.mybatis.builder;

import org.apache.ibatis.mapping.SqlCommandType;

import com.pinoo.core.mybatis.binding.MethodSignature;

public class DeleteMappedStatementBuilder extends AbstractMappedStatementBuilder {

    public DeleteMappedStatementBuilder(String mappedStatementId, MethodSignature method) {
        super(mappedStatementId, method);
    }

    @Override
    protected String getSql() {
        StringBuffer sqlBuffer = new StringBuffer("delete from `" + method.getTableName() + "`");
        sqlBuffer.append(" where " + method.getPrimaryFieldInfo().getDbName() + "=#{"
                + method.getPrimaryFieldInfo().getName() + "}");
        return sqlBuffer.toString();
    }

    @Override
    protected SqlCommandType getSqlCommandType() {
        return SqlCommandType.DELETE;
    }
}