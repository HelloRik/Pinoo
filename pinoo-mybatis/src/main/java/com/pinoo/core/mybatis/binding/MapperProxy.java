package com.pinoo.core.mybatis.binding;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;

import com.pinoo.common.utils.AnnotationScaner;
import com.pinoo.core.mybatis.SessionDataSourceHelper;
import com.pinoo.core.mybatis.annotation.ReadMaster;
import com.pinoo.core.mybatis.datasource.MasterSalveKey;

public class MapperProxy<T> implements InvocationHandler, Serializable {

    protected Log logger = LogFactory.getLog(getClass());

    private static final long serialVersionUID = -2110376289859960089L;

    private final SqlSession sqlSession;

    private final Class<T> mapperInterface;

    private final Map<Method, MapperMethod> methodCache;

    /**
     * 标记从主库读取的方法
     */
    private final Map<Method, Annotation> readMasterMethodMap;

    // static {
    // methodMap = AnnotationScaner.scanMethod("com.haier.mobilemall",
    // ReadMaster.class);
    // }

    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
        readMasterMethodMap = AnnotationScaner.scanMethod(mapperInterface, ReadMaster.class);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }

            MasterSalveKey transactionKey = SessionDataSourceHelper.getOptionKey();

            if (this.logger.isDebugEnabled()) {

                if (!MasterSalveKey.TRANSACTION.equals(transactionKey))
                    this.logger.debug("============================================================================");

                this.logger.debug("Thread ID : 【" + Thread.currentThread().getId() + "】, mapper proxy Method Name : "
                        + method);
                this.logger.debug("Thread ID : 【" + Thread.currentThread().getId()
                        + "】,mapper proxy current lookup key is " + transactionKey + "!");
            }

            if (readMasterMethodMap.containsKey(method) && !MasterSalveKey.TRANSACTION.equals(transactionKey)) {
                SessionDataSourceHelper.setOptionKey(MasterSalveKey.FORCE);
            }
            // cache

            final MapperMethod mapperMethod = cachedMapperMethod(method);
            Object result = mapperMethod.execute(sqlSession, args);

            return result;
        } catch (Exception e) {
            this.logger.info("Thread ID : 【" + Thread.currentThread().getId() + "】,method invoke error,Method Name : "
                    + method);
            throw e;
        } finally {
            MasterSalveKey key = SessionDataSourceHelper.getOptionKey();

            // 如果不是事务，取消标记
            if (!MasterSalveKey.TRANSACTION.equals(key)) {
                SessionDataSourceHelper.removeOptionKey();
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Thread ID : 【" + Thread.currentThread().getId()
                            + "】,mapper proxy remove lookup key " + key + "!");
                }
            }
        }
    }

    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
            mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
            methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
    }

}
