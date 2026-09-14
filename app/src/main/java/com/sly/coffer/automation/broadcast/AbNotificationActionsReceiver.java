package com.sly.coffer.automation.broadcast;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.settings.NotificationCancelBehaviour;
import com.sly.coffer.auxiliary.enums.settings.NotificationClickBehaviour;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AbNotificationActionsReceiver extends BroadcastReceiver {
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        Bundle bundle = intent.getExtras();                                         //流水记录数据包
        String ruleName = intent.getStringExtra(KeyStrings.NOTIFICATION_RULE_NAME.v()); //规则名称
        int notificationID = intent.getIntExtra(KeyStrings.NOTIFICATION_ID.v(), -1);
        String action = intent.getAction();
        if (action == null || bundle == null || notificationID == -1) {
            return;
        }

        //设置为异步状态
        PendingResult syncResult = goAsync();

        //根据action执行接下来的逻辑
        if (action.equals(BroadcastActions.ACTION_INPUT_REMARK.toString())) {
            Bundle inputResults = RemoteInput.getResultsFromIntent(intent);
            if (inputResults == null) {
                return;
            }

            String remark = inputResults.getString(KeyStrings.RUNNING_REMARK.v());
            if (remark == null) {
                return;
            }

            bundle.putString(KeyStrings.RUNNING_REMARK.v(), remark);    //覆盖数据包中的备注
            saveAccountData(syncResult, bundle, ruleName, notificationID, context);
        } else if (action.equals(BroadcastActions.ACTION_KEEP.toString())) {
            saveAccountData(syncResult, bundle, ruleName, notificationID, context);
        } else if (action.equals(BroadcastActions.ACTION_DELETE.toString())) {
            sendAbadonNotification(context, notificationID, ruleName);
            disposable.dispose();
            syncResult.finish();
        } else if (action.equals(BroadcastActions.ACTION_NOTIFICATION_CANCELED.toString())) {
            int behaviour = AutoBookKeepingPreference.getNotificationCancelBehaviour(context);
            if (behaviour == NotificationCancelBehaviour.KEEP.getItemId()) {
                saveAccountData(syncResult, bundle, ruleName, notificationID, context);
            } else {
                sendAbadonNotification(context, notificationID, ruleName);
                disposable.dispose();
                syncResult.finish();
            }
        } else if (action.equals(BroadcastActions.ACTION_NOTIFICATION_CLICKED.toString())) {
            int behaviour = AutoBookKeepingPreference.getNotificationClickBehaviour(context);
            if (behaviour == NotificationClickBehaviour.KEEP.getItemId()) {
                saveAccountData(syncResult, bundle, ruleName, notificationID, context);
            } else if (behaviour == NotificationClickBehaviour.ABADON.getItemId()) {
                sendAbadonNotification(context, notificationID, ruleName);
                disposable.dispose();
                syncResult.finish();
            }
        }
    }

    /**
     * 获取能够跳转到流水记录输入界面的 PendingInten
     *
     * @param accountId 新添加的流水记录的编号
     * @param context   上下文
     * @return 能够跳转到流水记录输入界面的 PendingIntent
     */
    private PendingIntent getAccountDetailPendingIntent(long accountId, Context context) {
        //生成数据包
        Bundle bundle = new Bundle();
        bundle.putLong(KeyStrings.RUNNING_ID.v(), accountId);

        //生成 Intent
        Intent skip2AccountInput = new Intent(context, RunningAccountInputActivity.class);
        skip2AccountInput.putExtras(bundle);

        //生成 PendingIntent
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(skip2AccountInput)
                .getPendingIntent(
                        PendingRequestCode.SKIP_TO_ACCOUNT_INPUT.ordinal(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
    }

    /**
     * 将流水记录数据保存到数据库中
     *
     * @param syncResult     保持异步状态的对象
     * @param bundle         包含流水记录数据的数据包
     * @param ruleName       触发的通知规则的名称
     * @param notificationID 通知唯一标识符，用于覆盖通知监听服务中发送的通知
     * @param context        上下文
     */
    private void saveAccountData(PendingResult syncResult, @NonNull Bundle bundle, String ruleName, int notificationID, Context context) {
        //解包数据包
        long dateTimeMillis = bundle.getLong(KeyStrings.RUNNING_DATETIME.v());
        int type = bundle.getInt(KeyStrings.RUNNING_TYPE.v());
        String remark = bundle.getString(KeyStrings.RUNNING_REMARK.v());
        double amount = bundle.getDouble(KeyStrings.RUNNING_AMOUNT.v());
        long[] tagIds = bundle.getLongArray(KeyStrings.TAG_ID.v());
        List<Long> tagIdList;
        if (tagIds != null) {
            tagIdList = Arrays.stream(tagIds)
                    .boxed()
                    .collect(Collectors.toList());
        } else {
            tagIdList = null;
        }
        String exportAccount = bundle.getString(KeyStrings.RUNNING_EXPORT_ACCOUNT.v());
        String importAccount = bundle.getString(KeyStrings.RUNNING_IMPORT_ACCOUNT.v());

        //实例化实体类
        int autoBookkeepingType = bundle.getInt(KeyStrings.AUTO_BOOKKEEPING_TYPE.v(), 0);
        LocalDateTime dateTime = DateTimeConverter.toLocalDateTime(dateTimeMillis);
        AccountEntity account = new AccountEntity(amount, remark, type, dateTime, autoBookkeepingType);
        AccountTransferEntity transfer = new AccountTransferEntity(exportAccount, importAccount);

        //保存数据
        disposable.add(AccountService.addNewAccount(account, transfer, null, tagIdList, context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        accountId -> {
                            //创建通知构建器
                            String content = String.format(
                                    Locale.getDefault(),
                                    "已保留由“%s”触发的记录，点击查看详情。",
                                    ruleName
                            );
                            String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
                            PendingIntent accountModifyPendingIntent = getAccountDetailPendingIntent(accountId, context);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("自动记账")
                                    .setContentText(content)
                                    .setContentIntent(accountModifyPendingIntent)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setCategory(NotificationCompat.CATEGORY_CALL)
                                    .setTimeoutAfter(3000)
                                    .setAutoCancel(true);

                            //发送通知
                            NotificationHelper.sendNotification(
                                    notificationID,
                                    builder,
                                    context
                            );

                            disposable.dispose();
                            syncResult.finish();
                        },
                        e -> {
                            //创建通知构建器
                            String content = String.format(
                                    Locale.getDefault(),
                                    "保存由“%s”触发的记录时出错。",
                                    ruleName
                            );
                            String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("自动记账")
                                    .setContentText(content)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setCategory(NotificationCompat.CATEGORY_CALL)
                                    .setTimeoutAfter(3000)
                                    .setAutoCancel(true);

                            //发送通知
                            NotificationHelper.sendNotification(
                                    notificationID,
                                    builder,
                                    context
                            );

                            disposable.dispose();
                            syncResult.finish();
                        }
                )
        );
    }

    /**
     * 舍弃操作
     *
     * @param context        上下文
     * @param notificationID 触发该接收器的通知的ID
     * @param ruleName       触发自动记账的规则名称
     */
    private void sendAbadonNotification(Context context, int notificationID, String ruleName) {
        //创建通知构建器
        String content = String.format(Locale.getDefault(), "已舍弃由“%s”触发的记录。", ruleName);
        String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("自动记账")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setTimeoutAfter(3000)
                .setAutoCancel(true);

        //发送通知
        NotificationHelper.sendNotification(
                notificationID,
                builder,
                context
        );
    }
}
