package com.sly.coffer.data.backup.pojo.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomNotificationRuleTransferPojo {
    private String exportAccount;
    private String importAccount;
    private long ruleNo;

    public RoomNotificationRuleTransferPojo() {
    }

    public String getExportAccount() {
        return exportAccount;
    }

    public void setExportAccount(String exportAccount) {
        this.exportAccount = exportAccount;
    }

    public String getImportAccount() {
        return importAccount;
    }

    public void setImportAccount(String importAccount) {
        this.importAccount = importAccount;
    }

    public long getRuleNo() {
        return ruleNo;
    }

    public void setRuleNo(long ruleNo) {
        this.ruleNo = ruleNo;
    }
}
