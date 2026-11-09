package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class AccessibilityRuleDataMap {
    private List<AccessibilityRulePojo> ruleList;                       //规则
    private List<AccessibilityRuleTagRefPojo> tagRefList;               //标签映射
    private List<ExtraTransferPojo> extraTransferList;                  //转账账户
    private List<ExtraLoanPojo> extraLoanList;                          //借款记录
    private List<AccessibilityRuleKeywordGroupPojo> keywordGroupList;   //关键词组合

    public AccessibilityRuleDataMap(List<AccessibilityRulePojo> ruleList, List<AccessibilityRuleTagRefPojo> tagRefList, List<ExtraTransferPojo> extraTransferList, List<ExtraLoanPojo> extraLoanList, List<AccessibilityRuleKeywordGroupPojo> keywordGroupList) {
        this.ruleList = ruleList;
        this.tagRefList = tagRefList;
        this.extraTransferList = extraTransferList;
        this.extraLoanList = extraLoanList;
        this.keywordGroupList = keywordGroupList;
    }

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
