package com.pinoo.storage.mongodb.dao;

import java.util.List;

import org.springframework.data.mongodb.core.query.Query;

/**
 * Mongodb的基本操作
 * 
 * @Filename: IMongoDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 * @param <T>
 * @param <ID>
 */
public interface IMongoDao<T, ID> extends IBaseDao<T, ID> {

    /**
     * 获取数组字段的个数
     * 
     * @param id
     * @param listName
     * @return
     * @throws Exception
     */
    public int getListCount(ID id, String listName) throws Exception;

    /**
     * 获取数组字段内的ID
     * 
     * @param id
     * @param listName
     * @return
     * @throws Exception
     */
    public List<Long> getList(ID id, String listName) throws Exception;

    /**
     * 添加一个值到数组字段中
     * 
     * @param id
     * @param listName
     * @param value
     * @return
     * @throws Exception
     */
    public boolean pushToList(ID id, String listName, long value) throws Exception;

    /**
     * 删除数组字段的值
     * 
     * @param id
     * @param listName
     * @param value
     * @return
     * @throws Exception
     */
    public boolean removeToList(ID id, String listName, long value) throws Exception;

    @Deprecated
    public boolean existList(ID id, String listName, List<Long> objs) throws Exception;

    /**
     * 查询对象
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public T query(Query query) throws Exception;

    /**
     * 查询对象列表ID
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public List<ID> queryForList(Query query) throws Exception;

    /**
     * 获取对象列表数
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public long queryForListCount(Query query) throws Exception;

    /**
     * 批量删除
     * 
     * @param id
     * @param listName
     * @param values
     * @return
     * @throws Exception
     */
    public boolean removeAllToList(ID id, String listName, List<Long> values) throws Exception;

}
