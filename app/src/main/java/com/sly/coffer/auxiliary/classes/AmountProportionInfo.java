package com.sly.coffer.auxiliary.classes;

import java.util.List;

public class AmountProportionInfo {
    private final int percentage;                               //此来源占支出/收入的比例
    private final double amount;                                //该来源的总金额
    private final String name;                                  //该来源的名称
    private final long id;                                      //该来源的编号
    private final List<Long> accountIdList;                     //属于该来源的流水记录的编号

    public AmountProportionInfo(int percentage, double amount, String name, long id, List<Long> accountIdList) {
        this.percentage = percentage;
        this.amount = amount;
        this.name = name;
        this.id = id;
        this.accountIdList = accountIdList;
    }

    public int getPercentage() {
        return percentage;
    }

    public double getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public List<Long> getAccountIdList() {
        return accountIdList;
    }
}
