package com.pinoo.demo;

import java.util.List;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.pinoo.demo.dao.MessageDao;
import com.pinoo.demo.model.Message;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StartServer {

    private static Logger logger = LoggerFactory.getLogger(StartServer.class);

    private static ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("server.xml");

    private RedisTemplate templet = context.getBean("redisTemplate", RedisTemplate.class);

    private StringRedisTemplate stringRedisTemplet = context.getBean(StringRedisTemplate.class);

    private MessageDao dao = context.getBean("messageDao", MessageDao.class);

    @Test
    public void testGetList() {
        System.out.println(dao.testGetList(2, 1));
    }

    // @Test
    public void delectCache() {

        System.out.println(stringRedisTemplet.boundValueOps("com.pinoo.demo.model.Message_count__type_2").get());
        System.out.println(stringRedisTemplet.boundValueOps("com.pinoo.demo.model.Message_count__type_1").get());
        // stringRedisTemplet.delete("com.pinoo.demo.model.Message_count__type_1");
        // templet.delete("com.pinoo.demo.model.Message_object_3");
        // templet.delete("com.pinoo.demo.model.Message_object_4");
        // templet.delete("com.pinoo.demo.model.Message_object_5");
        // templet.delete("com.pinoo.demo.model.Message_object_6");
    }

    // @Test
    public void insert() {
        Message msg = new Message();
        msg.setTitle("test");
        msg.setContent("text !!!!!");
        msg.setAddTime(System.currentTimeMillis());
        msg.setType(2);
        msg.setStatus(2);

        dao.insert(msg);
    }

    // @Test
    public void load() {
        Message msg = dao.load(30);
        System.out.println(msg);
    }

    // @Test
    public void loadCount() {
        System.out.println(dao.getMsgCountByType(1));
    }

    // @Test
    public void loadAllList() {
        List<Message> msgs = dao.getAllMsgList(2, 4);
        System.out.println(msgs);
    }

    // @Test
    public void loadList() {
        List<Message> msgs = dao.getMsgList(1, 2, -1, 10);
        System.out.println(msgs);
    }

    // @Test
    public void update() {
        Message msg = dao.load(35);
        msg.setType(2);
        msg.setStatus(1);
        dao.update(msg);
    }

    @After
    public void after() {
        System.out.println("==================================================================");
    }

}
