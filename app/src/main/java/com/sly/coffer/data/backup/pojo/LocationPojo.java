package com.sly.coffer.data.backup.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class LocationPojo {
    private long accountId;         //流水记录编号（外键）
    private double latitude;        //纬度
    private double longitude;       //经度
    private double altitude;        //海拔
    private boolean isFineLocation = true; //是否为精确位置
    private String provider = "";   //位置提供者

    public LocationPojo() {
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public boolean isFineLocation() {
        return isFineLocation;
    }

    public void setFineLocation(boolean fineLocation) {
        isFineLocation = fineLocation;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
