package com.pinoo.core.executor;

public interface ExecutorFactory {

    /**
     * 获取执行器
     * 
     * @return
     */
    public <T> T getExecutor(Class<T> clz);

}
