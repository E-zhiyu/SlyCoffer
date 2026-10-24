package com.sly.coffer.ui.pages.accessibility.rule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.PickedPageEntity;

public class AccessibilityRuleInputViewModel extends ViewModel {
    private final MutableLiveData<PickedPageEntity> pickResult = new MutableLiveData<>(null); //视图拾取结果

    public LiveData<PickedPageEntity> getPickedPage() {
        return pickResult;
    }

    public void setPickResult(PickedPageEntity pickedPage) {
        this.pickResult.postValue(pickedPage);
    }
}
