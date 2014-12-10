package com.pinoo.core.mybatis;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import com.pinoo.core.mybatis.binding.MapperRegistry;
import com.pinoo.core.mybatis.cache.ICache;

public class ConfigurationExtend extends Configuration {

    protected MapperRegistry mapperRegistry = new MapperRegistry(this);

    protected ICache cache;

    public void addMappers(String packageName, Class<?> superType) {
        mapperRegistry.addMappers(packageName, superType);
    }

    public void addMappers(String packageName) {
        mapperRegistry.addMappers(packageName);
    }

    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }

    public ICache getCache() {
        return cache;
    }

    public void setCache(ICache cache) {
        this.cache = cache;
    }

}
