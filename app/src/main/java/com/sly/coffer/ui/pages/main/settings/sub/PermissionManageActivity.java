package com.sly.coffer.ui.pages.main.settings.sub;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.R;
import com.sly.coffer.automation.services.AccessibilityAbService;
import com.sly.coffer.automation.services.NotificationAbService;
import com.sly.coffer.automation.services.PickAccessibilityService;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.databinding.ActivityPermissionManageBinding;
import com.sly.coffer.helpers.permission.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.main.settings.components.SettingClickableTextView;

import java.util.List;
import java.util.function.Function;

public class PermissionManageActivity extends AppCompatActivity {
    private ActivityPermissionManageBinding binding;                //绑定的XML视图
    private SettingClickableTextView camera, appList, notification, notificationListener, battery, alarm;
    private SettingClickableTextView autoBookkeeping, pick;
    private final PermissionHelper.PermissionCallback REQUEST_CALLBACK = new PermissionHelper.PermissionCallback() {
        @Override
        public void allGranted(List<IPermission> permissionList) {
            Toast.makeText(PermissionManageActivity.this, "权限已授予", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void requested(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList, Function<List<IPermission>, String> descriptionFun) {
            boolean noDenied = deniedList.isEmpty();
            boolean noGranted = grantedList.isEmpty();
            if (noDenied && !noGranted) {
                Toast.makeText(PermissionManageActivity.this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else if (!XXPermissions.isDoNotAskAgainPermissions(PermissionManageActivity.this, deniedList)) {
                Toast.makeText(PermissionManageActivity.this, "请求被拒绝", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PermissionManageActivity.this, "请手动开启权限", Toast.LENGTH_SHORT).show();
                XXPermissions.startPermissionActivity(PermissionManageActivity.this, deniedList);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionManageBinding.inflate(getLayoutInflater());

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

        //刷新权限授予情况指示器
        refreshPermissionStat();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        //相机权限
        camera = new SettingClickableTextView(
                this,
                binding.cameraOption,
                R.string.camera_permission,
                "允许使用摄像头",
                R.drawable.baseline_photo_camera_24,
                RadiusStyle.TOP
        );
        camera.setFunctionListener(v -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "该权限允许应用调用摄像头，应用范围如下：\n- 写日记时使用系统相机添加媒体文件\n",
                REQUEST_CALLBACK,
                PermissionLists.getCameraPermission()
        ));
        camera.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            XXPermissions.startPermissionActivity(this, PermissionLists.getCameraPermission());
            return true;
        });

        //应用列表权限
        if (PermissionHelper.isAppListPermissionDefined(this)) {
            appList = new SettingClickableTextView(
                    this,
                    binding.appListOption,
                    R.string.app_list_permission,
                    "允许获取应用列表",
                    R.drawable.outline_apps_24,
                    RadiusStyle.MIDDLE
            );
            appList.setFunctionListener(v -> PermissionHelper.requestPermission(
                    this,
                    permissionList ->
                            "该权限允许应用读取应用列表，应用范围如下：\n" +
                                    "- 在输入自动记账规则时读取应用列表以便快速输入包名\n",
                    REQUEST_CALLBACK,
                    PermissionLists.getGetInstalledAppsPermission()
            ));
            appList.setOnLongClickListener(view -> {
                SlyCoffer.lockLifecycleObserver(true);
                XXPermissions.startPermissionActivity(this, PermissionLists.getGetInstalledAppsPermission());
                return true;
            });
        } else {
            binding.appListOption.getRoot().setVisibility(View.GONE);
        }

        //通知权限
        notification = new SettingClickableTextView(
                this,
                binding.notificationOption,
                R.string.notification_permission,
                "允许发送通知",
                R.drawable.outline_notification_settings_24,
                RadiusStyle.MIDDLE
        );
        notification.setFunctionListener(v -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "该权限允许应用发送通知，应用范围如下：\n" +
                                "- 预算余额低时发送提醒通知\n" +
                                "- 触发自动记账后发送确认通知\n",
                REQUEST_CALLBACK,
                PermissionLists.getPostNotificationsPermission()
        ));
        notification.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            XXPermissions.startPermissionActivity(this, PermissionLists.getPostNotificationsPermission());
            return true;
        });

        //通知监听权限
        notificationListener = new SettingClickableTextView(
                this,
                binding.notificationListenerOption,
                R.string.notification_listener_permission,
                "允许监听其他应用的通知",
                R.drawable.outline_notifications_active_24,
                RadiusStyle.MIDDLE
        );
        notificationListener.setFunctionListener(v -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "该权限允许应用读取其他应用发送的通知，应用范围如下：\n" +
                                "- 读取其他应用的通知实现自动记账\n" +
                                "- 捕获其他应用发送的通知用于快速生成通知规则\n",
                REQUEST_CALLBACK,
                PermissionLists.getBindNotificationListenerServicePermission(NotificationAbService.class)
        ));
        notificationListener.setOnLongClickListener(v -> {
            XXPermissions.startPermissionActivity(this, PermissionLists.getBindNotificationListenerServicePermission(NotificationAbService.class));
            return true;
        });

