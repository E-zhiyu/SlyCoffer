package com.sly.coffer.data.backup.pojo.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraBudgetPojo {
    private long budgetId;
    private String name;
    private double initAmount;
    private double balance;
    private long startDate;
    private int resetFrequency;
    private int lowBalanceRatio = 10;

    public ExtraBudgetPojo() {
    }

    public long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(long budgetId) {
        this.budgetId = budgetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getInitAmount() {
        return initAmount;
    }

    public void setInitAmount(double initAmount) {
        this.initAmount = initAmount;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public int getResetFrequency() {
        return resetFrequency;
    }

    public void setResetFrequency(int resetFrequency) {
        this.resetFrequency = resetFrequency;
    }

    public int getLowBalanceRatio() {
        return lowBalanceRatio;
    }

    public void setLowBalanceRatio(int lowBalanceRatio) {
        this.lowBalanceRatio = lowBalanceRatio;
    }
}
