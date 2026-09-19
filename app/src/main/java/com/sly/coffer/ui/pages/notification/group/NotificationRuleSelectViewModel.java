package com.sly.coffer.ui.pages.notification.group;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NotificationRuleSelectViewModel extends ViewModel {
    private final MutableLiveData<List<Long>> ruleIdLiveData = new MutableLiveData<>();

    public MutableLiveData<List<Long>> getRuleIdLiveData() {
        return ruleIdLiveData;
    }

    public void updateRuleLiveData(List<Long> ruleIdSet) {
        ruleIdLiveData.postValue(ruleIdSet);
    }
}
