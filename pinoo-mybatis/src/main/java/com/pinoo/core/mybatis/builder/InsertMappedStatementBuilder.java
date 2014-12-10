package com.pinoo.core.mybatis.builder;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;

import com.pinoo.core.mybatis.binding.MethodSignature;

public class InsertMappedStatementBuilder extends AbstractMappedStatementBuilder {

    public InsertMappedStatementBuilder(String mappedStatementId, MethodSignature method) {
        super(mappedStatementId, method);
    }

    @Override
    protected String getSql() {
        StringBuffer sqlBuffer = new StringBuffer("insert into `" + method.getTableName() + "`");
        sqlBuffer.append("(");
        for (int i = 0; i < method.getFields().size(); i++) {
            sqlBuffer.append(" `" + method.getFields().get(i).getDbName() + "`");
            if (i < method.getFields().size() - 1)
                sqlBuffer.append(",");
        }
        sqlBuffer.append(")values(");
        for (int i = 0; i < method.getFields().size(); i++) {
            sqlBuffer.append("#{" + method.getFields().get(i).getName() + "}");
            if (i < method.getFields().size() - 1)
                sqlBuffer.append(",");
        }
        sqlBuffer.append(")");
        return sqlBuffer.toString();
    }

    @Override
    protected SqlCommandType getSqlCommandType() {
        return SqlCommandType.INSERT;
    }

    @Override
    protected KeyGenerator getKeyGenerator() {
        return new Jdbc3KeyGenerator();
    }

    @Override
    protected String getKeyProperty() {
        return "id";
    }
}
