package com.pinoo.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FrameworkContext {

    public final static Executor THREAD_POOL = Executors.newCachedThreadPool();

    /**
     * 默认过期时间2小时
     */
    public final static long DEFAULT_EXPIRED_TIME = 60 * 60 * 2;

    /**
     * 默认过期时间单位：秒
     */
    public final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
}
