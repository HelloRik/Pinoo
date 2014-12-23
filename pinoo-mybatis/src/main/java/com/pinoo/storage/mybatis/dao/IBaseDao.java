package com.pinoo.storage.mybatis.dao;

import com.pinoo.mapping.MethodType;
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

    @Method(type = MethodType.SELECT)
    public T load(@MethodParam("id") long id);

    @Method(type = MethodType.INSERT)
    public int insert(T model);

    @Method(type = MethodType.UPDATE)
    public int update(T model);

    @Method(type = MethodType.DELETE)
    public int delete(long id);

}
