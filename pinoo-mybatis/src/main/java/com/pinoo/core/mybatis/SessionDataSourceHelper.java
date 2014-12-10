package com.pinoo.core.mybatis;

import org.springframework.core.NamedThreadLocal;

import com.pinoo.core.mybatis.datasource.MasterSalveKey;

public class SessionDataSourceHelper {

    private final static NamedThreadLocal<MasterSalveKey> keys = new NamedThreadLocal<MasterSalveKey>(
            "Thread Master Slave Keys");

    public static void setOptionKey(MasterSalveKey key) {
        keys.set(key);
    }

    public static MasterSalveKey getOptionKey() {
        return keys.get();
    }

    public static void removeOptionKey() {
        keys.remove();
    }
}
