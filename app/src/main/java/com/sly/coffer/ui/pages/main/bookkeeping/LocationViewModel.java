package com.sly.coffer.ui.pages.main.bookkeeping;

import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {
    @Nullable
    private Location location;                      //监听到的当前位置
    private final MutableLiveData<Boolean> isLocationOnLiveData = new MutableLiveData<>(false); //是否开始位置监听

    @Nullable
    public Location getLocation() {
        return location;
    }

    public void setLocation(@Nullable Location location) {
        this.location = location;
    }

    public boolean isLocationOn() {
        return isLocationOnLiveData.getValue() != null && isLocationOnLiveData.getValue();
    }

    public MutableLiveData<Boolean> getIsLocationOnLiveData() {
        return isLocationOnLiveData;
    }

    public void setLocationOn(boolean locationOn) {
        isLocationOnLiveData.setValue(locationOn);
    }
}
