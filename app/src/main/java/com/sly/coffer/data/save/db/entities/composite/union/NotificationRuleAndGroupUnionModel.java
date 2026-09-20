package com.sly.coffer.data.save.db.entities.composite.union;

import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;

import java.util.List;

public class NotificationRuleAndGroupUnionModel {
    private final NotificationRuleGroupEntity group;        //分组实体
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
