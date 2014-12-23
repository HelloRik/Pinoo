package com.pinoo.storage.mybatis.dao;

import com.pinoo.annotation.method.MethodParam;
import com.pinoo.annotation.method.MethodProxy;
import com.pinoo.mapping.MethodType;

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

    @MethodProxy(type = MethodType.SELECT)
    public T load(@MethodParam("id") long id);

    @MethodProxy(type = MethodType.INSERT)
    public int insert(T model);

    @MethodProxy(type = MethodType.UPDATE)
    public int update(T model);

    @MethodProxy(type = MethodType.DELETE)
    public int delete(long id);

}
