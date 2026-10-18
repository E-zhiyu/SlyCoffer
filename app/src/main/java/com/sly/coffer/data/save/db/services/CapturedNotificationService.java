package com.sly.coffer.data.save.db.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.NotificationRuleDao;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.CapturedNotificationUiModel;
import com.sly.coffer.helpers.AppListHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Flowable;

public class CapturedNotificationService {
    /**
     * 获取所有捕获的通知
     *
     * @param context 上下文
     * @param keyword 搜索关键词
     * @return 捕获的通知列表，包含日期分隔符
     */
    public static Flowable<List<CapturedNotificationUiModel>> getAllCapturedNotification(@NonNull Context context, String keyword) {
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        NotificationRuleDao notificationDao = db.notificationRuleDao();
        String safeKeyword = "";

        if (keyword != null && !keyword.trim().isEmpty()) {
            safeKeyword = keyword.replace("/", "//")
                    .replace("%", "/%")
                    .replace("_", "/_");
        }

        int isSearchFilter = !safeKeyword.isEmpty() ? 1 : 0;
        return notificationDao.getAllCapturedNotificationFlowable(safeKeyword, isSearchFilter)
                .map(rawList -> {
                    List<CapturedNotificationUiModel> resultList = new ArrayList<>();

                    //判空
                    if (rawList.isEmpty()) {
                        return resultList;
                    }

                    //通过发送者进行分组
                    Map<String, List<CapturedNotificationEntity>> groupedMap = rawList.stream()
                            .collect(Collectors.groupingBy(
                                    CapturedNotificationEntity::getPackageName,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    //循环插入分隔符和 Item
                    for (Map.Entry<String, List<CapturedNotificationEntity>> entry : groupedMap.entrySet()) {
                        String separatorText = AppListHelper.getAppNameByPackageName(entry.getKey(), context);
                        resultList.add(new CapturedNotificationUiModel.Separator(separatorText));

                        List<CapturedNotificationUiModel.Item> itemList = entry.getValue().stream()
                                .map(CapturedNotificationUiModel.Item::new)
                                .collect(Collectors.toList());
                        resultList.addAll(itemList);
                    }

                    return resultList;
                });
    }
}
