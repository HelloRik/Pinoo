package com.pinoo.core.mybatis.cache;

import java.util.List;

import com.pinoo.common.annotation.model.FieldInfo;

public interface ICache {

    /**
     * 判断缓存中是否存在KEY
     * 
     * @param key
     * @return
     */
    public boolean hasKey(String key);

    /**
     * 设置对象缓存
     * 
     * @param key
     * @param obj
     */
    public void setObject(String key, Object obj);

    /**
     * 获取对象缓存
     * 
     * @param key
     * @return
     */
    public Object getObject(String key);

    /**
     * 移除对象缓存
     * 
     * @param key
     */
    public void removeObject(String key);

    /**
     * 设置计数缓存
     * 
     * @param key
     * @param count
     * @return
     */
    public void setCount(String key, int count);

    /**
     * 获取计数缓存
     * 
     * @param key
     * @return
     */
    public int getCount(String key);

    /**
     * 增加计数缓存
     * 
     * @param key
     */
    public void increaseCount(String key, int count);

    /**
     * 减少计数缓存
     * 
     * @param key
     */
    public void decreaseCount(String key, int count);

    /**
     * 设置列表缓存
     * 
     * @param listCacheKey
     * @param objects
     * @param primaryInfo
     * @param sortInfo
     * @throws Exception
     */
    public <T> void setList(String listCacheKey, List<T> objects, FieldInfo primaryInfo, FieldInfo sortInfo)
            throws Exception;

    /**
     * 获取列表缓存分页
     * 
     * @param listCacheKey
     * @param cursor
     * @param page
     * @param size
     * @return
     */
    public List<Long> getList(String listCacheKey, long cursor, int page, int size);

    /**
     * 将对象缓存放入列表缓存中
     * 
     * @param listCacheKey
     * @param obj
     * @param primaryInfo
     * @param sortInfo
     */
    public void addToList(String listCacheKey, Object obj, FieldInfo primaryInfo, FieldInfo sortInfo) throws Exception;

    /**
     * 将对象缓存从列表缓存中删除
     * 
     * @param listCacheKey
     * @param obj
     * @param primaryInfo
     * @throws Exception
     */
    public void removeToList(String listCacheKey, Object id) throws Exception;
}
