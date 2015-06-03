package com.pinoo.storage.mongodb.dao;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.geo.Box;
import org.springframework.data.mongodb.core.geo.Circle;
import org.springframework.data.mongodb.core.geo.Distance;
import org.springframework.data.mongodb.core.geo.GeoResult;
import org.springframework.data.mongodb.core.geo.GeoResults;
import org.springframework.data.mongodb.core.geo.Metrics;
import org.springframework.data.mongodb.core.geo.Point;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.pinoo.common.utils.ReflectionUtil;
import com.pinoo.storage.mongodb.annotation.model.GeoLocation;

public class GeoDao<T, ID> extends MongoDbDao<T, ID> implements IGeoDao<T, ID> {

    private final static String INDEX_INFO_NAME = "name";

    private final static String INDEX_INFO_BACKGROUND = "background";

    private final static String INDEX_POSTFIX = "_2d";

    /**
     * 索引字段名称
     */
    private String geoIndexName;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        // 获取索引字段
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                GeoLocation geo = field.getAnnotation(GeoLocation.class);
                if (geo == null)
                    continue;
                geoIndexName = field.getName();
            }
        }

        if (StringUtils.isEmpty(geoIndexName))
            throw new IllegalArgumentException("Geo models " + this.tableName + "is not Config GeoLocation Field!!!");

        boolean hasIndex = false;
        List<DBObject> objs = this.mongoTemplate.getCollection(this.tableName).getIndexInfo();
        for (DBObject o : objs) {
            String indexName = o.get(INDEX_INFO_NAME) != null ? (String) o.get(INDEX_INFO_NAME) : "";
            if (StringUtils.isNotEmpty(indexName) && indexName.equals(geoIndexName + INDEX_POSTFIX)) {
                hasIndex = true;
                break;
            }
        }

        // 建立索引
        if (!hasIndex) {
            DBObject keys = new BasicDBObject();
            // index.put(INDEX_INFO_NAME, geoIndexName);
            // index.put(INDEX_INFO_BACKGROUND, true);
            keys.put(this.geoIndexName, "2d");
            this.mongoTemplate.getCollection(this.tableName).createIndex(keys);
        }

    }

    @Override
    public List<T> near(double x, double y, double radius) throws Exception {
        return this.near(x, y, radius, null, Metrics.KILOMETERS);
    }

    @Override
    public List<T> near(double x, double y, double radius, Query query) throws Exception {
        return this.near(x, y, radius, query, Metrics.KILOMETERS);
    }

    @Override
    public List<T> near(double x, double y, double radius, Metrics metrics) throws Exception {
        return this.near(x, y, radius, null, metrics);
    }

    @Override
    public List<T> near(double x, double y, double radius, Query query, Metrics metrics) throws Exception {
        Point point = new Point(x, y);
        NearQuery nearQuery = NearQuery.near(point).maxDistance(new Distance(radius, metrics));

        if (query != null)
            nearQuery.query(query);

        GeoResults<T> geoResults = mongoTemplate.geoNear(nearQuery, this.entityClass);
        List<GeoResult<T>> list = geoResults.getContent();
        List<T> results = new ArrayList<T>();

        for (GeoResult<T> result : list) {
            T obj = result.getContent();
            results.add(obj);
        }
        return results;
    }

    @Override
    public List<T> circle(double x, double y, double radius) throws Exception {
        return circle(x, y, radius, null);
    }

    @Override
    public List<T> circle(double x, double y, double radius, Query query) throws Exception {
        Point point = new Point(x, y);
        Circle circle = new Circle(point, radius);
        if (query != null) {
            query.addCriteria(Criteria.where(geoIndexName).within(circle));
            return this.mongoTemplate.find(query, entityClass);
        } else {
            return this.mongoTemplate.find(new Query(Criteria.where(geoIndexName).within(circle)), entityClass);
        }
    }

    @Override
    public List<T> box(double leftLowX, double leftLowY, double rightUpperX, double rightUpperY) throws Exception {
        return box(leftLowX, leftLowY, rightUpperX, rightUpperY, null);
    }

    @Override
    public List<T> box(double leftLowX, double leftLowY, double rightUpperX, double rightUpperY, Query query)
            throws Exception {
        Box box = new Box(new Point(leftLowX, leftLowY), new Point(rightUpperX, rightUpperY));
        if (query != null) {
            query.addCriteria(Criteria.where(geoIndexName).within(box));
            return this.mongoTemplate.find(query, entityClass);
        } else {
            return this.mongoTemplate.find(new Query(Criteria.where(geoIndexName).within(box)), entityClass);
        }
    }

}
