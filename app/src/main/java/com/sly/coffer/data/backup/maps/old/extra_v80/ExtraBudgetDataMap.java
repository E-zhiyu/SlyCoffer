package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraBudgetDataMap {
    private List<BudgetPojo> budgetList;
    private List<BudgetTagRefPojo> budgetTagRefList;

    public ExtraBudgetDataMap() {
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
