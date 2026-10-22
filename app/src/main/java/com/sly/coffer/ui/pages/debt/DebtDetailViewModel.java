package com.sly.coffer.ui.pages.debt;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.AccountLoanUnionModel;

public class DebtDetailViewModel extends ViewModel {
    private final MutableLiveData<AccountLoanUnionModel> accountLoanModelLiveData = new MutableLiveData<>();

    public MutableLiveData<AccountLoanUnionModel> getAccountLoanModelLiveData() {
        return accountLoanModelLiveData;
    }

    @Nullable
    public AccountLoanUnionModel getAccountLoanUnionModel() {
        return accountLoanModelLiveData.getValue();
    }

    public void updateAccountLoanUnionModel(AccountLoanUnionModel model) {
        accountLoanModelLiveData.setValue(model);
    }
}
