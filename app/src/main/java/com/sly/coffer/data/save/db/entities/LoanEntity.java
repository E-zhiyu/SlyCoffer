package com.sly.coffer.data.save.db.entities;

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
                @Index(value = "accountId")
        }
)
public class LoanEntity {
    @PrimaryKey
    private long accountId;         //附属的流水记录的编号
    private String targetName;      //借钱对象的名称
    private int basepoint;          //利率基点
    private boolean allowCompoundInterest;  //是否允许复利
    private int type;               //种类(借入或借出)
    private LocalDate repayDate;    //应还日期

    public LoanEntity(long accountId, String targetName, int basepoint, boolean allowCompoundInterest, int type, LocalDate repayDate) {
        this.accountId = accountId;
        this.targetName = targetName;
        this.basepoint = basepoint;
        this.allowCompoundInterest = allowCompoundInterest;
        this.type = type;
        this.repayDate = repayDate;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public int getBasepoint() {
        return basepoint;
    }

    public void setBasepoint(int basepoint) {
        this.basepoint = basepoint;
    }

    public boolean isAllowCompoundInterest() {
        return allowCompoundInterest;
    }

    public void setAllowCompoundInterest(boolean allowCompoundInterest) {
        this.allowCompoundInterest = allowCompoundInterest;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public LocalDate getRepayDate() {
        return repayDate;
    }

    public void setRepayDate(LocalDate repayDate) {
        this.repayDate = repayDate;
    }
}
