package com.sly.coffer.ui.pages.main.bookkeeping;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.enums.types.AccountType;

public class AccountViewModel extends ViewModel {
    private final MutableLiveData<AccountType> typeLiveData = new MutableLiveData<>(AccountType.EXPENSE);

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
}
