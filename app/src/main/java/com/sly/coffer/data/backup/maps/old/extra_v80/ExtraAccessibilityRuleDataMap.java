package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraAccessibilityRuleDataMap {
    private List<AccessibilityRulePojo> rulePojoList;
    private List<AccessibilityRuleTagRefPojo> tagRefPojoList;
    private List<AccessibilityRuleTransferPojo> transferPojoList;
    private List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList;

    public ExtraAccessibilityRuleDataMap() {
    }

    public List<AccessibilityRulePojo> getRulePojoList() {
        return rulePojoList;
    }

    public void setRulePojoList(List<AccessibilityRulePojo> rulePojoList) {
        this.rulePojoList = rulePojoList;
    }

    public List<AccessibilityRuleTagRefPojo> getTagRefPojoList() {
        return tagRefPojoList;
    }

    public void setTagRefPojoList(List<AccessibilityRuleTagRefPojo> tagRefPojoList) {
        this.tagRefPojoList = tagRefPojoList;
    }

    public List<AccessibilityRuleTransferPojo> getTransferPojoList() {
        return transferPojoList;
    }

    public void setTransferPojoList(List<AccessibilityRuleTransferPojo> transferPojoList) {
        this.transferPojoList = transferPojoList;
    }

    public List<AccessibilityRuleKeywordGroupPojo> getKeywordGroupPojoList() {
        return keywordGroupPojoList;
    }

    public void setKeywordGroupPojoList(List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList) {
        this.keywordGroupPojoList = keywordGroupPojoList;
    }
}
