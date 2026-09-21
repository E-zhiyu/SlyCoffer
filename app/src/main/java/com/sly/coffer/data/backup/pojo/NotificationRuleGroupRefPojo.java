package com.sly.coffer.data.backup.pojo;

public class NotificationRuleGroupRefPojo {
    private long ruleId;
    private long groupId;
    private int order;

    public NotificationRuleGroupRefPojo() {
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
