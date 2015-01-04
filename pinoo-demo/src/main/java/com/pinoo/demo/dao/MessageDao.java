package com.pinoo.demo.dao;

import java.util.List;

import com.pinoo.annotation.method.MethodParam;
import com.pinoo.annotation.method.MethodProxy;
import com.pinoo.annotation.method.Page;
import com.pinoo.annotation.method.PageCursor;
import com.pinoo.annotation.method.PageSize;
import com.pinoo.demo.model.Message;
import com.pinoo.storage.mybatis.annotation.model.ModelInfo;
import com.pinoo.storage.mybatis.dao.IBaseDao;

@ModelInfo(entityClass = Message.class, tableName = "message")
public interface MessageDao extends IBaseDao<Message> {

    @MethodProxy
    public List<Message> getAllMsgList(@Page int page, @PageSize int pageSize);

    @MethodProxy
    public List<Message> getMsgList(@MethodParam("status") int status, @MethodParam("type") int type,
            @PageCursor long cursor, @PageSize int pageSize);

    @MethodProxy
    public List<Message> getMsgListByStatusInCursor(@MethodParam("status") int status, @PageCursor long cursor,
            @PageSize int pageSize);

    @MethodProxy
    public List<Message> getMsgListByStatusInPage(@MethodParam("status") int status, @Page int page,
            @PageSize int pageSize);

    @MethodProxy
    public List<Message> getMsgListByType(@MethodParam("type") int type, @PageCursor long cursor, @PageSize int pageSize);

    @MethodProxy
    public int getMsgCountByType(@MethodParam("type") int type);

    @MethodProxy(sql = "select * from message where type = #{type} and status = #{status}")
    public List<Message> testGetList(@MethodParam("type") int type, @MethodParam("status") int status);

}
