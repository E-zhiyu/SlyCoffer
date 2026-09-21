package com.sly.coffer.automation.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.services.AccessibilityRuleService;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.helpers.AppListHelper;

import java.time.LocalDateTime;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@SuppressLint("AccessibilityPolicy")
public class PickAccessibilityService extends AccessibilityService {
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!AutoBookKeepingPreference.getPagePickStat(this)) return;

            String packageName = event.getPackageName().toString();
            CharSequence className = event.getClassName();
            if (className != null && !packageName.equals(getPackageName())) {
                Log.d(
                        LogTags.PICK_ACCESSIBILITY_SERVICE.n(),
                        "className : " + className +
                                ",\npackageName : " + packageName
                );

                //组合成完整的组件名
                ComponentName componentName = new ComponentName(
                        packageName,
                        className.toString()
                );

                //判断是否为活动名
                if (AppListHelper.isActivity(componentName, this)) {
                    String activityName = className.toString();
                    LocalDateTime time = LocalDateTime.now();
                    PickedPageEntity pickedPage = new PickedPageEntity(
                            "界面 · " + time.format(CustomDateTimeFormatter.DATE_TIME),
                            packageName,
                            activityName,
                            time
                    );

                    //保存视图信息
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(AccessibilityRuleService.addPickedPage(pickedPage, db)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    id -> {
                                        if (id >= 0) {
                                            Log.d(LogTags.PICK_ACCESSIBILITY_SERVICE.n(), "已保存界面, id : " + id);
                                        } else {
                                            Log.e(LogTags.PICK_ACCESSIBILITY_SERVICE.n(), "界面保存失败");
                                        }
                                    },
                                    e -> Log.e(LogTags.PICK_ACCESSIBILITY_SERVICE.n(), "界面保存失败")
                            )
                    );
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    @Override
    public void onInterrupt() {
    }
}
