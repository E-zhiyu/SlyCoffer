package com.sly.coffer.ui.pages.notification;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;

public class GroupSelectViewModel extends ViewModel {
    private final MutableLiveData<Set<Long>> groupIdSetLiveData = new MutableLiveData<>();

    public MutableLiveData<Set<Long>> getGroupIdSetLiveData() {
        return groupIdSetLiveData;
    }

    /**
     * 更新选择的分组编号集合
     *
     * @param checkedIdSet 选择的分组编号集合
     */
    public void updateCheckedGroupId(Set<Long> checkedIdSet) {
        groupIdSetLiveData.postValue(checkedIdSet);
    }
}
