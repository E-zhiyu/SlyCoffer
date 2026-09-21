package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class NotificationRuleDataMap {
    private List<NotificationRulePojo> notificationRuleList;
    private List<NotificationRuleTagRefPojo> notificationRuleTagRefList;
    private List<NotificationRuleTransferPojo> notificationRuleTransferList;
    private List<NotificationRuleGroupPojo> notificationRuleGroupList;
    private List<NotificationRuleGroupRefPojo> notificationRuleGroupRefList;

    public NotificationRuleDataMap() {
    }

    public List<NotificationRulePojo> getNotificationRuleList() {
        return notificationRuleList;
    }

    public void setNotificationRuleList(List<NotificationRulePojo> notificationRuleList) {
        this.notificationRuleList = notificationRuleList;
    }

    public List<NotificationRuleTagRefPojo> getNotificationRuleTagRefList() {
        return notificationRuleTagRefList;
    }

    public void setNotificationRuleTagRefList(List<NotificationRuleTagRefPojo> notificationRuleTagRefList) {
        this.notificationRuleTagRefList = notificationRuleTagRefList;
    }

    public List<NotificationRuleTransferPojo> getNotificationRuleTransferList() {
        return notificationRuleTransferList;
    }

    public void setNotificationRuleTransferList(List<NotificationRuleTransferPojo> notificationRuleTransferList) {
        this.notificationRuleTransferList = notificationRuleTransferList;
    }

    public List<NotificationRuleGroupPojo> getNotificationRuleGroupList() {
        return notificationRuleGroupList;
    }

    public void setNotificationRuleGroupList(List<NotificationRuleGroupPojo> notificationRuleGroupList) {
        this.notificationRuleGroupList = notificationRuleGroupList;
    }

    public List<NotificationRuleGroupRefPojo> getNotificationRuleGroupRefList() {
        return notificationRuleGroupRefList;
    }

    public void setNotificationRuleGroupRefList(List<NotificationRuleGroupRefPojo> notificationRuleGroupRefList) {
        this.notificationRuleGroupRefList = notificationRuleGroupRefList;
    }
}
