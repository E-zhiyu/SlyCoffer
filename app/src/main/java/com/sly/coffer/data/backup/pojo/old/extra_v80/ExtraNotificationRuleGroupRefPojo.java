package com.sly.coffer.data.backup.pojo.old.extra_v80;

public class ExtraNotificationRuleGroupRefPojo {
    private long ruleId;
    private long groupId;
    private int order;

    public ExtraNotificationRuleGroupRefPojo() {
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
