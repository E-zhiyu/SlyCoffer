package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraBudgetPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraBudgetTagRefPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraBudgetDataMap {
    private List<ExtraBudgetPojo> budgetList;
    private List<ExtraBudgetTagRefPojo> budgetTagRefList;

    public ExtraBudgetDataMap() {
    }

    public List<ExtraBudgetPojo> getBudgetList() {
        return budgetList;
    }

    public void setBudgetList(List<ExtraBudgetPojo> budgetList) {
        this.budgetList = budgetList;
    }

    public List<ExtraBudgetTagRefPojo> getBudgetTagRefList() {
        return budgetTagRefList;
    }

    public void setBudgetTagRefList(List<ExtraBudgetTagRefPojo> budgetTagRefList) {
        this.budgetTagRefList = budgetTagRefList;
    }
}
