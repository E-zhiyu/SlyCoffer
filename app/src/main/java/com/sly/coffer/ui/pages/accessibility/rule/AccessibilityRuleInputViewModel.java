package com.sly.coffer.ui.pages.accessibility.rule;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccessibilityRuleUnionModel;

public class AccessibilityRuleInputViewModel extends ViewModel {
    private final MutableLiveData<PickedPageEntity> pickResult = new MutableLiveData<>(null); //视图拾取结果
    private final MutableLiveData<AccessibilityRuleUnionModel> ruleModelLiveData = new MutableLiveData<>();  //缓存的数据

    public LiveData<PickedPageEntity> getPickedPage() {
        return pickResult;
    }

    public void setPickResult(PickedPageEntity pickedPage) {
        this.pickResult.postValue(pickedPage);
    }

    public MutableLiveData<AccessibilityRuleUnionModel> getRuleModelLiveData() {
        return ruleModelLiveData;
    }

    @Nullable
    public AccessibilityRuleUnionModel getRuleModel() {
        return ruleModelLiveData.getValue();
    }

    public void setRuleModel(AccessibilityRuleUnionModel rule) {
        ruleModelLiveData.setValue(rule);
    }
}
