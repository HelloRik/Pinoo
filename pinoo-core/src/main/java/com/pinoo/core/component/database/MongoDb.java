//package com.pinoo.core.component.database;
//
//import static org.springframework.data.mongodb.core.query.Criteria.where;
//import static org.springframework.data.mongodb.core.query.Query.query;
//
//import java.util.Date;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Repository;
//
//import com.pinoo.common.annotation.model.IdentityType;
//import com.pinoo.core.component.Initialzing;
//import com.pinoo.core.dao.IEntityInfo;
//import com.pinoo.core.utils.TransformUtil;
//import com.mongodb.BasicDBObject;
//import com.mongodb.DBObject;
//
//@Repository
//public class MongoDb implements Initialzing, IDbData {
//
//    private Logger logger = LoggerFactory.getLogger(MongoDb.class);
//
//    protected String daoClassName = this.getClass().getSimpleName();
//
//    @Autowired
//    @Qualifier("mongoTemplate")
//    protected MongoTemplate mongoTemplate;
//
//    protected IEntityInfo entityInfo;
//
//    @Override
//    public void initialize(IEntityInfo entityInfo) throws Exception {
//        this.entityInfo = entityInfo;
//    }
//
//    protected <ID> Query doQuery(ID id) {
//        return query(where(this.entityInfo.getPrimaryKey().getDbName()).is(id));
//    }
//
//    @Override
//    public <T, ID> T load(ID id, Class<T> entityClass) throws Exception {
//        return this.mongoTemplate.findOne(doQuery(id), entityClass);
//    }
//
//    @Override
//    public <T> T insert(T model, Class<T> clz) throws Exception {
//        if (this.entityInfo.getIdentityType() == IdentityType.identity) {
//            long seqId = getNextSeqId();
//            this.entityInfo.getPrimaryKey().getWriteMethod().invoke(model, seqId);
//        }
//
//        if (this.entityInfo.getCtime() != null) {
//            this.entityInfo.getCtime().getWriteMethod().invoke(model, new Date().getTime());
//        }
//
//        if (this.entityInfo.getUtime() != null) {
//            this.entityInfo.getUtime().getWriteMethod().invoke(model, new Date().getTime());
//        }
//
//        this.mongoTemplate.insert(model);
//        return model;
//    }
//
//    private long getNextSeqId() {
//        DBObject returnObj = this.mongoTemplate.getCollection(this.entityInfo.getSeqTableName()).findAndModify(
//                new BasicDBObject("tableName", this.entityInfo.getTableName()), null, null, false,
//                new BasicDBObject("$inc", new BasicDBObject("seq", 1)), true, true);
//        return (Integer) returnObj.get("seq");
//    }
//
//    private Long getId(Object model) throws Exception {
//        return (Long) this.entityInfo.getPrimaryKey().getReadMethod().invoke(model);
//    }
//
//    @Override
//    public <T> boolean update(T model, Class<T> clz) throws Exception {
//        long id = getId(model);
//        Query query = doQuery(id);
//        DBObject obj = TransformUtil.toMap(model, BasicDBObject.class, this.entityInfo.getFields());
//        obj.removeField(this.entityInfo.getPrimaryKey().getDbName());
//        Update update = Update.fromDBObject(obj);
//        this.mongoTemplate.updateFirst(query, update, clz);
//        return true;
//    }
//
//    @Override
//    public <ID, T> T delete(ID id, Class<T> entityClass) throws Exception {
//        Query query = doQuery(id);
//        T model = (T) this.mongoTemplate.findOne(query, entityClass);
//        if (model != null) {
//            this.mongoTemplate.remove(query, entityClass);
//            return model;
//        }
//        return null;
//    }
//
// }
