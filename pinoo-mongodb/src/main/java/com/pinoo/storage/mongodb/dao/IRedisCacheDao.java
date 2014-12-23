package com.pinoo.storage.mongodb.dao;

import java.util.List;

/**
 * 
 * 
 * @Filename: IRedisCacheDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public interface IRedisCacheDao<T, K> extends IMongoDao<T, K> {

    public List<Long> getPageList(K id, String listName, int page, int pageSize) throws Exception;

    public List<Long> getList(K id, String listName, long cursor, int pageSize) throws Exception;

    // public List<K> queryForList(DBObject query, int page, int pageSize)
    // throws Exception;
    //
    // public List<K> queryForListByCursor(DBObject query, long cursor, int
    // pageSize) throws Exception;
}
