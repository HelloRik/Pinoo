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

import com.pinoo.demo.dao.SessionDao;
import com.pinoo.demo.model.Session;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MongoDemo {

    private static Logger logger = LoggerFactory.getLogger(MongoDemo.class);

    private static ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("server.xml");

    private RedisTemplate templet = context.getBean("redisTemplate", RedisTemplate.class);

    private StringRedisTemplate stringRedisTemplet = context.getBean(StringRedisTemplate.class);

    private SessionDao sessionDao = context.getBean(SessionDao.class);

    // @Test
    public void addToList() throws Exception {
        this.sessionDao.pushToList(25l, "mids", 13l);
    }

    @Test
    public void getCursorList() throws Exception {
        List<Session> list = sessionDao.getCursorList(-1, 6);

        for (Session session : list) {
            System.out.println(session);
        }
    }

    @Test
    public void getPageList() throws Exception {
        List<Session> list = sessionDao.getPageList(1, 5);

        for (Session session : list) {
            System.out.println(session);
        }
    }

    @Test
    public void getAllList() throws Exception {
        List<Session> list = sessionDao.getAllList();

        for (Session session : list) {
            System.out.println(session);
        }
    }

    // @Test
    public void select() throws Exception {
        System.out.println(sessionDao.load(25l));
    }

    @After
    public void after() {
        System.out.println("==================================================================");
    }
}
