package com.sly.coffer.automation.services;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.sly.coffer.R;
import com.sly.coffer.automation.broadcast.AbNotificationActionsReceiver;
import com.sly.coffer.automation.broadcast.BroadcastActions;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.auxiliary.enums.settings.NotificationClickBehaviour;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccessibilityRuleUnionModel;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.ui.pages.accessibility.rule.AccessibilityRuleListActivity;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@SuppressLint("AccessibilityPolicy")
public class AccessibilityAbService extends AccessibilityService {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final Map<CacheKey, List<AccessibilityRuleUnionModel>> ruleCacheMap = new HashMap<>();
    private final Map<Long, Long> antiShakeMap = new HashMap<>();   //用于防抖的哈希表，防止规则重复触发多次
    private final BroadcastReceiver SHUT_DOWN_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (BroadcastActions.ACTION_SHUT_DOWN_ACCESSIBILITY_BOOKKEEPING.toString().equals(action)) {
                Log.i(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "接收到关闭广播");
                disableSelf();
            }
        }
    };

    static class CacheKey {
        String packageName;
        String activityName;

        public CacheKey(String packageName, String activityName) {
            this.packageName = packageName;
            this.activityName = activityName;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(packageName, cacheKey.packageName) && Objects.equals(activityName, cacheKey.activityName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, activityName);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.accessibilityRuleDao().getOpenedAccessibilityRuleWithDetailFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        modelList -> {
                            ruleCacheMap.clear();
                            Map<CacheKey, List<AccessibilityRuleUnionModel>> map = modelList.stream()
                                    .collect(Collectors.groupingBy(
                                            model -> {
                                                AccessibilityRuleEntity rule = model.getRule();
                                                return new CacheKey(rule.getPackageName(), rule.getActivityName());
                                            },
                                            HashMap::new,
                                            Collectors.toList()
                                    ));

                            ruleCacheMap.putAll(map);
                        },
                        e -> Log.e(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "无障碍规则获取失败")
                )
        );

        //注册监听器
        IntentFilter filter = new IntentFilter(BroadcastActions.ACTION_SHUT_DOWN_ACCESSIBILITY_BOOKKEEPING.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(SHUT_DOWN_RECEIVER, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(this, SHUT_DOWN_RECEIVER, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "注册监听器");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        //判断 WindowType
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            AccessibilityWindowInfo window = source.getWindow();
            if (window != null) {
                int windowType = window.getType();
                if (windowType != AccessibilityWindowInfo.TYPE_APPLICATION) {
                    return;
                }
            } else {
                Log.w(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "无法获取窗口信息");
                return;
            }
        }

        //获取界面根节点
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.w(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "无法获取当前界面根节点");
            return;
        }

        //获取包名和活动名
        String packageName = root.getPackageName() != null ? root.getPackageName().toString() : "";
        String activityName = event.getClassName() != null ? event.getClassName().toString() : "";
        Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(),
                "种类 : " + event.getEventType() + ",\n包名 : " + packageName + ",\n活动名 : " + activityName
        );

        //获取对应活动名和包名的规则列表
        CacheKey key = new CacheKey(packageName, activityName);
        List<AccessibilityRuleUnionModel> modelList = ruleCacheMap.get(key);
        if (modelList == null || modelList.isEmpty()) {
            Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "当前界面没有匹配的规则");
            return;
        } else {
            String log = String.format(
                    Locale.getDefault(),
                    "获取到%d个规则",
                    modelList.size()
            );
            Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(), log);
        }

        final int DELAY = event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ? 1500 : 500;
        disposable.add(Single.fromCallable(() -> TextHelper.extractAllTextsFromNode(root))
                .delaySubscription(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        allTextSet -> {
                            //提取金额
                            Double amount = null;
                            final Pattern AMOUNT_PATTERN = Pattern.compile(
                                    "\\D?(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\D?"
                            );
                            for (String text : allTextSet) {
                                Matcher matcher = AMOUNT_PATTERN.matcher(text);

                                //仅当全匹配才提取金额
                                if (matcher.matches()) {
                                    String amountStr = matcher.group(1);
                                    try {
                                        amount = Double.parseDouble(Objects.requireNonNull(amountStr)
                                                .replace(",", "")
                                        );
                                    } catch (NumberFormatException e) {
                                        Log.e(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "无法将字符串转换为金额");
                                    }
                                    break;
                                }
                            }
                            if (amount == null) {
                                Log.w(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "无法从该界面中提取金额");
                                return;
                            } else {
                                Log.i(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "成功提取金额 : " + amount);
                            }

                            //尝试触发符合要求的规则
                            for (AccessibilityRuleUnionModel model : modelList) {
                                AccessibilityRuleEntity rule = model.getRule();

                                //获取上一次触发时间，以实现防抖
                                long ruleId = rule.getRuleId();
                                Long lastTimeMillis = antiShakeMap.get(ruleId);
                                long currentTimeMillis = System.currentTimeMillis();
                                if (lastTimeMillis != null && currentTimeMillis - lastTimeMillis < 3000L) {
                                    String log = String.format(Locale.getDefault(), "“%s”触发防抖", rule.getName());
                                    Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(), log);
                                    continue;
                                }

                                //验证关键词组合
                                if (!verifyKeywordGroups(model.getKeywordGroupList(), allTextSet)) {
                                    Log.w(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "关键词校验不通过");
                                    continue;
                                }

                                //根据偏好设置决定直接入帐还是发送通知
                                antiShakeMap.put(ruleId, currentTimeMillis);
                                if (!AutoBookKeepingPreference.getDirectDeposit(this)) {
                                    sendConfirmNotification(amount, model);
                                } else {
                                    saveInDbDirectly(amount, model);
                                }
                            }
                        }
                )
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();

        unregisterReceiver(SHUT_DOWN_RECEIVER);
        Log.d(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "注销监听器");
    }

    @Override
    public void onInterrupt() {
    }

    /**
     * 验证关键词组合
     *
     * @param keywordGroupList 关键词组合列表
     * @param allTextSet       界面中出现的所有文本的集合
     * @return 列表为空时返回 true，列表不为空时，任意一个关键词组合通过时返回 true，否则为 false
     */
    private boolean verifyKeywordGroups(@Nullable List<AccessibilityRuleKeywordGroupEntity> keywordGroupList, Set<String> allTextSet) {
        if (keywordGroupList == null || keywordGroupList.isEmpty()) return true;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.w(LogTags.AB_ACCESSIBILITY_SERVICE.n(), "根节点获取失败，无法验证关键词组合");
            return false;
        }

        //按照关键词数量进行升序排序
        List<AccessibilityRuleKeywordGroupEntity> sortedKeywordGroupList = new ArrayList<>(keywordGroupList);
        sortedKeywordGroupList.sort((g1, g2) -> {
            String c1 = g1.getContent();
            String c2 = g2.getContent();
            int count1 = c1 == null ? 0 : c1.split("\\s+").length;
            int count2 = c2 == null ? 0 : c2.split("\\s+").length;
            return Integer.compare(count1, count2); //关键词少的优先
        });

        //判断关键词是否出现
        boolean isVerified = false;
        for (AccessibilityRuleKeywordGroupEntity group : sortedKeywordGroupList) {
            String[] keywords = group.getContent().split("\\s+");
            if (TextHelper.checkGroupMatchOptimized(keywords, allTextSet)) {
                isVerified = true;
                break;
            }
        }
        return isVerified;
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
        Intent skip2RuleManage = new Intent(this, AccessibilityRuleListActivity.class);
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
                .setContentTitle("无障碍记账出错")
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
     * 获得流水记录数据
     *
     * @param amount 提取的金额数据
     * @param model  匹配到的通知规则详情数据
     * @return 流水数据包
     */
    @NonNull
    private Bundle getNewAccountData(
            double amount,
            @NonNull AccessibilityRuleUnionModel model
    ) {
        //获取规则数据
        AccessibilityRuleEntity rule = model.getRule();
        String remark = rule.getName();                   //规则名称
        int type = rule.getType();                          //流水种类枚举序数
        long[] tagIds = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .mapToLong(Long::longValue)
                .toArray();                                 //标签列表
        AccessibilityRuleTransferEntity transfer = model.getTransfer(); //转账账户数据

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
        bundle.putInt(KeyStrings.AUTO_BOOKKEEPING_TYPE.v(), AutoBookkeepingType.ACCESSIBILITY.ordinal());   //自动记账种类

        return bundle;
    }

    /**
     * 发送通知以提醒用户确认
     *
     * @param amount 提取的金额
     * @param model  触发自动记账的规则（包含抓张账户等其他数据）
     */
    private void sendConfirmNotification(double amount, @NonNull AccessibilityRuleUnionModel model) {
        //生成数据包
        AccessibilityRuleEntity rule = model.getRule();
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
                .setContentTitle("无障碍记账确认")
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
    private void saveInDbDirectly(double amount, @NonNull AccessibilityRuleUnionModel model) {
        //解析规则数据
        AccessibilityRuleEntity rule = model.getRule();
        String remark = rule.getName();
        int type = rule.getType();
        AccessibilityRuleTransferEntity ruleTransfer = model.getTransfer();
        String exportAccount = ruleTransfer.getExportAccount();
        String importAccount = ruleTransfer.getImportAccount();
        List<Long> tagIdList = model.getTagList().stream()
                .map(TagEntity::getTagId)
                .collect(Collectors.toList());

        //实例化实体类
        AccountEntity account = new AccountEntity(amount, remark, type, LocalDateTime.now(), AutoBookkeepingType.ACCESSIBILITY.ordinal());
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
                                    .setContentTitle("无障碍记账")
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
