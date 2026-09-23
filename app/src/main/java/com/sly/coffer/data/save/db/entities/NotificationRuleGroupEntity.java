package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notificationRuleGroups",
        indices = {
                @Index(value = "order")
        }
)
public class NotificationRuleGroupEntity {
    @PrimaryKey(autoGenerate = true)
    private long groupId;   //主键
    private String name;    //名称
    private int order;      //排序序号

    public NotificationRuleGroupEntity(String name) {
        this.name = name;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
