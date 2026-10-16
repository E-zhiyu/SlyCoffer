package com.sly.coffer.data.backup.pojo;

import androidx.annotation.Nullable;

public class ExtraLoanPojo {
    private long loanId;                    //自增主键
    private String loanTargetName;          //借钱对象的名称
    private int interestPeriodType;         //结息周期种类
    private int yearBasepoint;              //年利率基点
    private boolean allowCompoundInterest;  //是否允许复利
    @Nullable
    private Long repayDate = -1L;           //应还日期

    public ExtraLoanPojo() {
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
    public Long getRepayDate() {
        return repayDate;
    }

    public void setRepayDate(@Nullable Long repayDate) {
        this.repayDate = repayDate;
    }
}
