package com.pinoo.common.utils.properties;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class Property2Obj {

    public static Object property2Obj(Object obj, PropertyConfigurator configurator) throws Exception {

        PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();

        for (PropertyDescriptor pd : pds) {
            if (!"class".equals(pd.getName())) {
                Class clazz = pd.getPropertyType();
                if (clazz.equals(int.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getIntProperty(pd.getName()));
                } else if (clazz.equals(boolean.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getBooleanProperty(pd.getName()));
                } else if (clazz.equals(String.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getProperty(pd.getName()));
                } else {
                    throw new RuntimeException("not support type:" + clazz);
                }

            }
        }
        return obj;
    }

}
