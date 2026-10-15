package com.sly.coffer.data.backup.maps.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomBudgetPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomBudgetDataMap {
    private List<RoomBudgetPojo> budget_data;

    public RoomBudgetDataMap() {
    }

    public List<RoomBudgetPojo> getBudget_data() {
        return budget_data;
    }

    public void setBudget_data(List<RoomBudgetPojo> budget_data) {
        this.budget_data = budget_data;
    }
}
