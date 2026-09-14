package com.sly.coffer.ui.pages.notification.capture;

import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.enums.types.AccountType;

public class CNRInputViewModel extends ViewModel {
    private int groupPos = 1;   //金额捕获组的位置
    private AccountType type = AccountType.EXPENSE;         //流水种类

    public int getGroupPos() {
        return groupPos;
    }

    public void setGroupPos(int groupPos) {
        this.groupPos = groupPos;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }
}
