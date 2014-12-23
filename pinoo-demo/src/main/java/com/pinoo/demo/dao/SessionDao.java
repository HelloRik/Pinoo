package com.pinoo.demo.dao;

import org.springframework.stereotype.Repository;

import com.pinoo.demo.model.Session;
import com.pinoo.storage.mongodb.dao.RedisCacheDao;

@Repository("sessionDao")
public class SessionDao extends RedisCacheDao<Session, Long> {

}
