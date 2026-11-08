package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.NonNull;
import androidx.room.Embedded;

import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.Objects;

public class TagUseCountUnionModel {
    @Embedded
    private final TagEntity tag;
    private final int accountUseCount;  //流水记录的使用次数
    private final int notiRuleUseCount; //通知规则的使用次数
    private final int accRuleUseCount;  //无障碍规则使用次数
    private final int budgetUseCount;   //预算使用次数

    public TagUseCountUnionModel(TagEntity tag, int accountUseCount, int notiRuleUseCount, int accRuleUseCount, int budgetUseCount) {
        this.tag = tag;
        this.accountUseCount = accountUseCount;
        this.notiRuleUseCount = notiRuleUseCount;
        this.accRuleUseCount = accRuleUseCount;
        this.budgetUseCount = budgetUseCount;
    }

    public TagEntity getTag() {
        return tag;
    }

    public int getAccountUseCount() {
        return accountUseCount;
    }

    public int getNotiRuleUseCount() {
        return notiRuleUseCount;
    }

    public int getAccRuleUseCount() {
        return accRuleUseCount;
    }

    public int getBudgetUseCount() {
        return budgetUseCount;
    }

    /**
     * 判断使用次数是否完全相同
     *
     * @param other 另一个标签数据模型
     * @return 两个数据模型的使用次数是否完全相同
     */
    public boolean isUseCountSame(@NonNull TagUseCountUnionModel other) {
        return Objects.hash(accountUseCount, notiRuleUseCount, accRuleUseCount, budgetUseCount) ==
                Objects.hash(other.accountUseCount, other.notiRuleUseCount, other.accRuleUseCount, other.budgetUseCount);
    }
}
