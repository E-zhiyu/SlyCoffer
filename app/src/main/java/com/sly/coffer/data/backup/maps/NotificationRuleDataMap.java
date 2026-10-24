package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class NotificationRuleDataMap {
    private List<NotificationRulePojo> notificationRuleList;
    private List<NotificationRuleTagRefPojo> notificationRuleTagRefList;
    private List<ExtraTransferPojo> extraTransferList;
    private List<ExtraLoanPojo> extraLoanList;
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

    public List<ExtraTransferPojo> getExtraTransferList() {
        return extraTransferList;
    }

    public void setExtraTransferList(List<ExtraTransferPojo> extraTransferList) {
        this.extraTransferList = extraTransferList;
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

    public List<ExtraLoanPojo> getExtraLoanList() {
        return extraLoanList;
    }

    public void setExtraLoanList(List<ExtraLoanPojo> extraLoanList) {
        this.extraLoanList = extraLoanList;
    }
}
