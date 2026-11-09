package com.sly.coffer.ui.pages.budget;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.BudgetUnionModel;

public class BudgetInputViewModel extends ViewModel {
    private final MutableLiveData<BudgetUnionModel> budgetModelLiveData = new MutableLiveData<>();  //缓存的数据

    public MutableLiveData<BudgetUnionModel> getBudgetModelLiveData() {
        return budgetModelLiveData;
    }

    @Nullable
    public BudgetUnionModel getBudgetModel() {
        return budgetModelLiveData.getValue();
    }

    public void setBudgetModel(BudgetUnionModel model) {
        budgetModelLiveData.setValue(model);
    }
}
