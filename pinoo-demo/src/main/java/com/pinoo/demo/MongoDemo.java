package com.pinoo.demo;

import java.util.List;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.Metrics;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.pinoo.demo.dao.LocationDao;
import com.pinoo.demo.dao.SessionDao;
import com.pinoo.demo.model.Location;
import com.pinoo.demo.model.Session;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MongoDemo {

    private static Logger logger = LoggerFactory.getLogger(MongoDemo.class);

    private static ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("server.xml");

    private RedisTemplate templet = context.getBean("redisTemplate", RedisTemplate.class);

    private StringRedisTemplate stringRedisTemplet = context.getBean(StringRedisTemplate.class);

    private MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);

    private SessionDao sessionDao = context.getBean(SessionDao.class);

    private LocationDao locationDao = context.getBean(LocationDao.class);

    // @Test
    public void addToList() throws Exception {
        this.sessionDao.pushToList(25l, "mids", 13l);
    }

    // @Test
    public void getCursorList() throws Exception {
        List<Session> list = sessionDao.getCursorList(-1, 6);

        for (Session session : list) {
            System.out.println(session);
        }
    }

    // @Test
    public void getPageList() throws Exception {
        List<Session> list = sessionDao.getPageList(1, 5);

        for (Session session : list) {
            System.out.println(session);
        }
    }

    // @Test
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

    @Test
    public void initLocationData() throws Exception {

        System.out.println("@@@@@@@@@@@");
        for (int i = 0; i < 10000; i++) {

            double x = Math.random() * 180;
            double y = Math.random() * 180;

            System.out.println(x + "," + y);

            Location location = new Location();
            location.setUserId(i);
            location.getGps().add(x);
            location.getGps().add(y);

            this.locationDao.insert(location);

        }

        System.out.println("@@@@@@@@@@@");

    }

    // @Test
    public void delete() throws Exception {
        this.locationDao.delete(153l);
    }

    // @Test
    public void geo() throws Exception {

        Query query = Query.query(new Criteria("userId").is(2005));

        List<Location> locations = locationDao.near(30, 30, 200, null, Metrics.KILOMETERS);

        // locations = locationDao.circle(30, 30, 1);
        locations = this.locationDao.box(20, 20, 30, 30);

        for (Location l : locations)
            System.out.println("@@@@" + l);

    }

    @After
    public void after() {
        System.out.println("==================================================================");
    }
}
