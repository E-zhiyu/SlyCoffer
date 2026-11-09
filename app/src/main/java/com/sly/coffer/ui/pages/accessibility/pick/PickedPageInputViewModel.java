package com.sly.coffer.ui.pages.accessibility.pick;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.PickedPageEntity;

public class PickedPageInputViewModel extends ViewModel {
    private final MutableLiveData<PickedPageEntity> pickedPageLiveData = new MutableLiveData<>();

    public MutableLiveData<PickedPageEntity> getPickedPageLiveData() {
        return pickedPageLiveData;
    }

    @Nullable
    public PickedPageEntity getPickedPage() {
        return pickedPageLiveData.getValue();
    }

    public void setPickedPage(PickedPageEntity page) {
        pickedPageLiveData.setValue(page);
    }
}
