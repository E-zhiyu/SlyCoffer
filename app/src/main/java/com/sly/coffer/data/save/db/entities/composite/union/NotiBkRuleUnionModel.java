package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class NotiBkRuleUnionModel {
    @Embedded
    private final NotificationRuleEntity rule;                          //规则实体
    @Nullable
    @Relation(
            entity = ExtraTransferEntity.class,
            parentColumn = "transferId",
            entityColumn = "transferId"
    )
    private final ExtraTransferEntity transfer;                         //该规则的转账账户信息
    @Nullable
    @Relation(
            entity = ExtraLoanEntity.class,
            parentColumn = "loanId",
            entityColumn = "loanId"
    )
    private final ExtraLoanEntity loan;                                 //该规则的借款额外数据
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "tagId",
            associateBy = @Junction(NotificationRuleTagRefEntity.class)
    )
    private final List<TagEntity> tagList;                              //该规则绑定的标签列表
    @Relation(
            entity = NotificationRuleGroupRefEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final List<NotificationRuleGroupRefEntity> groupRefList;    //与自身相关的分组映射列表

    public NotiBkRuleUnionModel(NotificationRuleEntity rule, @Nullable ExtraTransferEntity transfer, @Nullable ExtraLoanEntity loan, List<TagEntity> tagList, List<NotificationRuleGroupRefEntity> groupRefList) {
        this.rule = rule;
        this.transfer = transfer;
        this.loan = loan;
        this.tagList = tagList;
        this.groupRefList = groupRefList;
    }

    public NotificationRuleEntity getRule() {
        return rule;
    }

    @Nullable
    public ExtraTransferEntity getTransfer() {
        return transfer;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<NotificationRuleGroupRefEntity> getGroupRefList() {
        return groupRefList;
    }

    @Nullable
    public ExtraLoanEntity getLoan() {
        return loan;
    }
}
