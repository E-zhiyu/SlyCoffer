package com.sly.coffer.ui.pages.notification.capture;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;

public class CNRInputViewModel extends ViewModel {
    private final MutableLiveData<CapturedNotificationEntity> notificationLiveData = new MutableLiveData<>();   //缓存的数据
    private int groupPos = 1;   //金额捕获组的位置

    public int getGroupPos() {
        return groupPos;
    }

    public void setGroupPos(int groupPos) {
        this.groupPos = groupPos;
    }

    public MutableLiveData<CapturedNotificationEntity> getNotificationLiveData() {
        return notificationLiveData;
    }

    @Nullable
    public CapturedNotificationEntity getNotification() {
        return notificationLiveData.getValue();
    }

    public void setNotification(CapturedNotificationEntity notification) {
        notificationLiveData.setValue(notification);
    }
}
