package com.sly.coffer.ui.pages.main.settings.sub;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sly.coffer.SlyCoffer;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.databinding.ActivityPermissionManageBinding;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.main.settings.components.SettingClickableTextView;

import java.util.Objects;

public class PermissionManageActivity extends AppCompatActivity {
    private ActivityPermissionManageBinding binding;                //绑定的XML视图
    private final ActivityResultLauncher<String> runtimeLauncher =  //申请运行时权限的启动器
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    o -> {
                        if (o) {
                            Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
    private SettingClickableTextView camera, appList, notification, notificationListener, battery, alarm;
    private SettingClickableTextView autoBookkeeping, pick;

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
        camera.setFunctionListener(v -> showExplanationDialog(
                R.string.camera_permission,
                "该权限允许应用调用摄像头，应用范围如下：\n" +
                        "- 添加流水记录图片时使用内置拍照功能拍照\n",
                () -> requestRuntimePermission(Manifest.permission.CAMERA)
        ));
        camera.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            Intent skip2Settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            skip2Settings.setData(uri);
            startActivity(skip2Settings);
            return true;
        });

        //应用列表权限
        if (PermissionHelper.isRuntimePermissionDefined("com.android.permission.GET_INSTALLED_APPS", this)) {
            appList = new SettingClickableTextView(
                    this,
                    binding.appListOption,
                    R.string.app_list_permission,
                    "允许获取应用列表",
                    R.drawable.outline_apps_24,
                    RadiusStyle.MIDDLE
            );
            appList.setFunctionListener(v -> showExplanationDialog(
                    R.string.app_list_permission,
                    "该权限允许应用读取应用列表，应用范围如下：\n" +
                            "- 在输入通知解析规则时读取应用列表以便快速输入包名\n",
                    () -> requestRuntimePermission("com.android.permission.GET_INSTALLED_APPS")
            ));
            appList.setOnLongClickListener(view -> {
                SlyCoffer.lockLifecycleObserver(true);
                Intent skip2Settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                skip2Settings.setData(uri);
                startActivity(skip2Settings);
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
        notification.setFunctionListener(v -> showExplanationDialog(
                        R.string.notification_permission,
                        "该权限允许应用发送通知，应用范围如下：\n" +
                                "- 预算余额低时发送提醒通知\n" +
                                "- 触发自动记账后发送确认通知\n",
                        () -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestRuntimePermission(Manifest.permission.POST_NOTIFICATIONS);
                            } else {
                                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
                            }
                        }
                )
        );
        notification.setOnLongClickListener(view -> {
            SlyCoffer.lockLifecycleObserver(true);
            Intent skip2Settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            skip2Settings.setData(uri);
            startActivity(skip2Settings);
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
        notificationListener.setFunctionListener(v -> showExplanationDialog(
                R.string.notification_listener_permission,
                "该权限允许应用读取其他应用发送的通知，应用范围如下：\n" +
                        "- 读取其他应用的通知实现自动记账\n" +
                        "- 捕获其他应用发送的通知用于快速生成通知规则\n",
                () -> {
                    SlyCoffer.lockLifecycleObserver(true);
                    Intent skip2NotificationListener = PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.getIntent(this);
                    startActivity(skip2NotificationListener);
                }
        ));

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
            autoStart.setFunctionListener(v -> showExplanationDialog(
                            R.string.auto_start_permission,
                            "该权限是定制安卓中特有的权限，其允许应用在后台启动服务，应用范围如下：\n" +
                                    "- 在退出应用后自动启动通知监听服务，确保自动记账功能能够运行\n",
                            () -> {
                                SlyCoffer.lockLifecycleObserver(true);
                                Intent skip2AutoStartPermission = PermissionHelper.SpecialPermissionType.AUTO_START.getIntent(this);
                                startActivity(skip2AutoStartPermission);
                            }
                    )
            );
        } else {
            binding.autoStartOption.getRoot().setVisibility(View.GONE);
        }

        //电池优化策略
        battery = new SettingClickableTextView(
                this,
                binding.batteryOptimizationsOption,
                R.string.battery_optimization,
                "设置安卓原生的电池优化策略",
                R.drawable.outline_battery_android_frame_3_24,
                RadiusStyle.MIDDLE
        );
        battery.setFunctionListener(v -> showExplanationDialog(
                R.string.battery_optimization,
                "该设置项是原生安卓的一个重要设置项，可以更加底层地调整应用的省电策略。" +
                        "如果需要确保应用在后台能够运行，请在电池优化设置界面找到对应应用，点击开关左侧的文本并勾选“无限制”。" +
                        "影响范围如下：\n" +
                        "- 自动记账的通知监听服务能否在后台保持运行\n" +
                        "- 自动记账触发后能否第一时间发送通知\n",
                () -> {
                    if (PermissionHelper.SpecialPermissionType.BATTERY.isGranted(this)) {
                        Toast.makeText(this, "已忽略电池优化，长按强制跳转电池优化界面", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = PermissionHelper.SpecialPermissionType.BATTERY.getIntent(this);
                    if (Objects.equals(intent.getAction(), Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) {
                        Toast.makeText(this, "请找到本应用并设置电池优化策略为“无限制”", Toast.LENGTH_SHORT).show();
                    }
                    SlyCoffer.lockLifecycleObserver(true);
                    startActivity(intent);
                }
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
        alarm.setFunctionListener(v -> showExplanationDialog(
                R.string.alarm_permission,
                "该权限允许应用执行某些定时任务，以实现一些自动化功能，应用范围如下：\n" +
                        "- 每日0点自动检查并重置预算\n",
                () -> {
                    SlyCoffer.lockLifecycleObserver(true);
                    Intent skip2ExactAlarm = PermissionHelper.SpecialPermissionType.ALARM.getIntent(this);
                    startActivity(skip2ExactAlarm);
                }
        ));

        //无障碍自动记账
        autoBookkeeping = new SettingClickableTextView(
                this,
                binding.autoBookkeepingService,
                R.string.auto_bookkeeping,
                "识别屏幕内容实现自动记账",
                R.drawable.outline_checkbook_24,
                RadiusStyle.TOP
        );
        autoBookkeeping.setFunctionListener(view -> showExplanationDialog(
                R.string.auto_bookkeeping,
                "该服务用于识别屏幕内容实现自动记账。",
                () -> {
                    SlyCoffer.lockLifecycleObserver(true);
                    Intent intent = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.getIntent(this);
                    startActivity(intent);
                }
        ));

        //无障碍视图拾取
        pick = new SettingClickableTextView(
                this,
                binding.pagePickService,
                R.string.page_pick,
                "允许获取当前显示界面的信息",
                R.drawable.outline_screenshot_monitor_24,
                RadiusStyle.BOTTOM
        );
        pick.setFunctionListener(view -> showExplanationDialog(
                R.string.page_pick,
                "该服务用于输入无障碍规则时获取屏幕点击位置，以此得到点击的视图信息作为金额来源。",
                () -> {
                    SlyCoffer.lockLifecycleObserver(true);
                    Intent intent = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.getIntent(this);
                    startActivity(intent);
                }
        ));
    }

    /**
     * 显示权限解释对话框
     *
     * @param title   对话框标题
     * @param message 对话框内容，支持Markdown格式
     * @param action  点击确定按钮后执行的操作
     */
    private void showExplanationDialog(@StringRes int title, String message, Runnable action) {
        new MarkdownDialogBuilder(this, getString(title), message)
                .setPositiveButton("前往设置", (dialogInterface, i) -> action.run())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 申请运行时权限
     *
     * @param permission 运行时权限
     */
    private void requestRuntimePermission(String permission) {
        if (PermissionHelper.isRuntimePermissionGranted(permission, this)) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
        } else {
            runtimeLauncher.launch(permission);
        }
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
        boolean isCameraGranted = PermissionHelper.isRuntimePermissionGranted(Manifest.permission.CAMERA, this);
        camera.getFunctionComponent().setText(isCameraGranted ? GRANTED : NOT_GRANTED);

        //应用列表权限
        if (appList != null) {
            boolean isAppListGranted = PermissionHelper.isRuntimePermissionGranted(
                    "com.android.permission.GET_INSTALLED_APPS",
                    this
            );
            appList.getFunctionComponent().setText(isAppListGranted ? GRANTED : NOT_GRANTED);
        }

        //通知权限
        boolean isNotificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                PermissionHelper.isRuntimePermissionGranted(Manifest.permission.POST_NOTIFICATIONS, this);
        notification.getFunctionComponent().setText(isNotificationGranted ? GRANTED : NOT_GRANTED);

        //通知监听
        boolean isNotificationListenerGranted = PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this);
        notificationListener.getFunctionComponent().setText(isNotificationListenerGranted ? GRANTED : NOT_GRANTED);

        //电池优化
        boolean isBatteryIgnored = PermissionHelper.SpecialPermissionType.BATTERY.isGranted(this);
        battery.getFunctionComponent().setText(isBatteryIgnored ? "已忽略" : "未忽略");

        //精确闹钟权限
        boolean isAlarmGranted = PermissionHelper.SpecialPermissionType.ALARM.isGranted(this);
        alarm.getFunctionComponent().setText(isAlarmGranted ? GRANTED : NOT_GRANTED);

        //无障碍自动记账服务
        boolean isAccessibilityBookkeepingGranted = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_BOOKKEEPING.isGranted(this);
        autoBookkeeping.getFunctionComponent().setText(isAccessibilityBookkeepingGranted ? ENABLED : NOT_ENABLED);

        //无障碍视图拾取服务
        boolean isAccessibilityPickGranted = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_PICK.isGranted(this);
        pick.getFunctionComponent().setText(isAccessibilityPickGranted ? ENABLED : NOT_ENABLED);
    }
}