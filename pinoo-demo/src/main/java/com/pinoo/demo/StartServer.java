package com.pinoo.demo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import com.pinoo.demo.dao.MessageDao;
import com.pinoo.demo.model.Message;

public class StartServer {

    static ClassPathXmlApplicationContext context;

    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(StartServer.class);
        context = new ClassPathXmlApplicationContext("server.xml");
        context.start();

        logger.info("=================================");
        logger.info("==== start server success!!! ====");
        logger.info("=================================");

        RedisTemplate templet = context.getBean("redisTemplate", RedisTemplate.class);

        // templet.delete("com.pinoo.demo.model.Message_list__status_2_type_1");
        // templet.delete("com.pinoo.demo.model.Message_object_3");
        // templet.delete("com.pinoo.demo.model.Message_object_4");
        // templet.delete("com.pinoo.demo.model.Message_object_5");
        // templet.delete("com.pinoo.demo.model.Message_object_6");

        MessageDao dao = context.getBean(MessageDao.class);
        //
        Message msg = new Message();
        msg.setTitle("test");
        msg.setContent("1234444");
        msg.setAddTime(System.currentTimeMillis());
        msg.setType(2);
        msg.setStatus(2);

        dao.insert(msg);
        //
        // msg = dao.load(30);
        // System.out.println(msg);
        // msg.setType(2);
        // dao.update(msg);
        //
        List<Message> msgs = dao.getMsgList(2, 1, -1, 10);
        System.out.println(msgs);
        //
        msgs = dao.getMsgList(2, 2, -1, 10);
        System.out.println(msgs);

    }
}
