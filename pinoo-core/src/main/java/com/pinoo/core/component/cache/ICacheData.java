package com.pinoo.core.component.cache;

public interface ICacheData {

    public boolean has(String key);

    public <T> T getObject(String key, Class<T> clz);

    public boolean setObject(String key, Object obj);

    public boolean delete(String key);

}
