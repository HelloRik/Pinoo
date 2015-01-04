package com.pinoo.demo.model;

import java.io.Serializable;

import com.pinoo.storage.mybatis.annotation.model.PrimaryKey;

public class Product implements Serializable {

    @PrimaryKey
    private long id;

    private String productName;

    private int brandId;

    /**
     * @return Returns the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     *            The id to set.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return Returns the productName
     */
    public String getProductName() {
        return productName;
    }

    /**
     * @param productName
     *            The productName to set.
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * @return Returns the brandId
     */
    public int getBrandId() {
        return brandId;
    }

    /**
     * @param brandId
     *            The brandId to set.
     */
    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Product [id=" + id + ", productName=" + productName + ", brandId=" + brandId + "]";
    }

}
