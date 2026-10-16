package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccessibilityRuleTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraAccessibilityRuleDataMap {
    private List<ExtraAccessibilityRulePojo> rulePojoList;
    private List<ExtraAccessibilityRuleTagRefPojo> tagRefPojoList;
    private List<ExtraAccessibilityRuleTransferPojo> transferPojoList;
    private List<ExtraAccessibilityRuleKeywordGroupPojo> keywordGroupPojoList;

    public ExtraAccessibilityRuleDataMap() {
    }

    public List<ExtraAccessibilityRulePojo> getRulePojoList() {
        return rulePojoList;
    }

    public void setRulePojoList(List<ExtraAccessibilityRulePojo> rulePojoList) {
        this.rulePojoList = rulePojoList;
    }

    public List<ExtraAccessibilityRuleTagRefPojo> getTagRefPojoList() {
        return tagRefPojoList;
    }

    public void setTagRefPojoList(List<ExtraAccessibilityRuleTagRefPojo> tagRefPojoList) {
        this.tagRefPojoList = tagRefPojoList;
    }

    public List<ExtraAccessibilityRuleTransferPojo> getTransferPojoList() {
        return transferPojoList;
    }

    public void setTransferPojoList(List<ExtraAccessibilityRuleTransferPojo> transferPojoList) {
        this.transferPojoList = transferPojoList;
    }

    public List<ExtraAccessibilityRuleKeywordGroupPojo> getKeywordGroupPojoList() {
        return keywordGroupPojoList;
    }

    public void setKeywordGroupPojoList(List<ExtraAccessibilityRuleKeywordGroupPojo> keywordGroupPojoList) {
        this.keywordGroupPojoList = keywordGroupPojoList;
    }
}
