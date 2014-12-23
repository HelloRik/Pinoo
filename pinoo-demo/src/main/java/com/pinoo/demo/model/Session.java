package com.pinoo.demo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.pinoo.storage.mongodb.annotation.model.ColumnKey;
import com.pinoo.storage.mongodb.annotation.model.ListSizeKey;
import com.pinoo.storage.mongodb.annotation.model.ModelInfo;
import com.pinoo.storage.mongodb.annotation.model.SortKey;

@Document(collection = "session")
@ModelInfo()
public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String STR_LIST_NAME_MSGS = "msgs";

    public final static String STR_LIST_NAME_MIDS = "mids";

    public final static String STR_NAME_TYPE = "type";

    public final static String STR_NAME_CTIME = "ctime";

    public final static String STR_NAME_UTIME = "utime";

    public final static String STR_DB_NAME_ID = "_id";

    @Id
    private long id;

    @ColumnKey(isListData = true)
    private List<Long> mids = new ArrayList<Long>();

    @ColumnKey(isListData = true)
    private List<Long> msgs = new ArrayList<Long>();

    @Field("mid_size")
    @ListSizeKey(listName = STR_LIST_NAME_MIDS)
    private int midSize = 0;

    @Field("msg_size")
    @ListSizeKey(listName = STR_LIST_NAME_MSGS)
    private int msgSize = 0;

    private int type;

    private Map<String, String> ext = new HashMap<String, String>();

    private long ctime;

    @SortKey
    private long utime;

    public List<Long> getMids() {
        return mids;
    }

    public void setMids(List<Long> mids) {
        this.mids = mids;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getUtime() {
        return utime;
    }

    public void setUtime(long utime) {
        this.utime = utime;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<String, String> getExt() {
        return Collections.unmodifiableMap(ext);
    }

    public void setExt(Map<String, String> ext) {
        if (ext != null)
            this.ext.putAll(ext);
    }

    public List<Long> getMsgs() {
        return msgs;
    }

    public void setMsgs(List<Long> msgs) {
        this.msgs = msgs;
    }

    public int getMidSize() {
        return midSize;
    }

    public void setMidSize(int midSize) {
        this.midSize = midSize;
    }

    public int getMsgSize() {
        return msgSize;
    }

    public void setMsgSize(int msgSize) {
        this.msgSize = msgSize;
    }

    @Override
    public String toString() {
        return "Session [id=" + id + ", mids=" + mids + ", msgs=" + msgs + ", midSize=" + midSize + ", msgSize="
                + msgSize + ", type=" + type + ", ext=" + ext + ", ctime=" + ctime + ", utime=" + utime + "]";
    }

}
