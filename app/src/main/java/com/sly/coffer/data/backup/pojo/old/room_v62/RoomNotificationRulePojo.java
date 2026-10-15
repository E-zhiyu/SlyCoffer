package com.sly.coffer.data.backup.pojo.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomNotificationRulePojo {
    private String content;
    private String packageName;
    private String ruleName;
    private long ruleNo;
    private long tag_no;
    private String title;
    private String type;

    public RoomNotificationRulePojo() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public long getRuleNo() {
        return ruleNo;
    }

    public void setRuleNo(long ruleNo) {
        this.ruleNo = ruleNo;
    }

    public long getTag_no() {
        return tag_no;
    }

    public void setTag_no(long tag_no) {
        this.tag_no = tag_no;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
