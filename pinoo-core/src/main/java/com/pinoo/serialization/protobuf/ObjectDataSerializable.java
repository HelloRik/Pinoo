package com.pinoo.serialization.protobuf;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;
import com.google.protobuf.Message;
import com.pinoo.beans.FieldInfo;
import com.pinoo.common.utils.ReflectionUtil;

/**
 * protobuf通用序列化生成器
 * 
 * @Filename: ObjectDataSerializable.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public abstract class ObjectDataSerializable<T, B> implements RedisSerializer<T>, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(ObjectDataSerializable.class);

    protected PropertyDescriptor[] entityPropertyDescriptors;

    protected PropertyDescriptor[] protobufPropertyDescriptors;

    protected Map<String, FieldInfo> entityFieldInfos = new HashMap<String, FieldInfo>();

    protected Map<String, FieldInfo> bufFieldInfos = new HashMap<String, FieldInfo>();

    /**
     * 实例对象中HASHMAP的存储关系
     */
    protected Map<String, FieldInfo> bufPairFieldInfos = new HashMap<String, FieldInfo>();

    protected Class<?> entityClass;

    protected Class<?> builderClass;

    public void afterPropertiesSet() throws Exception {
        this.entityClass = ReflectionUtil.getGenericType(this.getClass(), 0);
        this.builderClass = ReflectionUtil.getGenericType(this.getClass(), 1);

        entityPropertyDescriptors = Introspector.getBeanInfo(entityClass).getPropertyDescriptors();
        protobufPropertyDescriptors = Introspector.getBeanInfo(builderClass).getPropertyDescriptors();

        for (PropertyDescriptor p : entityPropertyDescriptors) {
            String fieldName = p.getName();
            if (!fieldName.equals("class")) {
                Field field = ReflectionUtil.getFieldByName(fieldName, entityClass);
                for (PropertyDescriptor buf : protobufPropertyDescriptors) {
                    if (fieldName.equals(buf.getName())) {

                        Method entityReadMethod = p.getReadMethod();
                        Method entityWriteMethod = p.getWriteMethod();

                        FieldInfo entityFieldInfo = new FieldInfo(field, fieldName, fieldName, entityWriteMethod,
                                entityReadMethod);
                        entityFieldInfos.put(fieldName, entityFieldInfo);

                        Method bufReadMethod = buf.getReadMethod();
                        Method bufWriteMethod = buf.getWriteMethod();

                        String writeMethodName;
                        String readMethodName;
                        if (field.getType() == List.class || field.getType() == Map.class) {
                            writeMethodName = "addAll" + fieldName.substring(0, 1).toUpperCase()
                                    + fieldName.substring(1);
                            readMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
                                    + "List";

                            for (Method m : builderClass.getMethods()) {
                                if (m.getName().equals(writeMethodName)) {
                                    bufWriteMethod = m;
                                    continue;
                                }
                                if (m.getName().equals(readMethodName)) {
                                    bufReadMethod = m;
                                    continue;
                                }
                            }
                        } else {
                            bufReadMethod = buf.getReadMethod();
                            writeMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                            for (Method m : builderClass.getMethods()) {
                                if (m.getName().equals(writeMethodName)) {
                                    bufWriteMethod = m;
                                    break;
                                }
                            }
                        }

                        FieldInfo bufFieldInfo = new FieldInfo(field, fieldName, fieldName, bufWriteMethod,
                                bufReadMethod);
                        bufFieldInfos.put(fieldName, bufFieldInfo);

                        if (field.getType() == Map.class) {
                            if (getNameValuePairMessage() == null) {
                                logger.error("未定义键值对protobuf builder，实例类型{}:字段{}", new Object[] { this.entityClass,
                                        fieldName });
                                throw new IllegalArgumentException();
                            }

                            GeneratedMessage.Builder pairBuilder = (Builder) getNameValuePairMessage()
                                    .newBuilderForType();
                            PropertyDescriptor[] pairPropertyDescriptor = Introspector.getBeanInfo(
                                    pairBuilder.getClass()).getPropertyDescriptors();

                            for (PropertyDescriptor pp : pairPropertyDescriptor) {
                                String pairName = pp.getName();
                                if (pairName.equals("key") || pairName.equals("value")) {
                                    Field pairField = ReflectionUtil.getFieldByName(fieldName, entityClass);
                                    Method bufPairReadMethod = pp.getReadMethod();
                                    Method bufPairWriteMethod = null;

                                    String pairWriteMethodName = "set" + pairName.substring(0, 1).toUpperCase()
                                            + pairName.substring(1);
                                    for (Method m : pairBuilder.getClass().getMethods()) {
                                        if (m.getName().equals(pairWriteMethodName)) {
                                            bufPairWriteMethod = m;
                                            break;
                                        }
                                    }

                                    // System.out.println("******" +
                                    // bufPairReadMethod);
                                    // System.out.println("******" +
                                    // bufPairWriteMethod);

                                    FieldInfo pairFieldInfo = new FieldInfo(pairField, pairName, pairName,
                                            bufPairWriteMethod, bufPairReadMethod);
                                    bufPairFieldInfos.put(fieldName + "_" + pairName, pairFieldInfo);
                                    // System.out.println("(((((((((" +
                                    // fieldName + "_" + pairName);
                                }
                            }
                        }
                    }
                }
            }
        }

        // for (String name : this.entityFieldInfos.keySet()) {
        // System.out.println(this.entityClass + "$$$$$$$$" + name);
        // }
    }

    public byte[] serialize(T t) throws SerializationException {
        try {
            GeneratedMessage.Builder builder = (Builder) getGeneratedMessage().newBuilderForType();

            for (FieldInfo info : entityFieldInfos.values()) {

                Method readMethod = info.getReadMethod();
                Object value = readMethod.invoke(t);

                // System.out.println(this.entityClass + "!!!!!!" +
                // info.getName() + "====" + value);

                // 判断下是否MAP,需要转换成
                if (info.getField().getType() != Map.class) {
                    Method writeMethod = bufFieldInfos.get(info.getName()).getWriteMethod();
                    writeMethod.invoke(builder, value);
                } else {
                    // System.out.println("=========" + info.getName());
                    Method writeMethod = bufFieldInfos.get(info.getName()).getWriteMethod();
                    Map<String, String> map = (Map) value;

                    List list = new ArrayList();
                    for (Iterator<String> it = map.keySet().iterator(); it.hasNext();) {
                        GeneratedMessage.Builder pairBuilder = (Builder) getNameValuePairMessage().newBuilderForType();
                        String key = it.next();
                        String v = map.get(key);

                        Method keyWriteMethod = this.bufPairFieldInfos.get(info.getName() + "_key").getWriteMethod();
                        Method valueWriteMethod = this.bufPairFieldInfos.get(info.getName() + "_value")
                                .getWriteMethod();

                        keyWriteMethod.invoke(pairBuilder, key);
                        valueWriteMethod.invoke(pairBuilder, v);
                        list.add(pairBuilder.build());
                    }
                    writeMethod.invoke(builder, list);
                }
            }
            // logger.info(this.getClass().getSimpleName() + "  serialize, obj:"
            // + t);
            return builder.build().toByteArray();
        } catch (Exception e) {
            logger.info(this.getClass().getSimpleName() + " serialize obj error, obj:" + t);
            e.printStackTrace();
            throw new SerializationException(e.getMessage(), e);
        }
    }

    public T deserialize(byte[] bytes) throws SerializationException {

        if (bytes == null || bytes.length == 0)
            return null;

        try {
            // Message message =
            // getGeneratedMessage().getParserForType().parseFrom(bytes);
            GeneratedMessage.Builder builder = (Builder) getGeneratedMessage().newBuilderForType();
            builder.mergeFrom(bytes);
            T object = (T) this.entityClass.newInstance();

            for (FieldInfo info : entityFieldInfos.values()) {

                Method readMethod = bufFieldInfos.get(info.getName()).getReadMethod();
                Object value = readMethod.invoke(builder);

                // System.out.println(this.entityClass + "@@@@@@@@@" +
                // info.getName() + "====" + value);

                Method writeMethod = info.getWriteMethod();
                if (info.getField().getType() != Map.class) {
                    writeMethod.invoke(object, value);
                } else {
                    Map<String, String> namePairMap = new HashMap<String, String>();
                    List list = (List) value;

                    String key = info.getName() + "_key";
                    String v = info.getName() + "_value";

                    // System.out.println(key + "******" + v);

                    Method keyReadMethod = this.bufPairFieldInfos.get(key).getReadMethod();
                    Method valueReadMethod = this.bufPairFieldInfos.get(v).getReadMethod();

                    // System.out.println("@@@@@@" + keyReadMethod);
                    // System.out.println("@@@@@@" + valueReadMethod);

                    for (Object pair : list) {
                        // System.out.println(pair);
                        com.google.protobuf.Message.Builder pairBuilder = this.getNameValuePairMessage()
                                .newBuilderForType().mergeFrom((Message) pair);

                        namePairMap.put((String) keyReadMethod.invoke(pairBuilder),
                                (String) valueReadMethod.invoke(pairBuilder));
                    }
                    writeMethod.invoke(object, namePairMap);
                }
            }
            // logger.info(this.getClass().getSimpleName() +
            // "  deserialize, obj:" + object);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SerializationException(e.getMessage() + ",class :" + this.getClass(), e);
        }
    }

    protected GeneratedMessage getNameValuePairMessage() {
        return null;
    }

    protected abstract GeneratedMessage getGeneratedMessage();

}
