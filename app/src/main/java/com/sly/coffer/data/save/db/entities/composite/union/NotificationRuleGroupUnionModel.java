package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;

import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;

public class NotificationRuleGroupUnionModel {
    @Embedded
    private final NotificationRuleGroupEntity group;    //分组实体
    private final int count;                            //包含的规则数量

    public NotificationRuleGroupUnionModel(NotificationRuleGroupEntity group, int count) {
        this.group = group;
        this.count = count;
    }

    public NotificationRuleGroupEntity getGroup() {
        return group;
    }

    public int getCount() {
        return count;
    }
}
