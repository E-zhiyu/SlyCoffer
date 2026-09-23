package com.sly.coffer.data.backup.pojo;

public class NotificationRuleGroupPojo {
    private long groupId;
    private String name;
    private int order;

    public NotificationRuleGroupPojo() {
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
