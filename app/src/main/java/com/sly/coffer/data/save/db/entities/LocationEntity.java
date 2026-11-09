package com.sly.coffer.data.save.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import java.util.Objects;

@Entity(
        tableName = "locations",
        primaryKeys = {"accountId"},
        foreignKeys = {
                @ForeignKey(
                        entity = AccountEntity.class,
                        parentColumns = "accountId",
                        childColumns = "accountId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class LocationEntity {
    private long accountId;         //段落编号（外键）
    private double latitude;        //纬度
    private double longitude;       //经度
    private double altitude;        //海拔
    @ColumnInfo(defaultValue = "1")
    private boolean isFineLocation; //是否为精确位置
    @ColumnInfo(defaultValue = "")
    private String provider;        //位置提供者

    public LocationEntity(long accountId, double latitude, double longitude, double altitude, boolean isFineLocation, String provider) {
        this.accountId = accountId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.isFineLocation = isFineLocation;
        this.provider = provider;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocationEntity location = (LocationEntity) o;
        return Double.compare(latitude, location.latitude) == 0 && Double.compare(longitude, location.longitude) == 0 && Double.compare(altitude, location.altitude) == 0 && isFineLocation == location.isFineLocation && Objects.equals(provider, location.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, altitude, isFineLocation, provider);
    }
}
