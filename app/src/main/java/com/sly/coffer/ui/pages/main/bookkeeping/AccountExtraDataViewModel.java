package com.sly.coffer.ui.pages.main.bookkeeping;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;
import com.sly.coffer.data.save.db.entities.AccountEntity;

public class AccountExtraDataViewModel extends ViewModel {
    private final MutableLiveData<AccountType> typeLiveData = new MutableLiveData<>(AccountType.EXPENSE);
    private final MutableLiveData<AccountEntity> relatedLoanAccountLiveData = new MutableLiveData<>();
    private InterestPeriodType interestPeriodType = InterestPeriodType.EVERY_MONTH; //结息周期

    /**
     * 更新流水记录种类
     *
     * @param type 更新后的种类
     */
    public void updateAccountType(AccountType type) {
        typeLiveData.postValue(type);
    }

    public MutableLiveData<AccountType> getAccountTypeLiveData() {
        return typeLiveData;
    }

    /**
     * 获取流水记录种类
     *
     * @return 当前流水记录种类
     */
    public AccountType getAccountType() {
        AccountType type = typeLiveData.getValue();
        return type == null ? AccountType.EXPENSE : type;
    }

    /**
     * 更新关联的借款记录
     *
     * @param account 带有借款记录的流水记录
     */
    public void updateRelatedLoan(AccountEntity account) {
        relatedLoanAccountLiveData.postValue(account);
    }

    public MutableLiveData<AccountEntity> getRelatedLoanAccountLiveData() {
        return relatedLoanAccountLiveData;
    }

    /**
     * 获取关联的借款记录的流水记录编号
     *
     * @return 关联的借款记录的流水记录编号
     */
    public Long getRepayAccountId() {
        AccountEntity relatedLoanAccount = relatedLoanAccountLiveData.getValue();
        return relatedLoanAccount == null ? null : relatedLoanAccount.getAccountId();
    }

    public InterestPeriodType getInterestPeriodType() {
        return interestPeriodType;
    }

    public void setInterestPeriodType(InterestPeriodType interestPeriodType) {
        this.interestPeriodType = interestPeriodType;
    }
}
