package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "notificationRuleGroupRef",
        primaryKeys = {"ruleId", "groupId"},
        foreignKeys = {
                @ForeignKey(
                        entity = NotificationRuleEntity.class,
                        childColumns = "ruleId",
                        parentColumns = "ruleId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = NotificationRuleGroupEntity.class,
                        parentColumns = "groupId",
                        childColumns = "groupId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "ruleId"),
                @Index(value = "groupId")
        }
)
public class NotificationRuleGroupRefEntity {
    private long ruleId;
    private long groupId;

    public NotificationRuleGroupRefEntity(long ruleId, long groupId) {
        this.ruleId = ruleId;
        this.groupId = groupId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
}
