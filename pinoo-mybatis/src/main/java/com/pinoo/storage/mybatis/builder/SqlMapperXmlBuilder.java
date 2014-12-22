package com.pinoo.storage.mybatis.builder;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.mapping.SqlCommandType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.pinoo.common.annotation.model.FieldInfo;
import com.pinoo.common.utils.AnnotationScaner;
import com.pinoo.common.utils.ReflectionUtil;
import com.pinoo.storage.mybatis.annotation.method.Method;
import com.pinoo.storage.mybatis.annotation.method.MethodParam;
import com.pinoo.storage.mybatis.binding.MethodSignature;

public class SqlMapperXmlBuilder {

    private MethodSignature methodSignature;

    private String resultMapName;

    public SqlMapperXmlBuilder(MethodSignature methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String buildXml() throws Exception {
        Document document = createDocument();
        Element root = document.createElement("mapper");
        root.setAttribute("namespace", methodSignature.getMapperInterface().getName());
        document.appendChild(root);

        buildResultMap(document, root);
        buildMethods(document, root);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        // transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
        // "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(bos));

        String xml = bos.toString();
        xml = xml.substring(0, xml.indexOf(">") + 1)
                + "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
                + xml.substring(xml.indexOf(">") + 1);

        return xml;
    }

    private void buildMethods(Document document, Element root) throws Exception {
        for (java.lang.reflect.Method method : this.methodSignature.getMapperInterface().getMethods()) {
            String methodName = method.getName();
            Method methodAnn = method.getAnnotation(Method.class);
            if (methodAnn != null) {
                SqlCommandType sqlCommandType = methodAnn.sqlCommandType();
                switch (sqlCommandType) {
                case SELECT:
                    buildSelectCommand(document, root, method.getName(), methodAnn, method);
                    break;
                case INSERT:
                    buildInsertCommand(document, root, method.getName());
                    break;
                case UPDATE:
                    buildUpdateCommand(document, root, method.getName());
                    break;
                case DELETE:
                    buildDeleteCommand(document, root, method.getName());
                    break;
                }

            }
        }
    }

    private void buildDeleteCommand(Document document, Element root, String id) throws Exception {
        Element deleteCommand = createElement(document, root, "delete");
        deleteCommand.setAttribute("id", id);

        StringBuffer sqlBuffer = new StringBuffer("delete from `" + methodSignature.getTableName() + "`");
        sqlBuffer.append(" where " + methodSignature.getPrimaryFieldInfo().getDbName() + "=#{"
                + methodSignature.getPrimaryFieldInfo().getName() + "}");
        deleteCommand.setTextContent(sqlBuffer.toString());
    }

    private void buildUpdateCommand(Document document, Element root, String id) throws Exception {
        Element updateCommand = createElement(document, root, "update");
        updateCommand.setAttribute("id", id);
        updateCommand.setAttribute("parameterType", methodSignature.getEntityClass().getName());

        StringBuffer sqlBuffer = new StringBuffer("update `" + methodSignature.getTableName() + "` set ");

        for (int i = 0; i < methodSignature.getFields().size(); i++) {
            if (!methodSignature.getFields().get(i).getDbName()
                    .equals(methodSignature.getPrimaryFieldInfo().getDbName())) {
                sqlBuffer.append(" `" + methodSignature.getFields().get(i).getDbName() + "`=");
                sqlBuffer.append("#{" + methodSignature.getFields().get(i).getName() + "}");
                if (i < methodSignature.getFields().size() - 1)
                    sqlBuffer.append(",");
            }
        }
        sqlBuffer.append(" where " + methodSignature.getPrimaryFieldInfo().getDbName() + "=#{"
                + methodSignature.getPrimaryFieldInfo().getName() + "}");
        updateCommand.setTextContent(sqlBuffer.toString());
    }

    private void buildInsertCommand(Document document, Element root, String id) throws Exception {
        Element insertCommand = createElement(document, root, "insert");
        insertCommand.setAttribute("id", id);
        insertCommand.setAttribute("useGeneratedKeys", Boolean.TRUE.toString());
        insertCommand.setAttribute("keyProperty", methodSignature.getPrimaryFieldInfo().getName());
        insertCommand.setAttribute("parameterType", methodSignature.getEntityClass().getName());

        StringBuffer sb = new StringBuffer("insert into `" + methodSignature.getTableName() + "` (");

        for (int i = 0; i < methodSignature.getFields().size(); i++) {
            FieldInfo info = methodSignature.getFields().get(i);
            sb.append("`" + info.getDbName() + "`");
            if (i < methodSignature.getFields().size() - 1)
                sb.append(",");
        }
        sb.append(") values (");
        for (int i = 0; i < methodSignature.getFields().size(); i++) {
            FieldInfo info = methodSignature.getFields().get(i);
            sb.append("#{" + info.getName() + "}");
            if (i < methodSignature.getFields().size() - 1)
                sb.append(",");
        }
        sb.append(")");
        insertCommand.setTextContent(sb.toString());
    }

    private void buildSelectCommand(Document document, Element root, String id, Method methodAnn,
            java.lang.reflect.Method method) throws Exception {

        boolean isReturnCount = ReflectionUtil.isMethodReturnCount(method);

        Element selectCommand = createElement(document, root, "select");
        selectCommand.setAttribute("id", id);

        if (StringUtils.isNotEmpty(methodAnn.parameterMap())) {
            selectCommand.setAttribute("parameterMap", methodAnn.parameterMap());
        } else if (StringUtils.isNotEmpty(methodAnn.parameterType())) {
            selectCommand.setAttribute("parameterType", methodAnn.parameterType());
        }

        if (StringUtils.isNotEmpty(methodAnn.resultMap())) {
            selectCommand.setAttribute("resultMap", methodAnn.resultMap());
        } else if (StringUtils.isNotEmpty(methodAnn.resultType())) {
            selectCommand.setAttribute("resultType", methodAnn.resultType());
        } else if (isReturnCount) {
            if (method.getReturnType().equals(Long.class))
                selectCommand.setAttribute("resultType", "Long");
            else
                selectCommand.setAttribute("resultType", "Integer");
        } else {
            selectCommand.setAttribute("resultMap", resultMapName);
        }

        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotEmpty(methodAnn.sql())) {
            sb.append(methodAnn.sql());
        } else if (isReturnCount) {
            sb.append("select count(*) from " + this.methodSignature.getTableName());
        } else {
            sb.append("select ");
            for (int i = 0; i < methodSignature.getFields().size(); i++) {
                FieldInfo info = methodSignature.getFields().get(i);
                sb.append("`" + info.getDbName() + "`");
                if (i < methodSignature.getFields().size() - 1)
                    sb.append(",");
            }
            sb.append(" from " + this.methodSignature.getTableName());
        }
        boolean isFirst = true;
        List<MethodParam> paramAnns = AnnotationScaner.scanMethodParam(method, MethodParam.class);

        for (MethodParam p : paramAnns) {
            String paramName = p.value();
            for (FieldInfo info : methodSignature.getFields()) {
                if (info.getName().equals(paramName)) {
                    if (isFirst) {
                        sb.append(" where ");
                        isFirst = false;
                    } else {
                        sb.append(" and ");
                    }
                    sb.append("`" + info.getDbName() + "`" + p.sign() + "#{" + paramName + "}");
                    continue;
                }
            }

        }
        selectCommand.setTextContent(sb.toString());
    }

    private void buildResultMap(Document document, Element root) throws Exception {
        Element resultMap = createElement(document, root, "resultMap");
        resultMapName = this.methodSignature.getEntityClass().getSimpleName() + "Result";
        // resultMapName = resultMapName.substring(0, 1).toLowerCase() +
        // resultMapName.substring(1);
        resultMap.setAttribute("id", resultMapName);
        resultMap.setAttribute("type", this.methodSignature.getEntityClass().getName());

        for (FieldInfo info : this.methodSignature.getFields()) {
            Element result = createElement(document, resultMap, "result");
            result.setAttribute("property", info.getName());
            result.setAttribute("column", info.getDbName());
        }
    }

    private Document createDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    private Element createElement(Document document, Element parent, String tags) throws Exception {
        Element e = document.createElement(tags);
        if (parent != null)
            parent.appendChild(e);
        return e;
    }

}
