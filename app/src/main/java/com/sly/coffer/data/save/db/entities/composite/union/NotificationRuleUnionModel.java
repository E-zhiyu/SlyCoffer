package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class NotificationRuleUnionModel {
    @Embedded
    private final NotificationRuleEntity rule;                  //通知规则
    @Nullable
    @Relation(
            entity = ExtraTransferEntity.class,
            parentColumn = "transferId",
            entityColumn = "transferId"
    )
    private final ExtraTransferEntity transfer;                 //转账账户
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "tagId",
            associateBy = @Junction(NotificationRuleTagRefEntity.class)
    )
    private final List<TagEntity> tagList;                      //标签
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "groupId",
            associateBy = @Junction(NotificationRuleGroupRefEntity.class)
    )
    private final List<NotificationRuleGroupEntity> groupList;  //规则分组

    public NotificationRuleUnionModel(NotificationRuleEntity rule, @Nullable ExtraTransferEntity transfer, List<TagEntity> tagList, List<NotificationRuleGroupEntity> groupList) {
        this.rule = rule;
        this.transfer = transfer;
        this.tagList = tagList;
        this.groupList = groupList;
    }

    public NotificationRuleEntity getRule() {
        return rule;
    }

    @Nullable
    public ExtraTransferEntity getTransfer() {
        return transfer;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<NotificationRuleGroupEntity> getGroupList() {
        return groupList;
    }
}
