package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "loanRepayRef",
        primaryKeys = {"repayAccountId","loanAccountId"},
        foreignKeys = {
                @ForeignKey(
                        entity = AccountEntity.class,
                        parentColumns = "accountId",
                        childColumns = "repayAccountId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = AccountEntity.class,
                        parentColumns = "accountId",
                        childColumns = "loanAccountId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class LoanRepayRefEntity {
    private long repayAccountId;    //作为还款记录的流水记录编号
    private long loanAccountId;     //有借款记录的流水记录编号

    public LoanRepayRefEntity(long repayAccountId, long loanAccountId) {
        this.repayAccountId = repayAccountId;
        this.loanAccountId = loanAccountId;
    }

    public long getRepayAccountId() {
        return repayAccountId;
    }

    public void setRepayAccountId(long repayAccountId) {
        this.repayAccountId = repayAccountId;
    }

    public long getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(long loanAccountId) {
        this.loanAccountId = loanAccountId;
    }
}
