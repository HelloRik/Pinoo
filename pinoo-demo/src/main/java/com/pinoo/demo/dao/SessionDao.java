package com.pinoo.demo.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.pinoo.annotation.method.MethodProxy;
import com.pinoo.annotation.method.Page;
import com.pinoo.annotation.method.PageCursor;
import com.pinoo.annotation.method.PageSize;
import com.pinoo.demo.model.Session;
import com.pinoo.storage.mongodb.annotation.dao.ProxyDao;
import com.pinoo.storage.mongodb.dao.RedisCacheDao;

@ProxyDao
@Repository("sessionDao")
public class SessionDao extends RedisCacheDao<Session, Long> {

    @MethodProxy
    public List<Session> getAllList() {
        return null;
    };

    @MethodProxy
    public List<Session> getPageList(@Page int page, @PageSize int pageSize) {
        return null;
    };

    @MethodProxy
    public List<Session> getCursorList(@PageCursor long cursor, @PageSize int pageSize) {
        return null;
    };

}
