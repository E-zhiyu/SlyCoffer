package com.sly.coffer.automation.services;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.sly.coffer.automation.broadcast.BroadcastActions;
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
    private final BroadcastReceiver SHUT_DOWN_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (BroadcastActions.ACTION_SHUT_DOWN_PAGE_PICK.toString().equals(action)) {
                Log.i(LogTags.PICK_ACCESSIBILITY_SERVICE.n(), "接收到关闭广播");
                disableSelf();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //注册监听器
        IntentFilter filter = new IntentFilter(BroadcastActions.ACTION_SHUT_DOWN_PAGE_PICK.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(SHUT_DOWN_RECEIVER, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(this, SHUT_DOWN_RECEIVER, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        Log.d(LogTags.PICK_ACCESSIBILITY_SERVICE.n(), "注册监听器");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            //判断功能开关是否开启
            if (!AutoBookKeepingPreference.getPagePickStat(this)) {
                disableSelf();
                return;
            }

            //解析界面内容
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
                    String[] parts = activityName.split("\\.");
                    String remark = parts.length > 1 ?
                            parts[parts.length - 1] :
                            "界面 · " + time.format(CustomDateTimeFormatter.DATE_TIME);
                    PickedPageEntity pickedPage = new PickedPageEntity(
                            remark,
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
