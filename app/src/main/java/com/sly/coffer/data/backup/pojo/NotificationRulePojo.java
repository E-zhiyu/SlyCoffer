package com.sly.coffer.data.backup.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class NotificationRulePojo {
    private long ruleId;
    private String name;
    private int type;
    private String packageName;
    private String targetTitle;
    private String contentRegex;
    private int captureGroupPos = 1;
    private boolean enabled = true;
    private Long transferId = null;
    private Long loanId = null;
    private Long repayAccountId = null;

    public NotificationRulePojo() {
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getContentRegex() {
        return contentRegex;
    }

    public void setContentRegex(String contentRegex) {
        this.contentRegex = contentRegex;
    }

    public int getCaptureGroupPos() {
        return captureGroupPos;
    }

    public void setCaptureGroupPos(int captureGroupPos) {
        this.captureGroupPos = captureGroupPos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getRepayAccountId() {
        return repayAccountId;
    }

    public void setRepayAccountId(Long repayAccountId) {
        this.repayAccountId = repayAccountId;
    }
}
