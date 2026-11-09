package com.sly.coffer.ui.pages.notification.rule;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleUnionModel;

public class NotiRuleInputViewModel extends ViewModel {
    private final MutableLiveData<NotificationRuleUnionModel> ruleModelLiveData = new MutableLiveData<>();  //缓存的数据

    public MutableLiveData<NotificationRuleUnionModel> getRuleModelLiveData() {
        return ruleModelLiveData;
    }

    @Nullable
    public NotificationRuleUnionModel getRuleModel() {
        return ruleModelLiveData.getValue();
    }

    public void setRuleModel(NotificationRuleUnionModel model) {
        ruleModelLiveData.setValue(model);
    }
}
