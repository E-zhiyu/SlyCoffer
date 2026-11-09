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
    private List<NotificationRulePojo> notificationRuleList;                    //规则
    private List<NotificationRuleTagRefPojo> notificationRuleTagRefList;        //标签映射
    private List<ExtraTransferPojo> extraTransferList;                          //转账账户
    private List<ExtraLoanPojo> extraLoanList;                                  //借款记录
    private List<NotificationRuleGroupPojo> notificationRuleGroupList;          //规则分组
    private List<NotificationRuleGroupRefPojo> notificationRuleGroupRefList;    //规则分组与规则映射

    public NotificationRuleDataMap(List<NotificationRulePojo> notificationRuleList, List<NotificationRuleTagRefPojo> notificationRuleTagRefList, List<ExtraTransferPojo> extraTransferList, List<ExtraLoanPojo> extraLoanList, List<NotificationRuleGroupPojo> notificationRuleGroupList, List<NotificationRuleGroupRefPojo> notificationRuleGroupRefList) {
        this.notificationRuleList = notificationRuleList;
        this.notificationRuleTagRefList = notificationRuleTagRefList;
        this.extraTransferList = extraTransferList;
        this.extraLoanList = extraLoanList;
        this.notificationRuleGroupList = notificationRuleGroupList;
        this.notificationRuleGroupRefList = notificationRuleGroupRefList;
    }

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
