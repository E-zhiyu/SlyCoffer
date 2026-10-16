package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTransferPojo;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class AccessibilityRuleDataMap {
    private List<AccessibilityRulePojo> ruleList;
    private List<AccessibilityRuleTagRefPojo> tagRefList;
    private List<ExtraTransferPojo> extraTransferList;
    private List<ExtraLoanPojo> extraLoanList;
    private List<AccessibilityRuleKeywordGroupPojo> keywordGroupList;

    public AccessibilityRuleDataMap() {
    }

    public List<AccessibilityRulePojo> getRuleList() {
        return ruleList;
    }

    public void setRuleList(List<AccessibilityRulePojo> ruleList) {
        this.ruleList = ruleList;
    }

    public List<AccessibilityRuleTagRefPojo> getTagRefList() {
        return tagRefList;
    }

    public void setTagRefList(List<AccessibilityRuleTagRefPojo> tagRefList) {
        this.tagRefList = tagRefList;
    }

    public List<ExtraTransferPojo> getExtraTransferList() {
        return extraTransferList;
    }

    public void setExtraTransferList(List<ExtraTransferPojo> extraTransferList) {
        this.extraTransferList = extraTransferList;
    }

    public List<ExtraLoanPojo> getExtraLoanList() {
        return extraLoanList;
    }

    public void setExtraLoanList(List<ExtraLoanPojo> extraLoanList) {
        this.extraLoanList = extraLoanList;
    }

    public List<AccessibilityRuleKeywordGroupPojo> getKeywordGroupList() {
        return keywordGroupList;
    }

    public void setKeywordGroupList(List<AccessibilityRuleKeywordGroupPojo> keywordGroupList) {
        this.keywordGroupList = keywordGroupList;
    }
}
