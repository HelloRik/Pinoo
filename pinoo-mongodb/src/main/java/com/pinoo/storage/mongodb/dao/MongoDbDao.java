package com.pinoo.storage.mongodb.dao;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.pinoo.common.utils.ReflectionUtil;
import com.pinoo.storage.mongodb.annotation.model.ColumnKey;
import com.pinoo.storage.mongodb.annotation.model.FieldInfo;
import com.pinoo.storage.mongodb.annotation.model.IdentityType;
import com.pinoo.storage.mongodb.annotation.model.ListFieldInfo;
import com.pinoo.storage.mongodb.annotation.model.ListSizeKey;
import com.pinoo.storage.mongodb.annotation.model.ModelInfo;
import com.pinoo.storage.mongodb.annotation.model.SortKey;

/**
 * MONGODB的CURD等基本操作实现类
 * 
 * @Filename: MongoDbDao.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 * @param <T>实体类型
 * @param <ID>主键类型
 * 
 *        目前数组类型的字段只支持long类型,可能以后会有String等
 */
public abstract class MongoDbDao<T, ID> implements InitializingBean, ApplicationContextAware, IMongoDao<T, ID> {

    private Logger logger = LoggerFactory.getLogger(MongoDbDao.class);

    private final static String loggerTypeTag = "DB";

    protected ApplicationContext applicationContext;

    @Autowired
    @Qualifier("mongoTemplate")
    protected MongoTemplate mongoTemplate;

    protected String daoClassName = this.getClass().getSimpleName();

    /**
     * 实体类型
     */
    protected Class<T> entityClass;

    /**
     * 对象模型信息
     */
    protected ModelInfo modelInfo;

    protected String seqTableName;

    protected String tableName;

    protected PropertyDescriptor[] propertyDescriptors;

    // 所有的列信息
    protected List<FieldInfo> fields = new ArrayList<FieldInfo>();

    protected Map<String, FieldInfo> fieldsMap = new HashMap<String, FieldInfo>();

    // 主键
    protected FieldInfo primaryFieldInfo;

    // 排序
    protected FieldInfo sortFieldInfo;

    protected FieldInfo ctimeFieldInfo;

    protected FieldInfo utimeFieldInfo;

    /**
     * 数据库主键自增类型
     */
    protected IdentityType identityType;

    protected Map<String, ListFieldInfo> listFieldInfos = new HashMap<String, ListFieldInfo>();

