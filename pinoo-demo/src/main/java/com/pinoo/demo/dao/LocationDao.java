package com.pinoo.demo.dao;

import org.springframework.stereotype.Repository;

import com.pinoo.demo.model.Location;
import com.pinoo.storage.mongodb.annotation.dao.ProxyDao;
import com.pinoo.storage.mongodb.dao.GeoDao;

@ProxyDao
@Repository("locationDao")
public class LocationDao extends GeoDao<Location, Long> {

}
