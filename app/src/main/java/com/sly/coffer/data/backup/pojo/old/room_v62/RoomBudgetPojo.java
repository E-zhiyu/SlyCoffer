package com.sly.coffer.data.backup.pojo.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomBudgetPojo {
    private long bno;
    private double initAmount;
    private double leftAmount;
    private String name;
    private String resetFrequency;
    private String startDate;
    private List<Long> tagNoList;

    public RoomBudgetPojo() {
    }

    public long getBno() {
        return bno;
    }

    public void setBno(long bno) {
        this.bno = bno;
    }

    public double getInitAmount() {
        return initAmount;
    }

    public void setInitAmount(double initAmount) {
        this.initAmount = initAmount;
    }

    public double getLeftAmount() {
        return leftAmount;
    }

    public void setLeftAmount(double leftAmount) {
        this.leftAmount = leftAmount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResetFrequency() {
        return resetFrequency;
    }

    public void setResetFrequency(String resetFrequency) {
        this.resetFrequency = resetFrequency;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public List<Long> getTagNoList() {
        return tagNoList;
    }

    public void setTagNoList(List<Long> tagNoList) {
        this.tagNoList = tagNoList;
    }
}
