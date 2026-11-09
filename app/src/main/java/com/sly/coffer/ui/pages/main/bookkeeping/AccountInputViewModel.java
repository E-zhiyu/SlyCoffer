package com.sly.coffer.ui.pages.main.bookkeeping;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.AccountDetailUnionModel;

public class AccountInputViewModel extends ViewModel {
    private final MutableLiveData<AccountDetailUnionModel> accountModelLiveData = new MutableLiveData<>();  //缓存的流水记录数据模型

    public MutableLiveData<AccountDetailUnionModel> getAccountModelLiveData() {
        return accountModelLiveData;
    }

    public void setAccountModel(AccountDetailUnionModel accountModel) {
        accountModelLiveData.setValue(accountModel);
    }

    /**
     * 获取已缓存的流水记录数据模型
     *
     * @return 缓存的流水记录数据模型
     */
    @Nullable
    public AccountDetailUnionModel getAccountUnionModel() {
        return accountModelLiveData.getValue();
    }
}
