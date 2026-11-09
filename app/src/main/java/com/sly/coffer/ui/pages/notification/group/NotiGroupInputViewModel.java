package com.sly.coffer.ui.pages.notification.group;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleAndGroupUnionModel;

public class NotiGroupInputViewModel extends ViewModel {
    private final MutableLiveData<NotificationRuleAndGroupUnionModel> ruleGroupModelLiveData = new MutableLiveData<>(); //缓存的数据

    public MutableLiveData<NotificationRuleAndGroupUnionModel> getRuleGroupModelLiveData() {
        return ruleGroupModelLiveData;
    }

    @Nullable
    public NotificationRuleAndGroupUnionModel getRuleGroupModel() {
        return ruleGroupModelLiveData.getValue();
    }

    public void setRuleGroupModel(NotificationRuleAndGroupUnionModel model) {
        ruleGroupModelLiveData.setValue(model);
    }
}
