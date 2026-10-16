package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraNotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraNotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraNotificationRulePojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraNotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraNotificationRuleTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraNotificationRuleDataMap {
    private List<ExtraNotificationRulePojo> notificationRuleList;
    private List<ExtraNotificationRuleTagRefPojo> notificationRuleTagRefList;
    private List<ExtraNotificationRuleTransferPojo> notificationRuleTransferList;
    private List<ExtraNotificationRuleGroupPojo> notificationRuleGroupList;
    private List<ExtraNotificationRuleGroupRefPojo> notificationRuleGroupRefList;

    public ExtraNotificationRuleDataMap() {
    }

    public List<ExtraNotificationRulePojo> getNotificationRuleList() {
        return notificationRuleList;
    }

    public void setNotificationRuleList(List<ExtraNotificationRulePojo> notificationRuleList) {
        this.notificationRuleList = notificationRuleList;
    }

    public List<ExtraNotificationRuleTagRefPojo> getNotificationRuleTagRefList() {
        return notificationRuleTagRefList;
    }

    public void setNotificationRuleTagRefList(List<ExtraNotificationRuleTagRefPojo> notificationRuleTagRefList) {
        this.notificationRuleTagRefList = notificationRuleTagRefList;
    }

    public List<ExtraNotificationRuleTransferPojo> getNotificationRuleTransferList() {
        return notificationRuleTransferList;
    }

    public void setNotificationRuleTransferList(List<ExtraNotificationRuleTransferPojo> notificationRuleTransferList) {
        this.notificationRuleTransferList = notificationRuleTransferList;
    }

    public List<ExtraNotificationRuleGroupPojo> getNotificationRuleGroupList() {
        return notificationRuleGroupList;
    }

    public void setNotificationRuleGroupList(List<ExtraNotificationRuleGroupPojo> notificationRuleGroupList) {
        this.notificationRuleGroupList = notificationRuleGroupList;
    }

    public List<ExtraNotificationRuleGroupRefPojo> getNotificationRuleGroupRefList() {
        return notificationRuleGroupRefList;
    }

    public void setNotificationRuleGroupRefList(List<ExtraNotificationRuleGroupRefPojo> notificationRuleGroupRefList) {
        this.notificationRuleGroupRefList = notificationRuleGroupRefList;
    }
}
