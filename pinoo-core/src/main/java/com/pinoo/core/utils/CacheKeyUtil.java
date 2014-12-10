package com.pinoo.core.utils;

public class CacheKeyUtil {

    public static final String objectCacheKeySign = "_object_%s";

    public static <ID> String formatObject(Class<?> clz, ID id) {
        return format(clz.getName() + objectCacheKeySign, id);
    }

    private static String format(String format, Object... objs) {
        return format != null ? String.format(format, objs) : null;
    }

}
