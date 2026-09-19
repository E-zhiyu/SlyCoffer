package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;

import java.util.List;

public class NotificationRuleAndGroupUnionModel {
    @Embedded
    private final NotificationRuleGroupEntity group;        //分组实体
    @Relation(
            parentColumn = "groupId",
            entityColumn = "ruleId",
            associateBy = @Junction(NotificationRuleGroupRefEntity.class)
    )
    private final List<NotificationRuleEntity> ruleList;    //规则列表

    public NotificationRuleAndGroupUnionModel(NotificationRuleGroupEntity group, List<NotificationRuleEntity> ruleList) {
        this.group = group;
        this.ruleList = ruleList;
    }

    public NotificationRuleGroupEntity getGroup() {
        return group;
    }

    public List<NotificationRuleEntity> getRuleList() {
        return ruleList;
    }
}
