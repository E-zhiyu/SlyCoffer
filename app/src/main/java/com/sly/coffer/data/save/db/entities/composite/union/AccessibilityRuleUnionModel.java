package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class AccessibilityRuleUnionModel {
    @Embedded
    private final AccessibilityRuleEntity rule; //规则本体数据
    @Nullable
    @Relation(
            entity = ExtraTransferEntity.class,
            parentColumn = "transferId",
            entityColumn = "transferId"
    )
    private final ExtraTransferEntity transfer; //转账额外数据
    @Nullable
    @Relation(
            entity = ExtraLoanEntity.class,
            parentColumn = "loanId",
            entityColumn = "loanId"
    )
    private final ExtraLoanEntity loan;         //借款额外数据
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "tagId",
            associateBy = @Junction(AccessibilityRuleTagRefEntity.class)
    )
    private final List<TagEntity> tagList;      //标签列表
    @Relation(
            entity = AccessibilityRuleKeywordGroupEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final List<AccessibilityRuleKeywordGroupEntity> keywordGroupList;   //关键词分组列表

    public AccessibilityRuleUnionModel(AccessibilityRuleEntity rule, @Nullable ExtraTransferEntity transfer, @Nullable ExtraLoanEntity loan, List<TagEntity> tagList, List<AccessibilityRuleKeywordGroupEntity> keywordGroupList) {
        this.rule = rule;
        this.transfer = transfer;
        this.loan = loan;
        this.tagList = tagList;
        this.keywordGroupList = keywordGroupList;
    }

    public AccessibilityRuleEntity getRule() {
        return rule;
    }

    @Nullable
    public ExtraTransferEntity getTransfer() {
        return transfer;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<AccessibilityRuleKeywordGroupEntity> getKeywordGroupList() {
        return keywordGroupList;
    }

    @Nullable
    public ExtraLoanEntity getLoan() {
        return loan;
    }
}
