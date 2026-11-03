package com.sly.coffer.automation.broadcast;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.time.AlarmHelper;
import com.sly.coffer.ui.pages.budget.BudgetListActivity;
import com.sly.coffer.ui.pages.budget.ResetFrequency;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AutoBudgetResetReceiver extends BroadcastReceiver {
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LogTags.AUTO_BUDGET_RESET_RECEIVER.n(), "预算重置闹钟已触发");

        final PendingResult syncResult = goAsync();
        AtomicInteger budgetCount = new AtomicInteger(0);
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        disposable.add(db.budgetDao().getAllBudgetSingle()
                .flatMapCompletable(budgetList -> {
                    List<Long> resetBudgetIdList = getResetBudgetIdList(budgetList);
                    budgetCount.set(resetBudgetIdList.size());
                    return db.budgetDao().resetBudgetByIdCompletable(resetBudgetIdList);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                            syncResult.finish();
                            disposable.dispose();

                            if (budgetCount.get() > 0) {
                                //生成通知点击的 PendingIntent
                                Intent clickIntent = new Intent(context, BudgetListActivity.class);
                                PendingIntent clickPendingIntent = TaskStackBuilder.create(context)
                                        .addNextIntentWithParentStack(clickIntent)
                                        .getPendingIntent(
                                                PendingRequestCode.BUDGET_LOW_BALANCE.ordinal(),
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                        );

                                //发送通知
                                String content = String.format(
                                        Locale.getDefault(),
                                        "已自动重置%d条预算，点击查看详情",
                                        budgetCount.get()
                                );
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ChannelInfo.BUDGET_BALANCE.getId())
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("预算自动重置")
                                        .setContentText(content)
                                        .setContentIntent(clickPendingIntent)
                                        .setAutoCancel(true);
                                NotificationHelper.sendNotification(NotificationID.BUDGET_RESET.ordinal(), builder, context);
                            }
                        },
                        e -> {
                            syncResult.finish();
                            disposable.dispose();
                            Log.e(LogTags.AUTO_BUDGET_RESET_RECEIVER.n(), "预算重置出错\n" + e.getMessage());
                        }
                )
        );

        AlarmHelper.setBudgetCheckAlarm(context);
    }

    /**
     * 获取需要重置的预算的编号
     *
     * @param budgetList 原始预算数据
     * @return 需要重置的预算的编号列表
     */
    @NonNull
    private List<Long> getResetBudgetIdList(@NonNull List<BudgetEntity> budgetList) {
        List<Long> result = new ArrayList<>();

        ResetFrequency[] frequencies = ResetFrequency.values();
        LocalDate today = LocalDate.now();
        for (BudgetEntity budget : budgetList) {
            long id = budget.getBudgetId();
            ResetFrequency frequency = frequencies[budget.getResetFrequency()];
            LocalDate startDate = budget.getStartDate();
            long dateDifference = ChronoUnit.DAYS.between(today, startDate);

            switch (frequency) {
                case EVERY_DAY:
                    result.add(id);
                    break;
                case EVERY_WEEK:
                    if (dateDifference >= 7) {
                        result.add(id);
                    }
                    break;
                case EVERY_MONTH:
                    if (today.getDayOfMonth() == 1) {
                        result.add(id);
                    }
                    break;
            }
        }

        return result;
    }
}
