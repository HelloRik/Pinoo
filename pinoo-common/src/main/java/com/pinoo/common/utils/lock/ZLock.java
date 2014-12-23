package com.pinoo.common.utils.lock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

import com.pinoo.common.utils.properties.PropertyConfigurator;

/**
 * 基于ZOOKEEPER的分布式抢占(顺序)锁
 * 
 * @Filename: ZLock.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public class ZLock implements Watcher, Lock {

    private final static String DEFAULT_LOCK_ROOT_PATH = "/ZLockPath";

    private final static String lockRootPath;

    private static final int sessionTimeout = 60 * 60 * 1000;

    private final static int awaitTime = 100;

    /**
     * 获取锁超时时间
     */
    private int lockTimeOut = 5000;

    private static final String zkHosts;

    private String lockName;// 竞争资源的标志

    private CountDownLatch latch;

    private String seqNodeName;

    private String waitNode;

    private boolean locked = false;

    private static ZooKeeper zk;

    private final static Object initZkClientLock = new Object();

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

    /**
     * 获取锁对象
     */
    public ZLock() {
        this(5000);
    }

    /**
     * 自定义获取锁的超时时间，默认为5秒
     * 
     * @param lockTimeOut
     */
    public ZLock(int lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
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
                // System.out.println(event);
                connectedLatch.countDown();
            }
        }
    }

    public void process(WatchedEvent event) {

        if (event.getType() == EventType.NodeDeleted) {
            System.out.println(Thread.currentThread().getName() + "========waitNode :" + waitNode + " is delete");
            latch.countDown();
        }
    }

    public boolean lock(String lockName) {
        if (!lockName.startsWith("/"))
            lockName = "/" + lockName;

        this.lockName = lockName + "-";
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

            // 尝试获取锁
            if (tryLock()) {
                locked = true;
            } else {
                locked = waitForLock();
            }
        } catch (KeeperException e) {

            System.out.println(Thread.currentThread().getName() + " exception :" + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public boolean tryLock() {
        try {
            // 注册节点，得到节点名称
            seqNodeName = zk.create(lockRootPath + this.lockName, null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);

            System.out.println(Thread.currentThread().getName() + "=========seqNodeName :" + seqNodeName);

            // 获取所有子节点信息
            List<String> allNodeNames = zk.getChildren(lockRootPath, false);
            // 找出所有和lockName 相同的节点
            // 如果节点数为1 说明抢到锁
            if (allNodeNames.size() == 1) {
                return true;
            }

            // 如果节点数大于1，进行排序，找到锁的最小ID
            if (allNodeNames.size() > 0) {
                List<String> containNodeNames = new ArrayList<String>();

                for (String nodeName : allNodeNames) {
                    if (("/" + nodeName).startsWith(this.lockName)) {
                        containNodeNames.add(nodeName);
                    }
                }

                Collections.sort(containNodeNames);
                // 判断最小ID等于节点名称，等于获得锁
                if (containNodeNames.get(0).equals(seqNodeName)) {
                    System.out.println("==========is first ID");
                    return true;
                } else {
                    // 不等于的话，找到上一个ID

                    int index = Collections.binarySearch(containNodeNames,
                            seqNodeName.substring(seqNodeName.lastIndexOf("/") + 1));
                    if (index == 0) {
                        return true;
                    } else {
                        waitNode = containNodeNames.get(index - 1);
                        System.out.println(Thread.currentThread().getName() + "========waitNode :" + waitNode);
                    }
                }

            }

        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    private boolean waitForLock() throws InterruptedException, KeeperException {

        long waitTime = 0;

        // while (zk.exists(lockRootPath + "/" + waitNode, true) != null) {
        // this.latch = new CountDownLatch(1);
        // this.latch.await(awaitTime, TimeUnit.MILLISECONDS);
        // // 监听这个节点、等待唤醒
        // waitTime += awaitTime;
        // if (waitTime >= lockTimeOut)
        // return false;
        // }

        // long waitStartTime = System.currentTimeMillis();

        if (zk.exists(lockRootPath + "/" + waitNode, this) != null) {
            this.latch = new CountDownLatch(1);

            while (!this.latch.await(awaitTime, TimeUnit.MILLISECONDS)) {
                waitTime += awaitTime;
                if (waitTime >= lockTimeOut)
                    return false;
            }
        }

        // System.out.println(Thread.currentThread().getName() +
        // "========wait time "
        // + (System.currentTimeMillis() - waitStartTime));
        return true;
    }

    public void lockInterruptibly() throws InterruptedException {
        // TODO Auto-generated method stub

    }

    public Condition newCondition() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    public void unlock() {
        try {
            zk.delete(seqNodeName, -1);
            seqNodeName = null;
            latch = null;
            lockName = null;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
