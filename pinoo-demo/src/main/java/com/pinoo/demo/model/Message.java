package com.pinoo.demo.model;

import java.io.Serializable;

import com.pinoo.core.mybatis.annotation.model.ColumnKey;
import com.pinoo.core.mybatis.annotation.model.PrimaryKey;
import com.pinoo.core.mybatis.annotation.model.SortKey;

public class Message implements Serializable {

    private static final long serialVersionUID = -8525932566557045714L;

    @PrimaryKey
    private long id;

    private String title;

    private String content;

    // @Field("create_by")
    private int type;

    // @Field("session_id")
    private int status;

    @SortKey
    @ColumnKey(column = "add_time")
    private long addTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    /**
     * @return Returns the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     *            The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Returns the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return Returns the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     *            The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return Returns the addTime
     */
    public long getAddTime() {
        return addTime;
    }

    /**
     * @param addTime
     *            The addTime to set.
     */
    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    /**
     * @param content
     *            The content to set.
     */
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message [id=" + id + ", title=" + title + ", content=" + content + ", type=" + type + ", status="
                + status + ", addTime=" + addTime + "]";
    }

}
