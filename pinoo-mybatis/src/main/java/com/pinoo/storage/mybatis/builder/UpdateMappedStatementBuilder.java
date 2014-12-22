package com.pinoo.storage.mybatis.builder;

import org.apache.ibatis.mapping.SqlCommandType;

import com.pinoo.storage.mybatis.binding.MethodSignature;

@Deprecated
public class UpdateMappedStatementBuilder extends AbstractMappedStatementBuilder {

    public UpdateMappedStatementBuilder(String mappedStatementId, MethodSignature method) {
        super(mappedStatementId, method);
    }

    @Override
    protected String getSql() {
        StringBuffer sqlBuffer = new StringBuffer("update `" + method.getTableName() + "` set ");
        for (int i = 0; i < method.getFields().size(); i++) {
            if (!method.getFields().get(i).getDbName().equals(method.getPrimaryFieldInfo().getDbName())) {
                sqlBuffer.append(" `" + method.getFields().get(i).getDbName() + "`=");
                sqlBuffer.append("#{" + method.getFields().get(i).getName() + "}");
                if (i < method.getFields().size() - 1)
                    sqlBuffer.append(",");
            }
        }
        sqlBuffer.append(" where " + method.getPrimaryFieldInfo().getDbName() + "=#{"
                + method.getPrimaryFieldInfo().getName() + "}");
        return sqlBuffer.toString();
    }

    @Override
    protected SqlCommandType getSqlCommandType() {
        return SqlCommandType.UPDATE;
    }

}