        //自启动权限
        if (PermissionHelper.isAutoStartDefined()) {
            SettingClickableTextView autoStart = new SettingClickableTextView(
                    this,
                    binding.autoStartOption,
                    R.string.auto_start_permission,
                    "允许在后台启动服务",
                    R.drawable.outline_autorenew_24,
                    RadiusStyle.MIDDLE
            );
            autoStart.setFunctionListener(v -> new MarkdownDialogBuilder(
                    this,
                    getString(R.string.auto_start_permission),
                    "该权限是定制安卓中特有的权限，其允许应用在后台启动服务，应用范围如下：\n" +
                            "- 确保定时任务能够在后台自动执行\n" +
                            "- 在退出应用后自动启动通知监听服务，确保自动记账功能能够运行\n")
                    .setPositiveButton("前往设置", (dialogInterface, i) ->
                            PermissionHelper.requestAutoStartPermission(this)
                    )
                    .setNegativeButton("取消", null)
                    .show());
            autoStart.setOnLongClickListener(v -> {
                PermissionHelper.requestAutoStartPermission(this);
                return true;
            });
        } else {
            binding.autoStartOption.getRoot().setVisibility(View.GONE);
        }

        //电池优化策略
        battery = new SettingClickableTextView(
                this,
                binding.batteryOption,
                R.string.ignore_battery_optimization,
                "设置安卓原生的电池优化策略",
                R.drawable.outline_battery_android_frame_3_24,
                RadiusStyle.MIDDLE
        );
        battery.setFunctionListener(v -> PermissionHelper.requestBatteryIgnoration(
                this,
                permissionList ->
                        "该设置项是原生安卓的一个重要设置项，可以更加底层地调整应用的省电策略。" +
                                "如果需要确保应用在后台能够运行，请在电池优化设置界面找到对应应用，点击开关左侧的文本并勾选“无限制”。" +
                                "影响范围如下：\n" +
                                "- 自动记账的通知监听服务能否在后台保持运行\n" +
                                "- 自动记账触发后能否第一时间发送通知\n"
        ));
        battery.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
            return true;
        });

        //精确闹钟权限
        alarm = new SettingClickableTextView(
                this,
                binding.alarmOption,
                R.string.alarm_permission,
                "允许设置定时任务",
                R.drawable.outline_alarm_24,
                RadiusStyle.BOTTOM
        );
        alarm.setFunctionListener(v -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "该权限允许应用执行某些定时任务，以实现一些自动化功能，应用范围如下：\n" +
                                "- 根据用户设置自动检测当天是否有日记并发送通知提醒\n",
                REQUEST_CALLBACK,
                PermissionLists.getScheduleExactAlarmPermission()
        ));
        alarm.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            XXPermissions.startPermissionActivity(this, PermissionLists.getScheduleExactAlarmPermission());
            return true;
        });

        //无障碍自动记账
        autoBookkeeping = new SettingClickableTextView(
                this,
                binding.autoBookkeepingService,
                R.string.auto_bookkeeping,
                "识别屏幕内容实现自动记账",
                R.drawable.outline_checkbook_24,
                RadiusStyle.TOP
        );
        autoBookkeeping.setFunctionListener(view -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "此权限为无障碍权限，开启后将允许APP识别并操作屏幕，该服务的作用如下：\n" +
                                "- 识别屏幕内容实现自动记账。\n",
                REQUEST_CALLBACK,
                PermissionLists.getBindAccessibilityServicePermission(AccessibilityAbService.class)
        ));
        autoBookkeeping.setOnLongClickListener(v -> {
            XXPermissions.startPermissionActivity(this, PermissionLists.getBindAccessibilityServicePermission(AccessibilityAbService.class));
            return true;
        });

        //无障碍视图拾取
        pick = new SettingClickableTextView(
                this,
                binding.pagePickService,
                R.string.page_pick,
                "允许获取当前显示界面的信息",
                R.drawable.outline_screenshot_monitor_24,
                RadiusStyle.BOTTOM
        );
        pick.setFunctionListener(view -> PermissionHelper.requestPermission(
                this,
                permissionList ->
                        "此权限为无障碍权限，开启后将允许APP识别并操作屏幕，该服务的作用如下：\n" +
                                "- 该服务用于记录显示过的界面信息，用于在添加无障碍规则时选择。",
                REQUEST_CALLBACK,
                PermissionLists.getBindAccessibilityServicePermission(PickAccessibilityService.class)
        ));
        pick.setOnLongClickListener(v -> {
            XXPermissions.startPermissionActivity(this, PermissionLists.getBindAccessibilityServicePermission(PickAccessibilityService.class));
            return true;
        });
    }

    /**
     * 刷新权限授予情况指示器
     */
    private void refreshPermissionStat() {
        final String GRANTED = "已授予";
        final String ENABLED = "已启用";
        final String NOT_GRANTED = "未授予";
        final String NOT_ENABLED = "未启用";

        //相机权限
        boolean isCameraGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getCameraPermission());
        camera.getFunctionComponent().setText(isCameraGranted ? GRANTED : NOT_GRANTED);

        //应用列表权限
        if (appList != null) {
            boolean isAppListGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getGetInstalledAppsPermission());
            appList.getFunctionComponent().setText(isAppListGranted ? GRANTED : NOT_GRANTED);
        }

        //通知权限
        boolean isNotificationGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getPostNotificationsPermission());
        notification.getFunctionComponent().setText(isNotificationGranted ? GRANTED : NOT_GRANTED);

        //通知监听
        boolean isNotificationListenerGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getBindNotificationListenerServicePermission(NotificationAbService.class));
        notificationListener.getFunctionComponent().setText(isNotificationListenerGranted ? GRANTED : NOT_GRANTED);

        //电池优化
        boolean isBatteryIgnored = XXPermissions.isGrantedPermission(this, PermissionLists.getRequestIgnoreBatteryOptimizationsPermission());
        battery.getFunctionComponent().setText(isBatteryIgnored ? "已忽略" : "未忽略");

        //精确闹钟权限
        boolean isAlarmGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getScheduleExactAlarmPermission());
        alarm.getFunctionComponent().setText(isAlarmGranted ? GRANTED : NOT_GRANTED);

        //无障碍自动记账服务
        boolean isAccessibilityBookkeepingGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getBindAccessibilityServicePermission(AccessibilityAbService.class));
        autoBookkeeping.getFunctionComponent().setText(isAccessibilityBookkeepingGranted ? ENABLED : NOT_ENABLED);

        //无障碍视图拾取服务
        boolean isAccessibilityPickGranted = XXPermissions.isGrantedPermission(this, PermissionLists.getBindAccessibilityServicePermission(PickAccessibilityService.class));
        pick.getFunctionComponent().setText(isAccessibilityPickGranted ? ENABLED : NOT_ENABLED);
    }
}