package com.sly.coffer.ui.others.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.data.save.db.services.AccountService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class AccountFilterViewModel extends ViewModel {
    private LocalDate start, end;                                       //起止日期（闭区间）
    private final Set<Integer> filterTypeSet = new HashSet<>();         //种类白名单集合
    private final Set<Long> filterTagSet = new HashSet<>();             //标签 ID 白名单集合
    private boolean includeNoTag = false;                               //是否包含无标签的流水记录
    private final BehaviorProcessor<Boolean> filterUpdateProcessor =
            BehaviorProcessor.createDefault(true);
    private final BehaviorProcessor<String> searchKeywordProcessor =
            BehaviorProcessor.createDefault("");    //搜索关键词处理器
    private final MutableLiveData<Void> filterUpdatedLiveData = new MutableLiveData<>();    //提醒宿主更新 UI 的 LiveData

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public Set<Integer> getFilterTypeSet() {
        return filterTypeSet;
    }

    public Set<Long> getFilterTagSet() {
        return filterTagSet;
    }

    public void setIncludeNoTag(boolean includeNoTag) {
        this.includeNoTag = includeNoTag;
    }

    public boolean isIncludeNoTag() {
        return includeNoTag;
    }

    public MutableLiveData<Void> getFilterUpdatedLiveData() {
        return filterUpdatedLiveData;
    }

    /**
     * 加载流水记录列表数据
     *
     * @param db 数据库实例
     * @return 支持响应式更新的流水记录列表数据
     */
    public Flowable<List<AccountUiModel>> loadAccountListDataFlowable(BookkeepingDb db) {
        return Flowable.combineLatest(
                        searchKeywordProcessor.debounce(50, TimeUnit.MILLISECONDS),
                        filterUpdateProcessor.debounce(50, TimeUnit.MILLISECONDS),
                        (keyword, b) -> keyword
                )
                .switchMap(keyword -> AccountService.getAccountIdWithFilterFlowable(
                        filterTagSet,
                        filterTypeSet,
                        start,
                        end,
                        includeNoTag,
                        keyword,
                        db
                ))
                .switchMap(idList -> AccountService.getRunningAccountFlowableById(idList, db));
    }

    /**
     * 执行一次搜索
     *
     * @param keyword 搜索关键词
     */
    public void executeSearch(String keyword) {
        searchKeywordProcessor.onNext(keyword);
        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 提示过滤条件已变更
     */
    public void notifyFilterUpdated() {
        filterUpdateProcessor.onNext(true);
        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 判断是否有过滤条件
     *
     * @return 是否有过滤条件
     */
    public boolean hasFilter() {
        String searchText = searchKeywordProcessor.getValue();
        return start != null ||
                end != null ||
                !filterTypeSet.isEmpty() ||
                !filterTagSet.isEmpty() ||
                includeNoTag ||
                (searchText != null && !searchText.isEmpty());
    }

    /**
     * 清空过滤条件并刷新数据
     */
    public void clearFilter() {
        start = null;
        end = null;
        filterTagSet.clear();
        filterTypeSet.clear();
        includeNoTag = false;
        searchKeywordProcessor.onNext("");

        filterUpdatedLiveData.setValue(null);
    }
}
