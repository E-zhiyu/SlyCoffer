package com.sly.coffer.ui.pages.common.app_list;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.classes.AppInfo;
import com.sly.coffer.helpers.AppListHelper;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class AppListViewModel extends ViewModel {
    private final MutableLiveData<Void> filterUpdatedLiveData = new MutableLiveData<>();    //提醒宿主更新 UI 的 LiveData
    private final BehaviorProcessor<String> searchKeywordProcessor =
            BehaviorProcessor.createDefault("");    //搜索关键词处理器
    private final BehaviorProcessor<Boolean> sysAppVisibilityProcessor =
            BehaviorProcessor.createDefault(false); //搜索关键词处理器

    public boolean isSysAppVisible() {
        Boolean isVisible = sysAppVisibilityProcessor.getValue();
        return isVisible != null && isVisible;
    }

    public MutableLiveData<Void> getFilterUpdatedLiveData() {
        return filterUpdatedLiveData;
    }

    /**
     * 获取应用列表
     *
     * @param context 上下文
     * @return 应用数据，支持响应式更新
     */
    public Flowable<List<AppInfo>> getAppListFlowable(Context context) {
        return Flowable.combineLatest(
                searchKeywordProcessor.debounce(50, TimeUnit.MILLISECONDS),
                sysAppVisibilityProcessor.debounce(50, TimeUnit.MILLISECONDS),
                (keyword, sysAppVisible) -> {
                    List<AppInfo> appListCache = AppListHelper.getInstalledApps(sysAppVisible, context);

                    String lowerKeyword = keyword.toLowerCase();
                    return appListCache.stream()
                            .filter(app ->
                                    keyword.isEmpty() ||
                                            app.getAppName().toLowerCase().contains(lowerKeyword) ||
                                            app.getPackageName().toLowerCase().contains(lowerKeyword)
                            )
                            .sorted(Comparator.comparing(AppInfo::getAppName))
                            .collect(Collectors.toList());
                }
        );
    }

    /**
     * 执行一次搜索
     *
     * @param keyword 搜索的关键词
     */
    public void executeSearch(String keyword) {
        searchKeywordProcessor.onNext(keyword);

        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 切换系统应用可见性
     */
    public void toggleSysAppVisibility(boolean isVisible) {
        sysAppVisibilityProcessor.onNext(isVisible);

        filterUpdatedLiveData.setValue(null);
    }

    /**
     * 清除过滤条件
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
        return (searchText == null || searchText.isEmpty());
    }
}
