package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class BookkeepingNotiRuleUnionModel {
    @Embedded
    private final NotificationRuleEntity rule;                          //规则实体
    @Relation(
            entity = NotificationRuleTransferEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final NotificationRuleTransferEntity transfer;              //该规则的转账账户信息（如果有）
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "tagId",
            associateBy = @Junction(NotificationRuleTagRefEntity.class)
    )
    private final List<TagEntity> tagList;                              //该规则绑定的标签列表
    @Relation(
            entity = NotificationRuleGroupRefEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final List<NotificationRuleGroupRefEntity> groupRefList;    //与自身相关的分组映射列表

    public BookkeepingNotiRuleUnionModel(NotificationRuleEntity rule, NotificationRuleTransferEntity transfer, List<TagEntity> tagList, List<NotificationRuleGroupRefEntity> groupRefList) {
        this.rule = rule;
        this.transfer = transfer;
        this.tagList = tagList;
        this.groupRefList = groupRefList;
    }

    public NotificationRuleEntity getRule() {
        return rule;
    }

    public NotificationRuleTransferEntity getTransfer() {
        return transfer;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<NotificationRuleGroupRefEntity> getGroupRefList() {
        return groupRefList;
    }
}
