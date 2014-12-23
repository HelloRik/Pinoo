package com.pinoo.common.utils.lock;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

import com.pinoo.common.utils.properties.PropertyConfigurator;

/**
 * 基于ZOOKEEPER的分布式互斥（独占）锁
 * 
 * @author jun.ju@downjoy.com
 * 
 */
public class ZMutexLock implements Lock {

    private final static String DEFAULT_LOCK_ROOT_PATH = "/ZMutexLockPath";

    private static final int sessionTimeout = 60 * 60 * 1000;

    private static ZooKeeper zk;

    private final static Object initZkClientLock = new Object();

    private static final String zkHosts;

    private final static String lockRootPath;

    private String lockName;// 竞争资源的标志

    private boolean locked = false;

    static {
        PropertyConfigurator props = PropertyConfigurator.getInstance("common.properties");
        if (props != null) {
            String zkAddress = props.getProperty("zkAddress");
            if (StringUtils.isNotEmpty(zkAddress))
                zkHosts = zkAddress;
            else
                zkHosts = "127.0.0.1:2181";

            String zlockRootPath = props.getProperty("zlockRootPath");
            if (StringUtils.isNotEmpty(zlockRootPath)) {
                if (!zlockRootPath.startsWith("/"))
                    zlockRootPath = "/" + zlockRootPath;
                lockRootPath = zlockRootPath;
            } else
                lockRootPath = DEFAULT_LOCK_ROOT_PATH;

        } else {
            zkHosts = "127.0.0.1:2181";
            lockRootPath = DEFAULT_LOCK_ROOT_PATH;
        }
    }

    public ZMutexLock() {
        try {
            if (zk == null) {
                synchronized (initZkClientLock) {
                    if (zk == null) {
                        CountDownLatch connectedLatch = new CountDownLatch(1);
                        zk = new ZooKeeper(zkHosts, sessionTimeout, new ConnectedWatch(connectedLatch));
                        waitForZK(zk, connectedLatch);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void waitForZK(ZooKeeper zk, CountDownLatch connectedLatch) {
        try {
            if (zk.getState() == States.CONNECTING) {
                connectedLatch.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class ConnectedWatch implements Watcher {

        private CountDownLatch connectedLatch;

        public ConnectedWatch(CountDownLatch connectedLatch) {
            this.connectedLatch = connectedLatch;
        }

        public void process(WatchedEvent event) {
            if (event.getState() == KeeperState.SyncConnected) {
                connectedLatch.countDown();
            }
        }
    }

    public boolean lock(String lockName) {
        if (!lockName.startsWith("/"))
            lockName = "/" + lockName;

        this.lockName = lockName;
        lock();
        return locked;
    }

    public void lock() {

        try {

            // 判断根节点是否存在，没有创建
            if (zk.exists(lockRootPath, false) == null) {
                zk.create(lockRootPath, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                System.out.println("create root path");
            }

            if (tryLock()) {
                locked = true;
            } else {
                locked = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean tryLock() {

        try {
            if (StringUtils.isEmpty(this.lockName))
                return false;

            if (zk.exists(lockRootPath + this.lockName, false) == null) {
                String nodeName = zk.create(lockRootPath + this.lockName, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
                // System.out.println("========" + nodeName);
                return true;
            }
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }

        return false;
    }

    public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
        return tryLock();
    }

    public void unlock() {
        try {
            if (locked) {
                zk.delete(lockRootPath + this.lockName, -1);
                locked = false;
            }
            this.lockName = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    public void lockInterruptibly() throws InterruptedException {

    }

    public Condition newCondition() {
        return null;
    }

}
