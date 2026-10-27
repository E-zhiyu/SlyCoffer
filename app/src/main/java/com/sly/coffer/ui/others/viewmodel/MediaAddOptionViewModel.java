package com.sly.coffer.ui.others.viewmodel;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MediaAddOptionViewModel extends ViewModel {
    private Uri cameraFileUri = null;   //相机拍照得到的临时图片 File 类型的 Uri
    private final MutableLiveData<Integer> clickEvent = new MutableLiveData<>();

    public MutableLiveData<Integer> getClickEvent() {
        return clickEvent;
    }

    public void setClickEvent(int eventCode) {
        this.clickEvent.setValue(eventCode);
    }

    public Uri getCameraFileUri() {
        return cameraFileUri;
    }

    public void setCameraFileUri(Uri cameraFileUri) {
        this.cameraFileUri = cameraFileUri;
    }
}
