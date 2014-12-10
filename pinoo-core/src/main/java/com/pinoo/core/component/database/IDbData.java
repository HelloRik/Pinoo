package com.pinoo.core.component.database;

public interface IDbData {

    public <T, ID> T load(ID id, Class<T> entityClass) throws Exception;

    public <T> T insert(T model, Class<T> entityClass) throws Exception;

    public <T> boolean update(T model, Class<T> entityClass) throws Exception;

    public <ID, T> T delete(ID id, Class<T> entityClass) throws Exception;

}
