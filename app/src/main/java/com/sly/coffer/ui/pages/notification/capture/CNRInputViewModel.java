package com.sly.coffer.ui.pages.notification.capture;

import androidx.lifecycle.ViewModel;

public class CNRInputViewModel extends ViewModel {
    private int groupPos = 1;   //金额捕获组的位置

    public int getGroupPos() {
        return groupPos;
    }

    public void setGroupPos(int groupPos) {
        this.groupPos = groupPos;
    }
}
