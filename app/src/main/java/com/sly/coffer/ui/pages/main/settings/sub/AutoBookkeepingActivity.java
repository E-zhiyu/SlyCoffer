package com.sly.coffer.ui.pages.main.settings.sub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.R;
import com.sly.coffer.automation.broadcast.BroadcastActions;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.databinding.ActivityAutoBookkeepingBinding;
import com.sly.coffer.auxiliary.enums.settings.NotificationCancelBehaviour;
import com.sly.coffer.auxiliary.enums.settings.NotificationClickBehaviour;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.pages.accessibility.rule.AccessibilityRuleListActivity;
import com.sly.coffer.ui.pages.notification.rule.NotificationRuleListActivity;
import com.sly.coffer.ui.pages.main.settings.components.SettingClickableTextView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSpinnerView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSwitchView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AutoBookkeepingActivity extends AppCompatActivity {
    private ActivityAutoBookkeepingBinding binding; //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();
    private SettingSwitchView notificationBookkeepingSwitch, notificationCaptureSwitch;
    private SettingSwitchView accessibilityBookkeepingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAutoBookkeepingBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollView.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom + AppearanceHelper.dpToPx(this, 15)
            );
            return insets;
        });

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshSettingsStat();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        initNotificationBookkeepingSettings();
        initAccessibilityBookkeepingSettings();
        initCommonSettings();
    }

    /**
     * 初始化通知记账设置项
     */
    private void initNotificationBookkeepingSettings() {
        //通知记账开关
        notificationBookkeepingSwitch = new SettingSwitchView(
                this,
                binding.notificationBookkeepingSwitch,
                R.string.notification_bookkeeping,
                "通过应用发送的通知来记账",
                R.drawable.outline_notifications_active_24,
                RadiusStyle.TOP
        );
        boolean isNotificationAnalysisOpened = AutoBookKeepingPreference.getSwitchStat(this);
        if (isNotificationAnalysisOpened && PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this)) {
            notificationBookkeepingSwitch.setChecked(true);
        } else {
            notificationBookkeepingSwitch.setChecked(false);

            //考虑到无授权情况下自动关闭通知解析功能
            AutoBookKeepingPreference.setSwitchStat(false, this);
        }
        notificationBookkeepingSwitch.setFunctionListener((buttonView, isChecked) -> {
            //没有权限时提示授权
            if (isChecked && !PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("需要权限")
                        .setMessage("此功能需要读取其他应用发送的通知，请授予“通知使用”权限。")
                        .setPositiveButton("去授权", (dialog, which) -> {
                            SlyCoffer.lockLifecycleObserver();
                            Intent intent = PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.getIntent(this);
                            startActivity(intent);
                        })
                        .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.cancel())
                        .setOnCancelListener(dialogInterface -> buttonView.setChecked(false))
                        .show();
            }
            AutoBookKeepingPreference.setSwitchStat(isChecked, this);
        });

        //通知解析规则管理
        SettingClickableTextView ruleManageOption = new SettingClickableTextView(
                this,
                binding.notificationRuleManageOption,
                R.string.notification_rule,
                "点击进入规则管理界面",
                R.drawable.outline_rule_24,
                RadiusStyle.MIDDLE
        );
        ruleManageOption.setFunctionListener(v -> {
            Intent intent = new Intent(this, NotificationRuleListActivity.class);
            startActivity(intent);
        });

        //通知捕获开关
        notificationCaptureSwitch = new SettingSwitchView(
                this,
                binding.notificationCaptureSwitch,
                R.string.capture_notification,
                "保存通知以便添加通知规则",
                R.drawable.outline_download_24,
                RadiusStyle.MIDDLE
        );
        notificationCaptureSwitch.setChecked(AutoBookKeepingPreference.getNotificationCapture(this));
        notificationCaptureSwitch.setFunctionListener((compoundButton, isChecked) -> {
            //没有权限时提示授权
            if (isChecked && !PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("需要权限")
                        .setMessage("此功能需要读取其他应用发送的通知，请授予“通知使用”权限。")
                        .setPositiveButton("去授权", (dialog, which) -> {
                            SlyCoffer.lockLifecycleObserver();
                            Intent intent = PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.getIntent(this);
                            startActivity(intent);
                        })
                        .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.cancel())
                        .setOnCancelListener(dialogInterface -> compoundButton.setChecked(false))
                        .show();
            }
            AutoBookKeepingPreference.setNotificationCapture(this, isChecked);
        });

        //清空捕获通知
        SettingClickableTextView clearCapturedNotification = new SettingClickableTextView(
                this,
                binding.capturedNotificationClearOption,
                R.string.clear_captured_notification,
                "清空数据库中捕获的通知",
                R.drawable.baseline_clear_24,
                RadiusStyle.BOTTOM
        );
        clearCapturedNotification.setFunctionListener(view -> {
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().clearCapturedNotification()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> Toast.makeText(this, "捕获的通知已清除", Toast.LENGTH_SHORT).show(),
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        });
    }

    /**
     * 初始化无障碍记账设置项
     */
    private void initAccessibilityBookkeepingSettings() {
        //无障碍记账开关
        accessibilityBookkeepingSwitch = new SettingSwitchView(
                this,
                binding.accessibilityBookkeepingSwitch,
                R.string.accessibility_bookkeeping,
                "识别屏幕内容实现自动记账",
                R.drawable.outline_accessibility_new_24,
                RadiusStyle.TOP
        );
        accessibilityBookkeepingSwitch.setChecked(
                PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.isGranted(this)
        );
        accessibilityBookkeepingSwitch.setFunctionListener((compoundButton, b) -> {
            if (b) {
                String content = String.format(
                        Locale.getDefault(),
                        "此功能需要启动无障碍服务以识别屏幕内容，请开启“%s”服务。",
                        getString(R.string.accessibility_title_auto_bookkeeping)
                );
                new MaterialAlertDialogBuilder(this)
                        .setTitle("需要权限")
                        .setMessage(content)
                        .setPositiveButton("去设置", (dialog, which) -> {
                            SlyCoffer.lockLifecycleObserver();
                            Intent intent = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.getIntent(this);
                            startActivity(intent);
                        })
                        .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.cancel())
                        .setOnCancelListener(dialogInterface -> compoundButton.setChecked(false))
                        .show();
            } else {
                Intent shutDown = new Intent(BroadcastActions.ACTION_SHUT_DOWN_ACCESSIBILITY_BOOKKEEPING.toString());
                shutDown.setPackage(getPackageName());
                sendBroadcast(shutDown);
            }
        });

        //无障碍规则管理
        SettingClickableTextView accessibilityRuleManage = new SettingClickableTextView(
                this,
                binding.accessibilityRuleManageOption,
                R.string.accessibility_rule,
                "点击进入规则管理界面",
                R.drawable.outline_rule_24,
                RadiusStyle.BOTTOM
        );
        accessibilityRuleManage.setFunctionListener(view -> {
            Intent intent = new Intent(this, AccessibilityRuleListActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 初始化通用设置项
     */
    private void initCommonSettings() {
        //直接入账开关
        SettingSwitchView directDepositSwitch = new SettingSwitchView(
                this,
                binding.directDeposit,
                R.string.direct_deposit,
                "无需确认是否保留而直接入账",
                R.drawable.outline_notifications_off_24,
                RadiusStyle.TOP
        );
        directDepositSwitch.setChecked(AutoBookKeepingPreference.getDirectDeposit(this));
        directDepositSwitch.setFunctionListener((compoundButton, b) ->
                AutoBookKeepingPreference.setDirectDeposit(b, this)
        );

        //通知取消行为
        SettingSpinnerView notificationCancelBehaviour = new SettingSpinnerView(
                this,
                binding.notificationCancelBehaviour,
                R.string.notification_cancel_behaviour,
                "划走确认通知后执行的操作",
                R.drawable.outline_comments_disabled_24,
                RadiusStyle.MIDDLE
        );
        int cancelBehaviourCode = AutoBookKeepingPreference.getNotificationCancelBehaviour(this);
        notificationCancelBehaviour.setSpinnerText(
                NotificationCancelBehaviour.values()[cancelBehaviourCode].getTitle()
        );
        notificationCancelBehaviour.setFunctionListener(v -> {
            PopupMenu behaviourMenu = new PopupMenu(this, notificationCancelBehaviour.getFunctionComponent());

            //填充选项
            for (NotificationCancelBehaviour behaviour : NotificationCancelBehaviour.values()) {
                int groupId = behaviour.getGroupId();
                int itemId = behaviour.getItemId();
                int order = behaviour.getOrder();
                String title = behaviour.getTitle();
                behaviourMenu.getMenu().add(groupId, itemId, order, title);
            }

            //设置监听
            behaviourMenu.setOnMenuItemClickListener(item -> {
                //获取选项编号列表
                List<Integer> itemIdList = Arrays.stream(NotificationCancelBehaviour.values())
                        .map(NotificationCancelBehaviour::getItemId)
                        .collect(Collectors.toList());

                //判断是否选中
                if (itemIdList.contains(item.getItemId())) {
                    int index = itemIdList.indexOf(item.getItemId());
                    AutoBookKeepingPreference.setNotificationCancelBehaviour(this, index);
                    notificationCancelBehaviour.setSpinnerText(item.getTitle());
                    return true;
                } else {
                    return false;
                }
            });

            behaviourMenu.show();
        });

        //通知点击行为
        SettingSpinnerView notificationClickBehaviour = new SettingSpinnerView(
                this,
                binding.notificationClickBehaviour,
                R.string.notification_click_behaviour,
                "点击确认通知后执行的操作",
                R.drawable.outline_ads_click_24,
                RadiusStyle.BOTTOM
        );
        int clickBehaviourCode = AutoBookKeepingPreference.getNotificationClickBehaviour(this);
        notificationClickBehaviour.setSpinnerText(
                NotificationClickBehaviour.values()[clickBehaviourCode].getTitle()
        );
        notificationClickBehaviour.setFunctionListener(v -> {
            PopupMenu behaviourMenu = new PopupMenu(this, notificationClickBehaviour.getFunctionComponent());

            //填充选项
            for (NotificationClickBehaviour behaviour : NotificationClickBehaviour.values()) {
                int groupId = behaviour.getGroupId();
                int itemId = behaviour.getItemId();
                int order = behaviour.getOrder();
                String title = behaviour.getTitle();
                behaviourMenu.getMenu().add(groupId, itemId, order, title);
            }

            //设置监听
            behaviourMenu.setOnMenuItemClickListener(item -> {
                //获取选项编号列表
                List<Integer> itemIdList = Arrays.stream(NotificationClickBehaviour.values())
                        .map(NotificationClickBehaviour::getItemId)
                        .collect(Collectors.toList());

                //判断是否选中
                if (itemIdList.contains(item.getItemId())) {
                    int index = itemIdList.indexOf(item.getItemId());
                    AutoBookKeepingPreference.setNotificationClickBehaviour(this, index);
                    notificationClickBehaviour.setSpinnerText(item.getTitle());
                    return true;
                } else {
                    return false;
                }
            });

            behaviourMenu.show();
        });
    }

    /**
     * 刷新设置项状态
     */
    private void refreshSettingsStat() {
        if (!PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this)) {
            //通知记账开关
            if (notificationBookkeepingSwitch != null && notificationBookkeepingSwitch.getFunctionComponent().isChecked()) {
                notificationBookkeepingSwitch.setChecked(false);
            }

            //通知捕获开关
            if (notificationCaptureSwitch != null && notificationCaptureSwitch.getFunctionComponent().isChecked()) {
                notificationCaptureSwitch.setChecked(false);
            }
        }

        //无障碍记账开关
        if (!PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.isGranted(this) &&
                accessibilityBookkeepingSwitch.getFunctionComponent().isChecked()) {
            accessibilityBookkeepingSwitch.setChecked(false);
        }
    }
}