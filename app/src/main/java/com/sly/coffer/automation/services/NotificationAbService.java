package com.sly.coffer.automation.services;

import android.app.Notification;
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
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.automation.broadcast.BroadcastActions;
import com.sly.coffer.auxiliary.enums.settings.NotificationClickBehaviour;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.BookkeepingNotiRuleUnionModel;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.helpers.AppListHelper;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;
import com.sly.coffer.ui.pages.notification.rule.NotificationRuleListActivity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class NotificationAbService extends NotificationListenerService {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final PublishSubject<RuleWaitToTrigger> waitToTriggerSubject = PublishSubject.create(); //待触发规则的缓冲队列，用于同时处理多个通知
    private final Map<NotificationKey, List<BookkeepingNotiRuleUnionModel>> ruleMap = new HashMap<>();  //已启用规则的哈希表
    private final Map<Long, List<NotificationRuleGroupRefEntity>> groupMap = new HashMap<>();                                     //分组哈希表（k:分组编号，v:该组中的规则编号)

    private static class NotificationKey {
        private final String title;                                     //通知标题
        private final String packageName;                               //通知发送者包名

        public NotificationKey(String packageName, String title) {
            this.packageName = packageName;
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            NotificationKey key = (NotificationKey) o;
            return Objects.equals(title, key.title) &&
                    Objects.equals(packageName, key.packageName);       //只要包名和通知标题匹配则判定为相同对象
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, packageName);
        }
    }

    /**
     * 等待被触发的通知规则与金额的集合类
     */
    private static class RuleWaitToTrigger {
        final BookkeepingNotiRuleUnionModel model;    //规则模型
        final double amount;                          //该规则识别到的金额数据

        public RuleWaitToTrigger(BookkeepingNotiRuleUnionModel model, double amount) {
            this.model = model;
            this.amount = amount;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), "服务已启动");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), "服务已创建");

        //加载规则
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.notificationRuleDao().getEnabledNotificationRuleFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(
                        modelList -> {
                            ruleMap.clear();
                            Map<NotificationKey, List<BookkeepingNotiRuleUnionModel>> map = modelList.stream()
                                    .collect(Collectors.groupingBy(
                                            model -> {
                                                NotificationRuleEntity rule = model.getRule();
                                                return new NotificationKey(rule.getPackageName(), rule.getTargetTitle());
                                            },
                                            HashMap::new,
                                            Collectors.toList()
                                    ));

                            ruleMap.putAll(map);
                        },
                        e -> Log.e(LogTags.NOTIFICATION_AB_SERVICE.n(), "通知记账服务获取通知规则失败")
                )
        );

        //加载分组
        disposable.add(db.notificationRuleDao().getRuleGroupRefFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(
                        refList -> {
                            groupMap.clear();
                            Map<Long, List<NotificationRuleGroupRefEntity>> map = refList.stream()
                                    .collect(Collectors.groupingBy(
                                            NotificationRuleGroupRefEntity::getGroupId,
                                            HashMap::new,
                                            Collectors.toList()
                                    ));
                            groupMap.putAll(map);
                        },
                        e -> Log.e(LogTags.NOTIFICATION_AB_SERVICE.n(), "通知记账服务获取规则与分组映射失败")
                )
        );

        //构建通知缓冲流，将短时间内的通知统一处理
        final long BATCH_DELAY_MS = 1000;   //缓冲队列冲刷的等待时间
        disposable.add(waitToTriggerSubject
                .publish(shared ->
                        shared.buffer(shared.debounce(BATCH_DELAY_MS, TimeUnit.MILLISECONDS)))
                .filter(list -> !list.isEmpty())
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(
                        this::handleBatchLogic,
                        throwable -> Log.e(LogTags.NOTIFICATION_AB_SERVICE.n(), "RxJava 流错误", throwable)
                )
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), "服务已关闭");
        disposable.dispose();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) {
            return;
        }

        //保存通知内容
        if (AutoBookKeepingPreference.getNotificationCapture(this)) {
            captureNotification(sbn);
        }

        //判断是否开启通知解析功能
        if (!AutoBookKeepingPreference.getSwitchStat(this)) {
            Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), "通知记账未启用");
            return;
        }

        //获取通知数据
        String packageName = sbn.getPackageName();
        Notification sbnNotification = sbn.getNotification();
        String title = sbnNotification.extras.getString(Notification.EXTRA_TITLE);
        String text = sbnNotification.extras.getString(Notification.EXTRA_TEXT);
        if (text == null || text.isEmpty() || title == null || title.isEmpty()) return;
        String log = String.format(
                Locale.getDefault(),
                "通知发送者：%s\n通知标题：%s\n通知内容：%s",
                packageName, title, text
        );
        Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), log);

        //获取与包名和标题匹配的规则
        NotificationKey key = new NotificationKey(packageName, title);
        List<BookkeepingNotiRuleUnionModel> ruleModelList = ruleMap.get(key);
        if (ruleModelList == null || ruleModelList.isEmpty()) return;

        //获取待触发的规则
        for (BookkeepingNotiRuleUnionModel model : ruleModelList) {
            //解析数据
            NotificationRuleEntity rule = model.getRule();
            String contentRegex = rule.getContentRegex();
            String name = rule.getName();
            long ruleId = rule.getRuleId();

            //编译正则表达式
            Matcher matcher;
            try {
                Pattern pattern = Pattern.compile(contentRegex);
                matcher = pattern.matcher(text);
            } catch (PatternSyntaxException e) {
                Log.e(LogTags.NOTIFICATION_AB_SERVICE.n(), "正则表达式编译出错");
                String err = String.format(
                        Locale.getDefault(),
                        "规则“%s”的正则表达式编译出错",
                        name
                );
                sendErrorNotification(err, ruleId);
                continue;
            }

            //若通知内容匹配规则，则将规则模型加入待触发列表
            if (!matcher.find()) continue;
            Log.d(LogTags.NOTIFICATION_AB_SERVICE.n(), "成功匹配正则表达式");

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
            Log.i(LogTags.NOTIFICATION_AB_SERVICE.n(), "流水数据生成成功");

            //将规则添加到待触发的哈希表中
            RuleWaitToTrigger waitToTrigger = new RuleWaitToTrigger(model, amount);
            waitToTriggerSubject.onNext(waitToTrigger);
        }
    }

    /**
     * 将通知保存到数据库
     *
     * @param sbn 需要保存的通知
     */
    private void captureNotification(@NonNull StatusBarNotification sbn) {
        //获取通知数据
        String packageName = sbn.getPackageName();
        String appName = AppListHelper.getAppNameByPackageName(packageName, this);
        Notification sbnNotification = sbn.getNotification();
        String title = sbnNotification.extras.getString(Notification.EXTRA_TITLE);
        String text = sbnNotification.extras.getString(Notification.EXTRA_TEXT);
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
                        () -> Log.i(LogTags.NOTIFICATION_AB_SERVICE.n(), "通知捕获成功"),
                        e -> Log.e(LogTags.NOTIFICATION_AB_SERVICE.n(), e.getMessage() == null ? "通知捕获失败" : e.getMessage())
                )
        );
    }

    /**
     * 待触发的规则互斥逻辑
     *
     * @param waitToTriggerList 待触发的规则列表
     */
    private void handleBatchLogic(List<RuleWaitToTrigger> waitToTriggerList) {
        if (waitToTriggerList == null || waitToTriggerList.isEmpty()) return;

        //构建用于互斥的集合变量
        Map<Long, List<NotificationRuleGroupRefEntity>> subGroupMap = new HashMap<>();  //去掉了无需触发的规则的分组的哈希表
        Map<Long, RuleWaitToTrigger> waitToTriggerMap = new HashMap<>();                //待触发的规则哈希表（k:规则编号，v:待触发的规则）
        List<RuleWaitToTrigger> noGroupWaitToTriggerList = new ArrayList<>();           //没有分组但需要触发的规则

        //将通知列表中的通知全部解析，获取待触发的逻辑
        for (RuleWaitToTrigger waitToTrigger : waitToTriggerList) {
            List<NotificationRuleGroupRefEntity> refList = waitToTrigger.model.getGroupRefList();
            if (!refList.isEmpty()) {
                //添加待触发的规则
                waitToTriggerMap.put(waitToTrigger.model.getRule().getRuleId(), waitToTrigger);

                //获取分组哈希表的子集
                for (NotificationRuleGroupRefEntity ref : refList) {
                    long groupId = ref.getGroupId();
                    if (subGroupMap.containsKey(groupId)) continue;
                    subGroupMap.put(groupId, groupMap.get(groupId));
                }
            } else {
                noGroupWaitToTriggerList.add(waitToTrigger);
            }
        }

        //逐个分组扫描需要在分组中的规则
        for (Map.Entry<Long, List<NotificationRuleGroupRefEntity>> entry : subGroupMap.entrySet()) {
            List<NotificationRuleGroupRefEntity> ruleRefList = entry.getValue();
            if (ruleRefList == null || ruleRefList.isEmpty()) return;

            boolean isRuleTriggered = false;
            for (NotificationRuleGroupRefEntity ref : ruleRefList) {
                //取出并删除在哈希表中待触发的规则
                RuleWaitToTrigger waitToTrigger = waitToTriggerMap.get(ref.getRuleId());
                if (waitToTrigger == null) continue;
                waitToTriggerMap.remove(ref.getRuleId());

                //如果该分组此前没有规则触发，则触发一次规则
                if (!isRuleTriggered) {
                    isRuleTriggered = true; //更新标识符
                    if (!AutoBookKeepingPreference.getDirectDeposit(this)) {
                        sendConfirmNotification(waitToTrigger.amount, waitToTrigger.model);
                    } else {
                        saveInDbDirectly(waitToTrigger.amount, waitToTrigger.model);
                    }

                    //移除被该规则屏蔽的规则
                    for (NotificationRuleGroupRefEntity triggeredRef : waitToTrigger.model.getGroupRefList()) {
                        //获取相关联的分组中的映射数据
                        long relatedGroupId = triggeredRef.getGroupId();
                        long triggeredOrder = triggeredRef.getOrder();
                        List<NotificationRuleGroupRefEntity> relatedRefList = subGroupMap.get(relatedGroupId);
                        if (relatedRefList == null || relatedRefList.isEmpty()) continue;

                        //删除优先级比触发规则低的待触发规则
                        for (NotificationRuleGroupRefEntity relatedRef : relatedRefList) {
                            if (relatedRef.getOrder() < triggeredOrder) continue;
                            waitToTriggerMap.remove(relatedRef.getRuleId());
                        }
                    }
                }
            }
        }

        //逐个触发没在任何一个分组的规则
        for (RuleWaitToTrigger noGroupToTrigger : noGroupWaitToTriggerList) {
            if (!AutoBookKeepingPreference.getDirectDeposit(this)) {
                sendConfirmNotification(noGroupToTrigger.amount, noGroupToTrigger.model);
            } else {
                saveInDbDirectly(noGroupToTrigger.amount, noGroupToTrigger.model);
            }
        }
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
            @NonNull BookkeepingNotiRuleUnionModel model
    ) {
        //获取规则数据
        NotificationRuleEntity rule = model.getRule();
        String remark = rule.getName();                     //规则名称
        int type = rule.getType();                          //流水种类枚举序数
        long[] tagIds = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .mapToLong(Long::longValue)
                .toArray();                                 //标签列表
        NotificationRuleTransferEntity transfer = model.getTransfer();  //转账账户数据

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
        bundle.putInt(KeyStrings.AUTO_BOOKKEEPING_TYPE.v(), AutoBookkeepingType.NOTIFICATION.ordinal());    //自动记账种类

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
    private void sendConfirmNotification(double amount, @NonNull BookkeepingNotiRuleUnionModel model) {
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
    private void saveInDbDirectly(double amount, @NonNull BookkeepingNotiRuleUnionModel model) {
        //解析规则数据
        NotificationRuleEntity rule = model.getRule();
        String remark = rule.getName();
        int type = rule.getType();
        NotificationRuleTransferEntity ruleTransfer = model.getTransfer();
        String exportAccount = ruleTransfer.getExportAccount();
        String importAccount = ruleTransfer.getImportAccount();
        List<Long> tagIdList = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .collect(Collectors.toList());

        //实例化实体类
        AccountEntity account = new AccountEntity(amount, remark, type, LocalDateTime.now(), AutoBookkeepingType.NOTIFICATION.ordinal());
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
