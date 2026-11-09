package com.sly.coffer.ui.pages.main.bookkeeping;

import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>(null);     //监听到的当前位置
    private final MutableLiveData<Boolean> isLocationOnLiveData = new MutableLiveData<>(false); //是否开始位置监听

    @Nullable
    public Location getLocation() {
        return locationLiveData.getValue();
    }

    public MutableLiveData<Location> getLocationLiveData() {
        return locationLiveData;
    }

    public void setLocation(@Nullable Location location) {
        locationLiveData.setValue(location);
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
