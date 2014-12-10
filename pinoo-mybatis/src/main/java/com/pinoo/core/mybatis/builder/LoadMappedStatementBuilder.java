package com.pinoo.core.mybatis.builder;

import org.apache.ibatis.mapping.SqlCommandType;

import com.pinoo.core.mybatis.binding.MethodSignature;

public class LoadMappedStatementBuilder extends AbstractMappedStatementBuilder {

    public LoadMappedStatementBuilder(String mappedStatementId, MethodSignature method) {
        super(mappedStatementId, method);
    }

    @Override
    protected String getSql() {
        StringBuffer sqlBuffer = new StringBuffer("select");
        for (int i = 0; i < method.getFields().size(); i++) {
            sqlBuffer.append(" `" + method.getFields().get(i).getDbName() + "`");
            if (i < method.getFields().size() - 1)
                sqlBuffer.append(",");
        }
        sqlBuffer.append(" from `" + method.getTableName() + "` where `" + method.getPrimaryFieldInfo().getDbName()
                + "` = #{id}");
        return sqlBuffer.toString();
    }

    @Override
    protected SqlCommandType getSqlCommandType() {
        return SqlCommandType.SELECT;
    }

    // @Override
    // public void build() {
    //
    // Configuration configuration = method.getConfiguration();
    //
    // // 构建SqlSource
    // List<SqlNode> contents = new ArrayList<SqlNode>();
    // StringBuffer sqlBuffer = new StringBuffer("select");
    // for (int i = 0; i < method.getFields().size(); i++) {
    // sqlBuffer.append(" `" + method.getFields().get(i).getDbName() + "`");
    // if (i < method.getFields().size() - 1)
    // sqlBuffer.append(",");
    // }
    // sqlBuffer.append(" from `" + method.getTableName() + "` where `" +
    // method.getPrimaryFieldInfo().getDbName()
    // + "` = #{id}");
    // TextSqlNode textSqlNode = new TextSqlNode(sqlBuffer.toString());
    // contents.add(textSqlNode);
    // MixedSqlNode rootSqlNode = new MixedSqlNode(contents);
    // SqlSource sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    // // 构建 MappedStatement
    // MappedStatement.Builder statementBuilder = new
    // MappedStatement.Builder(configuration, mappedStatementId,
    // sqlSource, SqlCommandType.SELECT);
    //
    // // statementBuilder.resource(resource);
    // // statementBuilder.fetchSize(fetchSize);
    // statementBuilder.statementType(StatementType.PREPARED);
    // statementBuilder.keyGenerator(new NoKeyGenerator());
    // // statementBuilder.keyProperty(keyProperty);
    // // statementBuilder.keyColumn(keyColumn);
    // // statementBuilder.databaseId(databaseId);
    // statementBuilder.lang(new XMLLanguageDriver());
    // statementBuilder.resultOrdered(false);
    // statementBuilder.timeout(configuration.getDefaultStatementTimeout());
    //
    // // 参数类型
    // List<ParameterMapping> parameterMappings = new
    // ArrayList<ParameterMapping>();
    // ParameterMap.Builder inlineParameterMapBuilder = new
    // ParameterMap.Builder(configuration, statementBuilder.id()
    // + "-Inline", Long.class, parameterMappings);
    // statementBuilder.parameterMap(inlineParameterMapBuilder.build());
    //
    // // 结果类型
    // List<ResultMap> resultMaps = new ArrayList<ResultMap>();
    // ResultMap.Builder inlineResultMapBuilder = new
    // ResultMap.Builder(configuration, statementBuilder.id()
    // + "-Inline", method.getEntityClass(), new ArrayList<ResultMapping>(),
    // null);
    // resultMaps.add(inlineResultMapBuilder.build());
    // statementBuilder.resultMaps(resultMaps);
    // // statementBuilder.resultSetType(ResultSetType.SCROLL_INSENSITIVE);
    //
    // // statement cache
    // statementBuilder.flushCacheRequired(false);
    // statementBuilder.useCache(true);
    // // statementBuilder.cache(cache);
    //
    // MappedStatement statement = statementBuilder.build();
    // configuration.addMappedStatement(statement);
    // }

}
