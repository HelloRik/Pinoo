package com.pinoo.core.mybatis.builder;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.pinoo.core.mybatis.binding.MethodSignature;

/**
 * MappedStatement构造器
 * 
 * @Filename: AbstractMappedStatementBuilder.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
@Deprecated
public abstract class AbstractMappedStatementBuilder implements IMappedStatementBuilder {

    protected String mappedStatementId;

    protected MethodSignature method;

    protected AbstractMappedStatementBuilder(String mappedStatementId, MethodSignature method) {
        this.mappedStatementId = mappedStatementId;
        this.method = method;
    }

    protected abstract String getSql();

    protected abstract SqlCommandType getSqlCommandType();

    protected KeyGenerator getKeyGenerator() {
        return new NoKeyGenerator();
    }

    protected String getKeyProperty() {
        return null;
    }

    public void build() {
        Configuration configuration = method.getConfiguration();

        // 构建SqlSource
        List<SqlNode> contents = new ArrayList<SqlNode>();
        String sql = getSql();
        TextSqlNode textSqlNode = new TextSqlNode(sql);
        contents.add(textSqlNode);
        MixedSqlNode rootSqlNode = new MixedSqlNode(contents);
        SqlSource sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
        // 构建 MappedStatement
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, mappedStatementId,
                sqlSource, getSqlCommandType());

        // statementBuilder.resource(resource);
        // statementBuilder.fetchSize(fetchSize);
        statementBuilder.statementType(StatementType.PREPARED);
        statementBuilder.keyGenerator(getKeyGenerator());
        statementBuilder.keyProperty(getKeyProperty());
        // statementBuilder.keyColumn(keyColumn);
        // statementBuilder.databaseId(databaseId);
        statementBuilder.lang(new XMLLanguageDriver());
        statementBuilder.resultOrdered(false);
        statementBuilder.timeout(configuration.getDefaultStatementTimeout());

        // 参数类型
        List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
        ParameterMap.Builder inlineParameterMapBuilder = new ParameterMap.Builder(configuration, statementBuilder.id()
                + "-Inline", Long.class, parameterMappings);
        statementBuilder.parameterMap(inlineParameterMapBuilder.build());

        // 结果类型
        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(configuration, statementBuilder.id()
                + "-Inline", method.getEntityClass(), new ArrayList<ResultMapping>(), null);
        resultMaps.add(inlineResultMapBuilder.build());
        statementBuilder.resultMaps(resultMaps);
        // statementBuilder.resultSetType(ResultSetType.SCROLL_INSENSITIVE);

        // statement cache
        statementBuilder.flushCacheRequired(false);
        statementBuilder.useCache(true);
        // statementBuilder.cache(cache);

        MappedStatement statement = statementBuilder.build();
        configuration.addMappedStatement(statement);
    }
}
