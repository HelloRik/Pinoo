package com.pinoo.demo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pinoo.storage.mongodb.annotation.model.GeoLocation;
import com.pinoo.storage.mongodb.annotation.model.ModelInfo;

@ModelInfo()
@Document(collection = "location")
public class Location implements Serializable {

    @Id
    private long id;

    private long userId;

    @GeoLocation
    private List<Double> gps = new ArrayList<Double>(2);

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * @return Returns the gps
     */
    public List<Double> getGps() {
        return gps;
    }

    /**
     * @param gps
     *            The gps to set.
     */
    public void setGps(List<Double> gps) {
        this.gps = gps;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Location [id=" + id + ", userId=" + userId + ", gps=" + gps + "]";
    }

}
