package com.pinoo.core.mybatis.dao;

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

    public T load(long id);

    public int insert(T model);

    public int update(T model);

    public int delete(long id);

}
