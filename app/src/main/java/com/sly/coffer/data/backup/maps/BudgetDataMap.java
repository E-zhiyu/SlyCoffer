package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class BudgetDataMap {
    private List<BudgetPojo> budgetList;                //预算
    private List<BudgetTagRefPojo> budgetTagRefList;    //标签映射

    public BudgetDataMap(List<BudgetPojo> budgetList, List<BudgetTagRefPojo> budgetTagRefList) {
        this.budgetList = budgetList;
        this.budgetTagRefList = budgetTagRefList;
    }

    public BudgetDataMap() {
    }

    public List<BudgetTagRefPojo> getBudgetTagRefList() {
        return budgetTagRefList;
    }

    public void setBudgetTagRefList(List<BudgetTagRefPojo> budgetTagRefList) {
        this.budgetTagRefList = budgetTagRefList;
    }

    public List<BudgetPojo> getBudgetList() {
        return budgetList;
    }

    public void setBudgetList(List<BudgetPojo> budgetList) {
        this.budgetList = budgetList;
    }
}
