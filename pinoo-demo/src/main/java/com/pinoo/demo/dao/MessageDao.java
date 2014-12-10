package com.pinoo.demo.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.pinoo.core.mybatis.annotation.method.MethodParam;
import com.pinoo.core.mybatis.annotation.method.Page;
import com.pinoo.core.mybatis.annotation.method.PageCursor;
import com.pinoo.core.mybatis.annotation.method.PageSize;
import com.pinoo.core.mybatis.annotation.model.ModelInfo;
import com.pinoo.core.mybatis.dao.IBaseDao;
import com.pinoo.demo.model.Message;

@ModelInfo(entityClass = Message.class, tableName = "message")
public interface MessageDao extends IBaseDao<Message> {

    // public int insert(Message message);

    public List<Message> getMsgList(@MethodParam("status")
    @Param("status")
    int status, @MethodParam("type")
    @Param("type")
    int type, @PageCursor
    long cursor, @PageSize
    int pageSize);

    public List<Message> getMsgListByStatus(@MethodParam("status")
    @Param("status")
    int status, @PageCursor
    long cursor, @PageSize
    int pageSize);

    public List<Message> getMsgListByStatus(@MethodParam("status")
    @Param("status")
    int status, @Page
    int page, @PageSize
    int pageSize);

    public List<Message> getMsgListByType(@MethodParam("type")
    @Param("type")
    int type, @PageCursor
    long cursor, @PageSize
    int pageSize);
}
