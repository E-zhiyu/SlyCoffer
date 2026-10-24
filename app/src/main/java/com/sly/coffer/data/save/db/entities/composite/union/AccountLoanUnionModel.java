package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;

import java.util.List;

public class AccountLoanUnionModel {
    @Embedded
    private final AccountEntity account;                //借款记录的流水数据
    @Nullable
    @Relation(
            entity = ExtraLoanEntity.class,
            entityColumn = "loanId",
            parentColumn = "loanId"
    )
    private final ExtraLoanEntity loan;                 //借款记录
    @Relation(
            entity = AccountEntity.class,
            parentColumn = "accountId",
            entityColumn = "repayAccountId"
    )
    private final List<AccountEntity> repaymentList;    //所有还款的流水记录

    public AccountLoanUnionModel(@Nullable ExtraLoanEntity loan, AccountEntity account, List<AccountEntity> repaymentList) {
        this.loan = loan;
        this.account = account;
        this.repaymentList = repaymentList;
    }

    public AccountEntity getAccount() {
        return account;
    }

    @Nullable
    public ExtraLoanEntity getLoan() {
        return loan;
    }

    public List<AccountEntity> getRepaymentList() {
        return repaymentList;
    }
}
