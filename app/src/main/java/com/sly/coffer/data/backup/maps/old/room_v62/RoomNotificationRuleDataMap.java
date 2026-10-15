package com.sly.coffer.data.backup.maps.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomNotificationRulePojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomNotificationRuleTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomNotificationRuleDataMap {
    private List<RoomNotificationRuleTransferPojo> rule_account;
    private List<RoomNotificationRulePojo> rule_data;

    public RoomNotificationRuleDataMap() {
    }

    public List<RoomNotificationRuleTransferPojo> getRule_account() {
        return rule_account;
    }

    public void setRule_account(List<RoomNotificationRuleTransferPojo> rule_account) {
        this.rule_account = rule_account;
    }

    public List<RoomNotificationRulePojo> getRule_data() {
        return rule_data;
    }

    public void setRule_data(List<RoomNotificationRulePojo> rule_data) {
        this.rule_data = rule_data;
    }
}
