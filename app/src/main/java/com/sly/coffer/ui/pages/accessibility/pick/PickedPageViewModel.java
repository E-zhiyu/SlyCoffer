package com.sly.coffer.ui.pages.accessibility.pick;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.services.AccessibilityRuleService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PickedPageViewModel extends ViewModel {
    private final MutableLiveData<Void> filterUpdatedLiveData = new MutableLiveData<>();    //提醒宿主更新 UI 的 LiveData
    private final BehaviorProcessor<String> searchKeywordProcessor =
            BehaviorProcessor.createDefault("");    //搜索关键词处理器

    public MutableLiveData<Void> getFilterUpdatedLiveData() {
        return filterUpdatedLiveData;
    }

    /**
     * 获取角色数据
     *
     * @param db 数据库实例
     * @return 角色数据列表，包含分隔符
     */
    public Flowable<List<PickedPageEntity>> getPickedPageFlowable(BookkeepingDb db) {
        return searchKeywordProcessor
                .debounce(50, TimeUnit.MILLISECONDS)
                .switchMap(
                        keyword -> AccessibilityRuleService.getAllPickedPage(db, keyword)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                );
    }

    /**
     * 执行搜索
     *
     * @param keyword 搜索关键词
     */
    public void executeSearch(String keyword) {
        searchKeywordProcessor.onNext(keyword);
        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 清空过滤条件
     */
    public void clearFilter() {
        searchKeywordProcessor.onNext("");

        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 判断是否没有过滤条件
     *
     * @return 是否没有过滤条件
     */
    public boolean isNoFilter() {
        String searchText = searchKeywordProcessor.getValue();
        return searchText == null || searchText.isEmpty();
    }
}
