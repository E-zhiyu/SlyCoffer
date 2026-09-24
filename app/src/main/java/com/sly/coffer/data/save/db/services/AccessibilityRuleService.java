package com.sly.coffer.data.save.db.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.AccessibilityRuleDao;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.PickedPageGroupUiModel;
import com.sly.coffer.data.save.db.entities.composite.ui.PickedPageListUiModel;
import com.sly.coffer.helpers.AppListHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class AccessibilityRuleService {
    /**
     * 添加新无障碍规则
     *
     * @param rule             新规则
     * @param transfer         新规则对应的转账账户数据
     * @param keywordGroupList 关键词组合列表
     * @param tagIdList        新规则的标签编号数据
     * @param db               数据库实例
     * @return 是否完成
     */
    public static Completable addNewAccessibilityRule(
            AccessibilityRuleEntity rule,
            AccessibilityRuleTransferEntity transfer,
            List<AccessibilityRuleKeywordGroupEntity> keywordGroupList,
            List<Long> tagIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.accessibilityRuleDao().addAccessibilityRule(rule, transfer, keywordGroupList, tagIdList);
            return Completable.complete();
        });
    }

    /**
     * 修改无障碍规则
     *
     * @param rule             修改后的规则
     * @param transfer         转账账户数据
     * @param keywordGroupList 关键词组合列表
     * @param tagIdList        标签编号数据
     * @param db               数据库实例
     * @return 是否完成
     */
    public static Completable modifyAccessibilityRule(
            AccessibilityRuleEntity rule,
            AccessibilityRuleTransferEntity transfer,
            List<AccessibilityRuleKeywordGroupEntity> keywordGroupList,
            List<Long> tagIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.accessibilityRuleDao().modifyAccessibilityRule(rule, transfer, keywordGroupList, tagIdList);
            return Completable.complete();
        });
    }

    /**
     * 添加拾取的视图
     *
     * @param pickedPageEntity 拾取的视图
     * @param db               数据库实例
     * @return 分配的编号
     */
    public static Single<Long> addPickedPage(PickedPageEntity pickedPageEntity, BookkeepingDb db) {
        return Single.defer(() -> {
            long id = db.accessibilityRuleDao().addPickedPage(pickedPageEntity);
            return Single.just(id);
        });
    }

    /**
     * 获取所有拾取的视图
     *
     * @param db      数据库实例
     * @param keyword 搜索关键词
     * @return 拾取的视图列表，包含应用分隔符
     */
    public static Flowable<List<PickedPageListUiModel>> getAllPickedPage(@NonNull BookkeepingDb db, String keyword) {
        AccessibilityRuleDao dao = db.accessibilityRuleDao();
        String safeKeyword = "";

        if (keyword != null && !keyword.trim().isEmpty()) {
            safeKeyword = keyword.replace("/", "//")
                    .replace("%", "/%")
                    .replace("_", "/_");
        }

        int isSearchFilter = !safeKeyword.isEmpty() ? 1 : 0;
        return dao.getAllPickedPageFlowable(safeKeyword, isSearchFilter)
                .map(rawList -> {
                    List<PickedPageListUiModel> resultList = new ArrayList<>();

                    //判空
                    if (rawList.isEmpty()) {
                        return resultList;
                    }

                    //通过关系远近进行分组
                    Map<String, List<PickedPageEntity>> groupedMap = rawList.stream()
                            .collect(Collectors.groupingBy(
                                    PickedPageEntity::getPackageName,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    //循环插入分隔符和 Item
                    for (Map.Entry<String, List<PickedPageEntity>> entry : groupedMap.entrySet()) {
                        String separatorText = entry.getKey();
                        resultList.add(new PickedPageListUiModel.Separator(separatorText));

                        List<PickedPageListUiModel.Item> itemList = entry.getValue().stream()
                                .map(PickedPageListUiModel.Item::new)
                                .collect(Collectors.toList());
                        resultList.addAll(itemList);
                    }

                    return resultList;
                });
    }

    /**
     * 获取已根据应用分好组的已拾取的视图
     *
     * @param context 上下文
     * @return 分好组的视图列表，支持响应式更新
     */
    public static Flowable<List<PickedPageGroupUiModel>> getGroupedPickedPage(Context context) {
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        return db.accessibilityRuleDao().getAllPickedPageFlowable()
                .flatMap(pickedPageList -> {
                    //分组
                    Map<String, List<PickedPageEntity>> groupedMap = pickedPageList.stream()
                            .collect(Collectors.groupingBy(
                                    PickedPageEntity::getPackageName,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    //生成带有分隔符的列表
                    List<PickedPageGroupUiModel> resultList = new ArrayList<>();
                    for (Map.Entry<String, List<PickedPageEntity>> entry : groupedMap.entrySet()) {
                        String appName = AppListHelper.getAppNameByPackageName(entry.getKey(), context);
                        resultList.add(new PickedPageGroupUiModel.Separator(appName));

                        resultList.add(new PickedPageGroupUiModel.Item(entry.getValue()));
                    }

                    return Flowable.just(resultList);
                });
    }
}
