package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notificationRuleGroups"
)
public class NotificationRuleGroupEntity {
    @PrimaryKey(autoGenerate = true)
    private long groupId;   //主键
    private String name;    //名称

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
}
