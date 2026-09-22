package com.sly.coffer.ui.others.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;

public class TagMultiSelectViewModel extends ViewModel {
    private final MutableLiveData<Set<Long>> checkedIdLiveData = new MutableLiveData<>();

    public MutableLiveData<Set<Long>> getCheckedIdLiveData() {
        return checkedIdLiveData;
    }

    public void updateCheckedIdLiveData(Set<Long> idSet) {
        this.checkedIdLiveData.setValue(idSet);
    }
}
