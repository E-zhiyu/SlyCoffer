package com.sly.coffer.data.save.db.entities;

import androidx.room.ColumnInfo;
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
                @Index(value = "order")
        }
)
public class NotificationRuleGroupRefEntity {
    private long ruleId;
    private long groupId;
    @ColumnInfo(defaultValue = "0")
    private int order;      //排序优先级（越大优先级越高）

    public NotificationRuleGroupRefEntity(long ruleId, long groupId) {
        this.ruleId = ruleId;
        this.groupId = groupId;
        this.order = 0;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
