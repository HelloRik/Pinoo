package com.pinoo.storage.mongodb.dao;

import java.util.List;

/**
 * DAO基本操作
 * 
 * @Filename: IBaseDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public interface IBaseDao<T, ID> {

    public T load(ID id) throws Exception;

    public List<T> loads(List<ID> objs) throws Exception;

    public T insert(T model) throws Exception;

    public boolean update(T model) throws Exception;

    public boolean delete(ID id) throws Exception;

}
