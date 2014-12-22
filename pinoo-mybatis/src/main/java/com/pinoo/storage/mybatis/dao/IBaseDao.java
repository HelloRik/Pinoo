package com.pinoo.storage.mybatis.dao;

import org.apache.ibatis.mapping.SqlCommandType;

import com.pinoo.storage.mybatis.annotation.method.Method;
import com.pinoo.storage.mybatis.annotation.method.MethodParam;

/**
 * 所有DAO操作的基类
 * 
 * @Filename: IBaseDao.java
 * @Version: 1.0
 * @Author: jujun 鞠钧
 * @Email: hello_rik@sina.com
 * 
 */
public interface IBaseDao<T> {

    @Method(sqlCommandType = SqlCommandType.SELECT, parameterType = "Long")
    public T load(@MethodParam("id") long id);

    @Method(sqlCommandType = SqlCommandType.INSERT)
    public int insert(T model);

    @Method(sqlCommandType = SqlCommandType.UPDATE)
    public int update(T model);

    @Method(sqlCommandType = SqlCommandType.DELETE)
    public int delete(long id);

}
