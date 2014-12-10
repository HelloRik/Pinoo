package com.pinoo.common.annotation.model;

public class ListFieldInfo {

    private String fieldName;

    private boolean isPrivateKeySort;

    // key: com.downjoy.framework_list_msgIds_%s
    private String format;

    // key: com.downjoy.framework_count_msgIds_%s
    private String countFormat;

    private String sortDao;

    private String sortName;

    public ListFieldInfo(String fieldName, String format, String countFormat, boolean isPrivateKeySort, String sortDao,
            String sortName) {
        this.fieldName = fieldName;
        this.format = format;
        this.countFormat = countFormat;
        this.isPrivateKeySort = isPrivateKeySort;
        this.sortDao = sortDao;
        this.sortName = sortName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isPrivateKeySort() {
        return isPrivateKeySort;
    }

    public void setPrivateKeySort(boolean isPrivateKeySort) {
        this.isPrivateKeySort = isPrivateKeySort;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCountFormat() {
        return countFormat;
    }

    public void setCountFormat(String countFormat) {
        this.countFormat = countFormat;
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName;
    }

    public String getSortDao() {
        return sortDao;
    }

    public void setSortDao(String sortDao) {
        this.sortDao = sortDao;
    }

    @Override
    public String toString() {
        return "ListFieldInfo [fieldName=" + fieldName + ", isPrivateKeySort=" + isPrivateKeySort + ", format="
                + format + ", countFormat=" + countFormat + ", sortDao=" + sortDao + ", sortName=" + sortName + "]";
    }

}