    protected Map<String, FieldInfo> listSizeInfo = new HashMap<String, FieldInfo>();

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        // 0、实体化T类型
        this.initEntityClass();
        logger.info(daoClassName + "，entityClass: " + this.entityClass);
        // 1、初始化数据库信息
        this.initModelInfo();
        logger.info(daoClassName + "，identityType:{}", new Object[] { identityType });
        // 2、获取泛型类字段信息
        this.initFields();
        logger.info(daoClassName + "，init_fields: " + this.fields);
        // 3、获取主键字段
        this.initPrimaryFields();
        logger.info(daoClassName + "，primary_field: " + this.primaryFieldInfo);
        // 4、获取排序字段
        this.initSortField();
        logger.info(daoClassName + "，sortField: " + this.sortFieldInfo);
        // 5、获取数组字段
        initListInfo();
        logger.info(daoClassName + "，list field: " + this.listFieldInfos);
    }

    private void initModelInfo() throws Exception {
        modelInfo = this.entityClass.getAnnotation(ModelInfo.class);
        if (modelInfo == null) {
            throw new IllegalClassException("AutowareInit is null");
        }
        this.identityType = modelInfo.identityType();
        this.tableName = modelInfo.tableName();
        this.seqTableName = modelInfo.seqTableName();

        Document document = this.entityClass.getAnnotation(Document.class);
        if (document != null)
            this.tableName = document.collection();
        if (StringUtils.isEmpty(this.tableName))
            throw new IllegalArgumentException(this.entityClass.getSimpleName() + " params error!");
    }

    private void initEntityClass() throws Exception {
        entityClass = ReflectionUtil.getGenericType(this.getClass(), 0);
        if (entityClass == null) {
            throw new IllegalClassException("EntityClass is error");
        }
        propertyDescriptors = Introspector.getBeanInfo(entityClass).getPropertyDescriptors();
    }

    private void initFields() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    String name = fieldName;
                    String dbName = name;
                    Method writeMethod = one.getWriteMethod();
                    Method readMethod = one.getReadMethod();

                    org.springframework.data.mongodb.core.mapping.Field columnKey = field
                            .getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class);
                    if (columnKey != null) {
                        dbName = columnKey.value();
                    }

                    Id primaryKey = field.getAnnotation(Id.class);

                    if (primaryKey != null) {
                        dbName = "_id";
                    }

                    FieldInfo fieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                    fields.add(fieldInfo);
                    fieldsMap.put(fieldName, fieldInfo);
                    if (fieldName.equals("utime")) {
                        utimeFieldInfo = fieldInfo;
                    }
                    if (fieldName.equals("ctime")) {
                        ctimeFieldInfo = fieldInfo;
                    }

                    ListSizeKey listSizeKey = field.getAnnotation(ListSizeKey.class);
                    if (listSizeKey != null) {
                        this.listSizeInfo.put(listSizeKey.listName(), fieldInfo);
                    }
                }
            }
        }
    }

    private void initPrimaryFields() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    Id primaryKey = field.getAnnotation(Id.class);
                    if (primaryKey != null) {
                        String name = one.getName();
                        String dbName = "_id";
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();
                        FieldInfo tempFieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                        for (FieldInfo fieldInfo : fields) {
                            if (fieldInfo.equals(tempFieldInfo)) {
                                this.primaryFieldInfo = fieldInfo;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void initSortField() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    SortKey sortKey = field.getAnnotation(SortKey.class);
                    if (null != sortKey) {
                        String name = one.getName();
                        String dbName = name;
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();

                        FieldInfo tempFieldInfo = new FieldInfo(field, name, dbName, writeMethod, readMethod);
                        for (FieldInfo fieldInfo : fields) {
                            if (fieldInfo.equals(tempFieldInfo)) {
                                this.sortFieldInfo = fieldInfo;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    protected void initListInfo() throws Exception {
        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                if (field != null) {
                    ColumnKey columnKey = field.getAnnotation(ColumnKey.class);
                    if (columnKey != null && columnKey.isListData()) {
                        ListFieldInfo info = new ListFieldInfo(fieldName);
                        this.listFieldInfos.put(fieldName, info);
                        logger.info(this.entityClass + " find list data, list data info :{}", new Object[] { info });
                    }
                }
            }
        }
    }

    private DBObject toDBObject(T model) throws Exception {
        DBObject obj = new BasicDBObject();
        for (FieldInfo info : this.fields) {
            Method method = info.getReadMethod();
            obj.put(info.getDbName(), method.invoke(model));
        }
        return obj;
    }

    public T query(Query query) throws Exception {
        // saveLog(this.loggerTypeTag, "query", query);
        return this.mongoTemplate.findOne(query, entityClass);
    }

    public T load(ID id) throws Exception {
        saveLog(this.loggerTypeTag, "load", id);
        Query query = doQuery(id);
        return this.mongoTemplate.findOne(query, entityClass);
    }

    public List<T> loads(List<ID> objs) throws Exception {
        List<T> results = new ArrayList<T>();
        for (ID id : objs) {
            results.add(load(id));
        }
        return results;
    }

    public T insert(T model) throws Exception {
        saveLog(this.loggerTypeTag, "insert", model);

        if (identityType == IdentityType.identity) {
            long seqId = getNextSeqId();
            primaryFieldInfo.getWriteMethod().invoke(model, seqId);
        }

        if (this.ctimeFieldInfo != null) {
            ctimeFieldInfo.getWriteMethod().invoke(model, new Date().getTime());
        }

        if (this.utimeFieldInfo != null) {
            utimeFieldInfo.getWriteMethod().invoke(model, new Date().getTime());
        }

        this.mongoTemplate.insert(model);

        afterInsert(model);
        return model;
    }

    private long getNextSeqId() {
        DBObject returnObj = this.mongoTemplate.getCollection(seqTableName).findAndModify(
                new BasicDBObject("tableName", tableName), null, null, false,
                new BasicDBObject("$inc", new BasicDBObject("seq", 1)), true, true);
        return (Integer) returnObj.get("seq");
    }

    public boolean update(T model) throws Exception {
        // saveLog(this.loggerTypeTag, "update", model);
        ID id = getId(model);
        Query query = new Query(new Criteria(this.primaryFieldInfo.getDbName()).is(id));
        DBObject obj = toDBObject(model);
        obj.removeField(this.primaryFieldInfo.getDbName());
        if (listFieldInfos != null && listFieldInfos.size() > 0) {
            for (String fieldName : listFieldInfos.keySet()) {
                String dbName = this.fieldsMap.get(fieldName).getDbName();
                obj.removeField(dbName);
            }
        }

        // Update update = Update.fromDBObject(obj);
        Update update = new Update();
        for (String key : obj.keySet()) {
            Object value = obj.get(key);
            update.set(key, value);
        }

        this.mongoTemplate.updateFirst(query, update, entityClass);

        // 4、after update
        afterUpdate(model);
        return true;
    }

    public boolean delete(ID id) throws Exception {
        saveLog(this.loggerTypeTag, "delete", id);
        Query query = doQuery(id);
        T model = this.mongoTemplate.findOne(query, entityClass);
        if (model != null) {
            this.mongoTemplate.remove(query, entityClass);
            afterDelete(model);
            return true;
        }
        return false;
    }

    protected void afterInsert(T model) {

    };

    protected void afterUpdate(T model) {

    };

    protected void afterDelete(T model) {

    };

    /**
     * 现在只支持ID
     * 
     * 以后支持多个字段确定一条信息的联合组建
     * 
     * @param object
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Deprecated
    protected DBObject doPrimaryKeyQuery(Object object) {
        return new BasicDBObject(this.primaryFieldInfo.getDbName(), object);
    }

    protected Query doQuery(ID id) {
        // System.out.println("******" + this.primaryFieldInfo.getDbName() +
        // "&&&&&&&&" + this.primaryFieldInfo.getName());
        return new Query(new Criteria(this.primaryFieldInfo.getDbName()).is(id));
    }

    protected ID getId(T model) throws Exception {
        return (ID) this.primaryFieldInfo.getReadMethod().invoke(model);
    }

    public int getListCount(ID id, String listName) throws Exception {
        saveLog(this.loggerTypeTag, "getListCount", id, listName);
        Query query = doQuery(id);
        T object = this.mongoTemplate.findOne(query, this.entityClass);
        if (object != null)
            return (Integer) this.listSizeInfo.get(listName).getReadMethod().invoke(object);
        else
            return 0;
    }

    public boolean pushToList(ID id, String listName, long value) throws Exception {
        saveLog(this.loggerTypeTag, "pushToList", id, listName, value);
        Query query = doQuery(id);
        query.addCriteria(new Criteria(listName).ne(value));
        Update update = new Update().push(listName, value);
        if (this.listSizeInfo.containsKey(listName)) {
            update.inc(listSizeInfo.get(listName).getDbName(), 1);
        }

        // 修改时间
        if (this.utimeFieldInfo != null) {
            update.set(this.utimeFieldInfo.getDbName(), new Date().getTime());
        }
        long startTime = System.currentTimeMillis();
        T obj = this.mongoTemplate.findAndModify(query, update, entityClass);
        saveLog(this.loggerTypeTag, "pushToList", "time:{}", (System.currentTimeMillis() - startTime));
        if (obj != null)
            return true;
        else
            return false;
    }

    public boolean removeToList(ID id, String listName, long value) throws Exception {
        if (this.existList(id, listName, Arrays.asList(value))) {
            saveLog(this.loggerTypeTag, "removeToList", id, listName, value);
            Query query = doQuery(id);
            Update update = new Update().pull(listName, value);
            if (this.listSizeInfo.containsKey(listName)) {
                update.inc(this.listSizeInfo.get(listName).getDbName(), -1);
            }
            this.mongoTemplate.updateFirst(query, update, entityClass);
            return true;
        }
        return false;
    }

    public boolean removeAllToList(ID id, String listName, List<Long> values) throws Exception {
        saveLog(this.loggerTypeTag, "removeToList", id, listName, values);
        Query query = doQuery(id);
        Update update = new Update().pullAll(listName, values.toArray());
        if (this.listSizeInfo.containsKey(listName)) {
            update.inc(this.listSizeInfo.get(listName).getDbName(), 0 - values.size());
        }
        this.mongoTemplate.updateFirst(query, update, entityClass);

        query.addCriteria(new Criteria(listName).all(values));
        return !this.mongoTemplate.exists(query, entityClass);
    }

    public List<Long> getList(ID id, String listName) throws Exception {
        saveLog(this.loggerTypeTag, "getList", id, listName);
        Query query = doQuery(id);
        T object = this.mongoTemplate.findOne(query, this.entityClass);
        if (object != null) {
            return (List<Long>) this.fieldsMap.get(listName).getReadMethod().invoke(object);
        }
        return null;
    }

    public List<ID> queryForList(Query query) throws Exception {
        saveLog(this.loggerTypeTag, "queryForList", query);
        List<ID> result = new ArrayList<ID>();
        if (query != null && query.getQueryObject().toMap().size() > 0) {
            Method readMethod = this.primaryFieldInfo.getReadMethod();
            List<T> objs = this.mongoTemplate.find(query, this.entityClass);
            for (T o : objs) {
                ID id = (ID) readMethod.invoke(o);
                result.add(id);
            }
        }
        return result;
    }

    public List<T> queryForObjectList(Query query) throws Exception {
        saveLog(this.loggerTypeTag, "queryForList", query);
        if (query != null) {
            return this.mongoTemplate.find(query, this.entityClass);
        }
        return null;
    }

    public long queryForListCount(Query query) throws Exception {
        saveLog(this.loggerTypeTag, "queryForList", query);
        return this.mongoTemplate.count(query, entityClass);
    }

    @Deprecated
    public boolean existList(ID id, String listName, List<Long> objs) throws Exception {
        // saveLog(this.loggerTypeTag, "existList", "{}", id, listName, objs);
        Query query = doQuery(id);
        query.addCriteria(new Criteria(listName).all(objs));
        return this.mongoTemplate.exists(query, entityClass);
    }

    protected void saveLog(String tag, String methodName, Object... objs) {
        saveLog(tag, methodName, "{}", objs);
    }

    protected void saveLog(String tag, String methodName, String content, Object... objs) {
        try {
            // if (this.logger.isDebugEnabled()) {

            this.logger.info("【" + tag + "】" + "【" + this.daoClassName + "】" + "【" + methodName + "】" + "【" + content
                    + "】", objs);
            // }
        } catch (Exception e) {
        }
    }

    public Map<String, FieldInfo> getFieldsMap() {
        return fieldsMap;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public Map<String, FieldInfo> getListSizeInfo() {
        return listSizeInfo;
    }

}
