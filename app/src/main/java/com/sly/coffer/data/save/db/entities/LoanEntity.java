package com.sly.coffer.data.save.db.entities;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

@Entity(
        tableName = "loans",
        foreignKeys = {
                @ForeignKey(
                        entity = AccountEntity.class,
                        parentColumns = "accountId",
                        childColumns = "accountId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "accountId"),
                @Index(value = "loanTargetName"),
                @Index(value = "repayDate")
        }
)
public class LoanEntity {
    @PrimaryKey
    private long accountId;                 //附属的流水记录的编号
    private String loanTargetName;          //借钱对象的名称
    private int interestPeriodType;         //结息周期种类
    private int yearBasepoint;              //年利率基点
    private boolean allowCompoundInterest;  //是否允许复利
    @Nullable
    private LocalDate repayDate;            //应还日期

    public LoanEntity(long accountId, String loanTargetName, int interestPeriodType, int yearBasepoint, boolean allowCompoundInterest, @Nullable LocalDate repayDate) {
        this.accountId = accountId;
        this.loanTargetName = loanTargetName;
        this.interestPeriodType = interestPeriodType;
        this.yearBasepoint = yearBasepoint;
        this.allowCompoundInterest = allowCompoundInterest;
        this.repayDate = repayDate;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getLoanTargetName() {
        return loanTargetName;
    }

    public void setLoanTargetName(String loanTargetName) {
        this.loanTargetName = loanTargetName;
    }

    public int getYearBasepoint() {
        return yearBasepoint;
    }

    public void setYearBasepoint(int yearBasepoint) {
        this.yearBasepoint = yearBasepoint;
    }

    public boolean isAllowCompoundInterest() {
        return allowCompoundInterest;
    }

    public void setAllowCompoundInterest(boolean allowCompoundInterest) {
        this.allowCompoundInterest = allowCompoundInterest;
    }

    @Nullable
    public LocalDate getRepayDate() {
        return repayDate;
    }

    public void setRepayDate(@Nullable LocalDate repayDate) {
        this.repayDate = repayDate;
    }

    public int getInterestPeriodType() {
        return interestPeriodType;
    }

    public void setInterestPeriodType(int interestPeriodType) {
        this.interestPeriodType = interestPeriodType;
    }
}
