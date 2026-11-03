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
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.ui.pages.budget.BudgetListActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LowBudgetResetReceiver extends BroadcastReceiver {
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (!BroadcastActions.ACTION_RESET_LOW_BUDGET.name().equals(intent.getAction())) {
            return;
        }

        //获取需要重置的预算编号
        if (intent.getExtras() == null) return;
        long[] budgetIds = intent.getExtras().getLongArray(KeyStrings.BUDGET_ID.v());
        if (budgetIds == null) return;
        List<Long> budgetIdList = Arrays.stream(budgetIds)
                .boxed()
                .collect(Collectors.toList());

        final PendingResult syncResult = goAsync();
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        disposable.add(db.budgetDao().resetBudgetByIdCompletable(budgetIdList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            syncResult.finish();
                            disposable.dispose();

                            if (!budgetIdList.isEmpty()) {
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
                                        "已重置%d条预算，点击查看详情",
                                        budgetIdList.size()
                                );
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ChannelInfo.BUDGET_BALANCE.getId())
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("预算重置")
                                        .setContentText(content)
                                        .setContentIntent(clickPendingIntent)
                                        .setAutoCancel(true);
                                NotificationHelper.sendNotification(NotificationID.BUDGET_RESET.ordinal(), builder, context);
                            }
                        },
                        e -> {
                            syncResult.finish();
                            disposable.dispose();
                            Log.e(LogTags.LOW_BUDGET_RESET_RECEIVER.n(), "预算重置出错\n" + e.getMessage());

                            //发送通知
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ChannelInfo.BUDGET_BALANCE.getId())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("预算重置")
                                    .setContentText("预算重置出错")
                                    .setAutoCancel(true);
                            NotificationHelper.sendNotification(NotificationID.BUDGET_RESET.ordinal(), builder, context);
                        }
                )
        );
    }
}
