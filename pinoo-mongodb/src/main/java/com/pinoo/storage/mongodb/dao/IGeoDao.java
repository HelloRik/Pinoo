package com.pinoo.storage.mongodb.dao;

import java.util.List;

import org.springframework.data.mongodb.core.geo.Metrics;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 坐标位置中 X为纬度,Y为经度
 * 
 * @Filename: IGeoDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public interface IGeoDao<T, ID> extends IMongoDao<T, ID> {

    /**
     * 获取坐标附近范围内的点
     * 
     * 查询距离单位默认为公里
     * 
     * @param x
     * @param y
     * @param radius
     * @return
     */
    public List<T> near(double x, double y, double radius) throws Exception;

    public List<T> near(double x, double y, double radius, Query query) throws Exception;

    public List<T> near(double x, double y, double radius, Metrics metrics) throws Exception;

    public List<T> near(double x, double y, double radius, Query query, Metrics metrics) throws Exception;

    /**
     * 获取坐标附近以半径为范围内的点
     * 
     * 半径单位为经纬度数
     * 
     * @param x
     * @param y
     * @param radius
     * @return
     * @throws Exception
     */
    public List<T> circle(double x, double y, double radius) throws Exception;

    public List<T> circle(double x, double y, double radius, Query query) throws Exception;

    /**
     * 获取以左下角和右上角坐标为矩形范围内的点
     * 
     * @param leftLowX
     * @param leftLowY
     * @param rightUpperX
     * @param rightUpperY
     * @return
     * @throws Exception
     */
    public List<T> box(double leftLowX, double leftLowY, double rightUpperX, double rightUpperY) throws Exception;

    public List<T> box(double leftLowX, double leftLowY, double rightUpperX, double rightUpperY, Query query)
            throws Exception;

}
