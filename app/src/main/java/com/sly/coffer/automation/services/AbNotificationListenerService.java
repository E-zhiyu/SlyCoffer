package com.sly.coffer.automation.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;

import com.sly.coffer.R;
import com.sly.coffer.automation.broadcast.AbNotificationActionsReceiver;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.automation.broadcast.BroadcastActions;
import com.sly.coffer.auxiliary.enums.settings.NotificationClickBehaviour;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.NotificationRuleWithDetailModel;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.helpers.AppListHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;
import com.sly.coffer.ui.pages.notification.rule.NotificationRuleListActivity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AbNotificationListenerService extends NotificationListenerService {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final Map<RuleKey, List<NotificationRuleWithDetailModel>> ruleMap = new HashMap<>(); //解析规则哈希表
    private String lastPackageName = "";                                //上一次接收通知的包名
    private String lastTitle = "";                                      //上一次通知的标题
    private long lastReceiveEpochMilli = 0;                             //上一次接收消息的时间（毫秒）

    private static class RuleKey {
        private final String title;                                     //通知标题
        private final String packageName;                               //通知发送者包名

        public RuleKey(String packageName, String title) {
            this.packageName = packageName;
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            RuleKey key = (RuleKey) o;
            return Objects.equals(title, key.title) &&
                    Objects.equals(packageName, key.packageName);       //只要包名和通知标题匹配则判定为相同对象
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, packageName);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "服务已启动");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "服务已创建");

        //启动时则加载规则
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.notificationRuleDao().getEnabledNotificationRuleFlowable()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        modelList -> {
                            ruleMap.clear();
                            Map<RuleKey, List<NotificationRuleWithDetailModel>> map = modelList.stream()
                                    .collect(Collectors.groupingBy(
                                            model -> {
                                                NotificationRuleEntity rule = model.getRule();
                                                return new RuleKey(rule.getPackageName(), rule.getTargetTitle());
                                            },
                                            HashMap::new,
                                            Collectors.toList()
                                    ));

                            ruleMap.putAll(map);
                        },
                        e -> {
                            ExceptionHelper.showExceptionDialog(this, e);
                            Log.e(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "通知监听服务获取通知规则失败");
                        }
                )
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "服务已关闭");
        disposable.clear();
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        //保存通知内容
        if (AutoBookKeepingPreference.getNotificationCapture(this)) {
            saveNotification(sbn);
        }

        //判断是否开启通知解析功能
        if (!AutoBookKeepingPreference.getSwitchStat(this)) {
            Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "通知自动记账未启用");
            return;
        }

        //获取通知数据
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");
        if (text == null || text.isEmpty() || title == null || title.isEmpty()) return;

        //同一应用发送太频繁直接不运行
        long currentEpochMilli = System.currentTimeMillis();
        long difference = currentEpochMilli - lastReceiveEpochMilli;        //求时间差
        if (difference <= 1000 && title.equals(lastTitle) && packageName.equals(lastPackageName)) {
            Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "同一应用发送通知过于频繁，不执行任何操作");
            return;
        }
        lastReceiveEpochMilli = currentEpochMilli;
        lastPackageName = packageName;
        lastTitle = title;

        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), String.format("通知发送者包名：%s", packageName));
        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), String.format("通知标题：%s", title));
        Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), String.format("通知内容：%s", text));

        //处理通知内容
        RuleKey key = new RuleKey(packageName, title);
        List<NotificationRuleWithDetailModel> ruleList = ruleMap.get(key);
        if (ruleList != null) {
            for (NotificationRuleWithDetailModel model : ruleList) {
                NotificationRuleEntity rule = model.getRule();
                String contentRegex = rule.getContentRegex();
                String name = rule.getName();
                long ruleId = rule.getRuleId();

                Matcher matcher;                                        //通知内容匹配器
                try {
                    Pattern pattern = Pattern.compile(contentRegex);
                    matcher = pattern.matcher(text);
                } catch (PatternSyntaxException e) {                    //处理无法编译为Matcher的情况
                    Log.e(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "正则表达式编译出错");
                    String err = String.format(
                            Locale.getDefault(),
                            "规则“%s”的正则表达式编译出错",
                            name
                    );
                    sendErrorNotification(err, ruleId);
                    continue;
                }

                if (matcher.find()) {
                    Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "成功匹配正则表达式");

                    //获取匹配到的金额数据
                    double amount;
                    try {
                        String captured = matcher.group(rule.getCaptureGroupPos());
                        amount = Double.parseDouble(Objects.requireNonNull(captured).replace(",", ""));
                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
                        String err = String.format(Locale.getDefault(), "“%s”无法提取金额数据", rule.getName());
                        sendErrorNotification(err, ruleId);
                        continue;
                    }
                    Log.i(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "流水数据生成成功");

                    //根据偏好设置决定直接入帐还是发送通知
                    if (!AutoBookKeepingPreference.getDirectDeposit(this)) {
                        sendConfirmNotification(amount, model);
                    } else {
                        saveInDbDirectly(amount, model);
                    }
                } else {
                    Log.d(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "正则表达式不匹配");
                }
            }
        }
    }

    /**
     * 将通知保存到数据库
     *
     * @param sbn 需要保存的通知
     */
    private void saveNotification(@NonNull StatusBarNotification sbn) {
        //获取通知数据
        String packageName = sbn.getPackageName();
        String appName = AppListHelper.getAppNameByPackageName(packageName, this);
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");
        if (text == null || text.isEmpty() || title == null || title.isEmpty()) return;

        //判断是否有数字
        Pattern numPattern = Pattern.compile("\\d");
        Matcher matcher = numPattern.matcher(text);
        if (!matcher.find()) return;

        //保存数据
        CapturedNotificationEntity notification = new CapturedNotificationEntity(title, text, packageName, appName, LocalDateTime.now());
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.notificationRuleDao().insertCapturedNotification(notification)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Log.i(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), "通知捕获成功"),
                        e -> Log.e(LogTags.AB_NOTIFICATION_LISTENER_SERVICE.n(), e.getMessage() == null ? "通知捕获失败" : e.getMessage())
                )
        );
    }

    /**
     * 获得流水记录数据
     *
     * @param amount 提取的金额数据
     * @param model  匹配到的通知规则详情数据
     * @return 流水数据包
     */
    @NonNull
    private Bundle getNewAccountData(
            double amount,
            @NonNull NotificationRuleWithDetailModel model
    ) {
        //获取规则数据
        NotificationRuleEntity rule = model.getRule();
        String ruleName = rule.getName();                   //规则名称
        int type = rule.getType();                          //流水种类枚举序数
        long[] tagIds = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .mapToLong(Long::longValue)
                .toArray();                                 //标签列表
        NotificationRuleTransferEntity transfer = model.getTransfer();  //转账账户数据

        //生成备注
        String remark = "通知记账 : " + ruleName;

        //生成流水记录数据包
        Bundle bundle = new Bundle();
        bundle.putLongArray(KeyStrings.TAG_ID.v(), tagIds);                         //标签 ID
        bundle.putLong(                                                             //日期和时间
                KeyStrings.RUNNING_DATETIME.v(),
                DateTimeConverter.fromLocalDateTime(LocalDateTime.now())
        );
        bundle.putInt(KeyStrings.RUNNING_TYPE.v(), type);                           //种类
        bundle.putDouble(KeyStrings.RUNNING_AMOUNT.v(), amount);                    //金额
        bundle.putString(KeyStrings.RUNNING_REMARK.v(), remark);                    //备注
        if (type == AccountType.TRANSFER.ordinal() && transfer != null) {
            String exportAccount = transfer.getExportAccount();
            String importAccount = transfer.getImportAccount();
            bundle.putString(KeyStrings.RUNNING_EXPORT_ACCOUNT.v(), exportAccount); //转出账户
            bundle.putString(KeyStrings.RUNNING_IMPORT_ACCOUNT.v(), importAccount); //转入账户
        }

        return bundle;
    }

    /**
     * 发送错误警告通知
     *
     * @param content 通知内容
     * @param ruleId  出错的规则编号
     */
    private void sendErrorNotification(String content, long ruleId) {
        //发送错误提示通知
        int notificationID = (int) (ruleId + System.currentTimeMillis() + NotificationID.AUTO_BOOKKEEPING_ERROR.ordinal());
        Intent skip2RuleManage = new Intent(this, NotificationRuleListActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                notificationID,
                skip2RuleManage,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        //实例化构建器
        String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this,
                channelID
        )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("通知记账出错")
                .setContentText(content)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        //发送通知
        NotificationHelper.sendNotification(
                notificationID,
                builder,
                this
        );
    }

    /**
     * 发送通知以提醒用户确认
     *
     * @param amount 提取的金额
     * @param model  触发自动记账的规则（包含抓张账户等其他数据）
     */
    private void sendConfirmNotification(double amount, @NonNull NotificationRuleWithDetailModel model) {
        //生成数据包
        NotificationRuleEntity rule = model.getRule();
        Bundle bundle = getNewAccountData(amount, model);

        //生成通知唯一标识符
        String ruleName = rule.getName();
        long ruleId = rule.getRuleId();
        int notificationId = (int) (ruleId + System.currentTimeMillis() + NotificationID.AUTO_BOOKKEEPING_CONFIRM.ordinal());

        //创建保留 Action
        NotificationCompat.Action keepAction = createAction(
                bundle,
                ruleName,
                BroadcastActions.ACTION_KEEP.toString(),
                "保留",
                notificationId,
                PendingRequestCode.ACCOUNT_KEEP.ordinal(),
                null
        );

        //创建备注输入 Action
        RemoteInput remarkRemoteInput = new RemoteInput.Builder(KeyStrings.RUNNING_REMARK.v())
                .setLabel("输入备注")
                .build();
        NotificationCompat.Action remarkInputAction = createAction(
                bundle,
                ruleName,
                BroadcastActions.ACTION_INPUT_REMARK.toString(),
                "备注并保留",
                notificationId,
                PendingRequestCode.ACCOUNT_INPUT_REMARK.ordinal(),
                remarkRemoteInput
        );

        //创建舍弃 Action
        NotificationCompat.Action deleteAction = createAction(
                bundle,
                ruleName,
                BroadcastActions.ACTION_DELETE.toString(),
                "舍弃",
                notificationId,
                PendingRequestCode.ACCOUNT_DELETE.ordinal(),
                null
        );

        //创建通知被取消的 PendingIntent
        Intent notificationCancelIntent = new Intent(this, AbNotificationActionsReceiver.class);
        notificationCancelIntent.setAction(BroadcastActions.ACTION_NOTIFICATION_CANCELED.toString());
        notificationCancelIntent.putExtra(KeyStrings.NOTIFICATION_ID.v(), notificationId);
        notificationCancelIntent.putExtras(bundle);                                         //发送流水记录数据包
        notificationCancelIntent.putExtra(KeyStrings.NOTIFICATION_RULE_NAME.v(), ruleName); //发送规则名称
        int pendingCancelId = notificationId * 10 + PendingRequestCode.AUTO_BOOKKEEPING_NOTIFICATION_DELETE.ordinal();
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                this,
                pendingCancelId,
                notificationCancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        //创建通知构建器
        String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
        String content = String.format(
                Locale.getDefault(),
                "“%s”产生了一条%s记录，金额为%.2f，请展开通知确认是否入账。",
                ruleName, AccountType.values()[rule.getType()].getTitle(), amount
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("通知记账确认")
                .setContentText(content)
                .setAutoCancel(true)
                .addAction(keepAction)                  //点击保留按钮
                .addAction(remarkInputAction)           //点击更改备注按钮
                .addAction(deleteAction)                //点击删除按钮
                .setDeleteIntent(deletePendingIntent);  //通知被划走

        //创建通知点击 PendingIntent
        int clickBehaviourCode = AutoBookKeepingPreference.getNotificationClickBehaviour(this);
        if (clickBehaviourCode != NotificationClickBehaviour.NONE.getItemId()) {
            Intent notificationClickIntent = new Intent(this, AbNotificationActionsReceiver.class);
            notificationClickIntent.setAction(BroadcastActions.ACTION_NOTIFICATION_CLICKED.toString());
            notificationClickIntent.putExtra(KeyStrings.NOTIFICATION_ID.v(), notificationId);
            notificationClickIntent.putExtras(bundle);                                      //发送流水记录数据
            notificationClickIntent.putExtra(KeyStrings.NOTIFICATION_RULE_NAME.v(), ruleName);  //发送规则名称
            int pendingClickedId = notificationId * 10 + PendingRequestCode.AUTO_BOOKKEEPING_NOTIFICATION_CLICK.ordinal();
            PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                    this,
                    pendingClickedId,
                    notificationClickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            //为通知构建器加上点击逻辑
            builder.setContentIntent(clickPendingIntent);
            builder.setAutoCancel(false);
        }

        //发送通知
        NotificationHelper.sendNotification(
                notificationId,
                builder,
                this
        );
    }

    /**
     * 创建用于处理自动记账流水记录的通知Action（即通知按钮）
     *
     * @param dataBundle  自动生成的账单的数据包
     * @param ruleName    触发自动记账的规则名称
     * @param actionId    {@link Intent}的action标识符，用于区别不同的操作，可使用{@link BroadcastActions}中的枚举对象的.toString()方法
     * @param title       按钮的文本
     * @param requestCode PendingIntent的唯一请求代码
     * @param remoteInput 通知输入框（不需要输入框可以为null）
     * @return 通知Action实例，可直接使用.addAction()添加至NotificationCompat.Builder中
     */
    @NonNull
    private NotificationCompat.Action createAction(
            @NonNull Bundle dataBundle,
            String ruleName,
            String actionId,
            String title,
            int notificationId,
            int requestCode,
            RemoteInput remoteInput
    ) {
        //创建Intent
        Intent intent = new Intent(this, AbNotificationActionsReceiver.class);
        intent.setAction(actionId);
        intent.putExtra(KeyStrings.NOTIFICATION_ID.v(), notificationId);
        intent.putExtras(dataBundle);                                   //发送流水记录数据包
        intent.putExtra(KeyStrings.NOTIFICATION_RULE_NAME.v(), ruleName);   //发送规则名称

        //创建PendingIntent
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                notificationId * 10 + requestCode,  //为了区分不同通知和不同的 Action，必须将通知标识符和 Action 标识符组合
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        //创建构建器实例
        NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                title,
                pi
        );
        if (remoteInput != null) {
            builder.addRemoteInput(remoteInput);
        }

        return builder.build();
    }

    /**
     * 直接将生成的流水记录数据保存到数据库中
     *
     * @param amount 提取的金额数据
     * @param model  触发的通知规则（包含转账账户等其他数据）
     */
    private void saveInDbDirectly(double amount, @NonNull NotificationRuleWithDetailModel model) {
        //解析规则数据
        NotificationRuleEntity rule = model.getRule();
        String remark = "通知记账 : " + rule.getName();
        int type = rule.getType();
        NotificationRuleTransferEntity ruleTransfer = model.getTransfer();
        String exportAccount = ruleTransfer.getExportAccount();
        String importAccount = ruleTransfer.getImportAccount();
        List<Long> tagIdList = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .collect(Collectors.toList());

        //实例化实体类
        AccountEntity account = new AccountEntity(amount, remark, type, LocalDateTime.now());
        AccountTransferEntity transfer = new AccountTransferEntity(exportAccount, importAccount);

        //保存数据
        String ruleName = rule.getName();
        int notificationId = (int) (rule.getRuleId() + System.currentTimeMillis() + NotificationID.AUTO_BOOKKEEPING_CONFIRM.ordinal());
        disposable.add(AccountService.addNewAccount(account, transfer, null, tagIdList, this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        accountId -> {
                            //创建通知构建器
                            String content = String.format(
                                    Locale.getDefault(),
                                    "“%s”生成的%s记录已自动入账，金额为%.2f，点击查看详情。",
                                    ruleName, AccountType.values()[rule.getType()].getTitle(), amount
                            );
                            String channelID = ChannelInfo.AUTO_BOOKKEEPING.getId();
                            PendingIntent accountModifyPendingIntent = getAccountDetailPendingIntent(accountId);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("通知记账")
                                    .setContentText(content)
                                    .setContentIntent(accountModifyPendingIntent)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setCategory(NotificationCompat.CATEGORY_CALL)
                                    .setAutoCancel(true);

                            //发送通知
                            NotificationHelper.sendNotification(
                                    notificationId,
                                    builder,
                                    this
                            );
                        },
                        e -> {
                            //创建通知构建器
                            String content = String.format(Locale.getDefault(), "保存由“%s”触发的记录时出错。", ruleName);
                            sendErrorNotification(content, rule.getRuleId());
                        }
                )
        );
    }

    /**
     * 获取能够跳转到流水记录输入界面的 PendingInten
     *
     * @param accountId 新添加的流水记录的编号
     * @return 能够跳转到流水记录输入界面的 PendingIntent
     */
    private PendingIntent getAccountDetailPendingIntent(long accountId) {
        //生成数据包
        Bundle bundle = new Bundle();
        bundle.putLong(KeyStrings.RUNNING_ID.v(), accountId);

        //生成 Intent
        Intent skip2AccountInput = new Intent(this, RunningAccountInputActivity.class);
        skip2AccountInput.putExtras(bundle);

        //生成 PendingIntent
        return TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(skip2AccountInput)
                .getPendingIntent(
                        PendingRequestCode.SKIP_TO_ACCOUNT_INPUT.ordinal(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
    }
}
