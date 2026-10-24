package com.sly.coffer.data.save.db.entities;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.util.Objects;

@Entity(
        tableName = "extraLoan"
)
public class ExtraLoanEntity {
    @PrimaryKey(autoGenerate = true)
    private long loanId;                    //自增主键
    private String loanTargetName;          //借钱对象的名称
    private int interestPeriodType;         //结息周期种类
    private int yearBasepoint;              //年利率基点（精确到百分比小数后两位，也就是利率=基点/10000）
    private boolean allowCompoundInterest;  //是否允许复利
    @Nullable
    private LocalDate repayDate;            //应还日期

    /**
     * 作为空外键对象的构造方法
     */
    @Ignore
    public ExtraLoanEntity() {
        loanId = -1;
        loanTargetName = null;
        interestPeriodType = 0;
        yearBasepoint = 0;
        allowCompoundInterest = false;
        repayDate = null;
    }

    public ExtraLoanEntity(String loanTargetName, int interestPeriodType, int yearBasepoint, boolean allowCompoundInterest, @Nullable LocalDate repayDate) {
        this.loanTargetName = loanTargetName;
        this.interestPeriodType = interestPeriodType;
        this.yearBasepoint = yearBasepoint;
        this.allowCompoundInterest = allowCompoundInterest;
        this.repayDate = repayDate;
    }

    public long getLoanId() {
        return loanId;
    }

    public void setLoanId(long loanId) {
        this.loanId = loanId;
    }

    public String getLoanTargetName() {
        return loanTargetName;
    }

    public void setLoanTargetName(String loanTargetName) {
        this.loanTargetName = loanTargetName;
    }

    public int getInterestPeriodType() {
        return interestPeriodType;
    }

    public void setInterestPeriodType(int interestPeriodType) {
        this.interestPeriodType = interestPeriodType;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExtraLoanEntity loan = (ExtraLoanEntity) o;
        return interestPeriodType == loan.interestPeriodType && yearBasepoint == loan.yearBasepoint && allowCompoundInterest == loan.allowCompoundInterest;
    }

    @Override
    public int hashCode() {
        return Objects.hash(interestPeriodType, yearBasepoint, allowCompoundInterest);
    }
}
