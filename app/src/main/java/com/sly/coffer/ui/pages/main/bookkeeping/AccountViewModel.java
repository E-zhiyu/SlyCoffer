package com.sly.coffer.ui.pages.main.bookkeeping;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;

public class AccountViewModel extends ViewModel {
    private final MutableLiveData<AccountType> typeLiveData = new MutableLiveData<>(AccountType.EXPENSE);
    private InterestPeriodType interestPeriodType = InterestPeriodType.EVERY_MONTH; //结息周期
    private long relatedLoanAccountId = 0;                                          //还款额外数据：关联的借款记录编号

    public void updateAccountType(AccountType type) {
        typeLiveData.postValue(type);
    }

    public MutableLiveData<AccountType> getTypeLiveData() {
        return typeLiveData;
    }

    /**
     * 获取流水记录种类
     * @return 当前流水记录种类
     */
    public AccountType getAccountType() {
        AccountType type = typeLiveData.getValue();
        return type == null ? AccountType.EXPENSE : type;
    }

    public InterestPeriodType getInterestPeriodType() {
        return interestPeriodType;
    }

    public void setInterestPeriodType(InterestPeriodType interestPeriodType) {
        this.interestPeriodType = interestPeriodType;
    }

    public long getRelatedLoanAccountId() {
        return relatedLoanAccountId;
    }

    public void setRelatedLoanAccountId(long relatedLoanAccountId) {
        this.relatedLoanAccountId = relatedLoanAccountId;
    }
}
