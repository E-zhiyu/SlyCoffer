package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(
        tableName = "accounts",
        indices = {
                @Index(value = "accountId"),
                @Index(value = "remark"),
                @Index(value = "type"),
                @Index(value = "dateTime")
        }
)
public class AccountEntity {
    @PrimaryKey(autoGenerate = true)
    private long accountId;         //主键
    private double amount;          //金额
    private String remark;          //备注
    private int type;               //种类
    private LocalDateTime dateTime; //日期和时间
    private int autoTag;            //自动记账标识

    public AccountEntity(double amount, String remark, int type, LocalDateTime dateTime, int autoTag) {
        this.amount = amount;
        this.remark = remark;
        this.type = type;
        this.dateTime = dateTime;
        this.autoTag = autoTag;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setAutoTag(int autoTag) {
        this.autoTag = autoTag;
    }

    public int getAutoTag() {
        return autoTag;
    }
}
