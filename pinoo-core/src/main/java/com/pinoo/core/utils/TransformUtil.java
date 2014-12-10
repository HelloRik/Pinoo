package com.pinoo.core.utils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.pinoo.common.annotation.model.FieldInfo;

public class TransformUtil {

    public static <E extends Map> E toMap(Object model, Class<E> clz, List<FieldInfo> fields) throws Exception {
        E obj = clz.newInstance();
        for (FieldInfo info : fields) {
            Method method = info.getReadMethod();
            obj.put(info.getDbName(), method.invoke(model));
        }
        return obj;
    }

    public static Object getValue(Method readMethod, Object model) throws Exception {
        return readMethod.invoke(model);
    }
}
