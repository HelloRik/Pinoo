package com.pinoo.core.dao;

import java.util.List;

/**
 * DAO基本操作
 * 
 * @author jun.ju
 * 
 * @param <T>
 * @param <ID>
 */
public interface IBaseDao<T, ID> {

    public T load(ID id) throws Exception;

    public List<T> loads(List<ID> objs) throws Exception;

    public T insert(T model) throws Exception;

    public boolean update(T model) throws Exception;

    public boolean delete(ID id) throws Exception;

}
