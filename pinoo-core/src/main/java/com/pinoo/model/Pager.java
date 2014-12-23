package com.pinoo.model;

import java.io.Serializable;
import java.util.List;

public class Pager<T> implements Serializable {

    private long cursor;

    private long nextCursor;

    private int page;

    private int pageSize;

    private int total;

    private List<T> result;

    public Pager() {
    }

    public Pager(List<T> result, int page, int pageSize, int total) {
        this.result = result;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.cursor = 0;
        this.nextCursor = 0;
    }

    public Pager(List<T> result, long cursor, int pageSize, int total) {
        this.result = result;
        this.page = 0;
        this.pageSize = pageSize;
        this.total = total;
        this.cursor = cursor;
        this.nextCursor = 0;
    }

    public long getCursor() {
        return cursor;
    }

    public long getNextCursor() {
        return nextCursor;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<T> getResult() {
        return result;
    }

    public int getTotalPages() {
        return getPageSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getPageSize());
    }

    public boolean hasNextPage() {
        return getPage() + 1 <= getTotalPages();
    }

    public boolean isLastPage() {
        return !hasNextPage();
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setCursor(long cursor) {
        this.cursor = cursor;
    }

    public void setNextCursor(long nextCursor) {
        this.nextCursor = nextCursor;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setResult(List<T> result) {
        this.result = result;
    }

}
