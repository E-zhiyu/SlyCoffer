package com.sly.coffer.ui.pages.tag;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.entities.composite.union.TagUnionModel;

public class TagInputViewModel extends ViewModel {
    private final MutableLiveData<TagUnionModel> tagModelLiveData = new MutableLiveData<>();    //缓存的数据

    public MutableLiveData<TagUnionModel> getTagModelLiveData() {
        return tagModelLiveData;
    }

    @Nullable
    public TagUnionModel getTagModel() {
        return tagModelLiveData.getValue();
    }

    public void setTagModel(TagUnionModel model) {
        tagModelLiveData.setValue(model);
    }
}
