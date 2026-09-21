package com.sly.coffer.helpers;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.device.compat.DeviceOs;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.automation.services.AccessibilityAbService;
import com.sly.coffer.automation.services.PickAccessibilityService;
import com.sly.coffer.data.save.preference.AppSettingsPreference;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 申请权限的工具类
 */
public class PermissionHelper {
    private final ComponentActivity activity;   //需要申请权限的Activity
    private final Map<String, String> runtimePermissionMessageMap = new HashMap<>(); //运行时权限解释文本
    private final List<String> runtimePermissions = new ArrayList<>();              //运行时权限列表
    private final List<String> rationaleRuntimePermissions = new ArrayList<>();     //可以再次申请的运行时权限
    private final Queue<PermissionRequest> specialQueue = new LinkedList<>();       //特殊权限队列
    private ActivityResultLauncher<String[]> runtimeLauncher;   //申请运行时权限的启动器
    private boolean isProcessing = false;                       //是否正在处理权限，防止在处理权限时重复调用权限申请方法

    public enum SpecialPermissionType {
        //精确闹钟权限
        ALARM(
                PermissionHelper::isExactAlarmEnabled,
                PermissionHelper::buildExactAlarmIntent
        ),
        //电池优化
        @SuppressLint("BatteryLife") BATTERY(
                PermissionHelper::isIgnoringBatteryOptimizations,
                PermissionHelper::buildIgnoringBatteryOptimizationsIntent
        ),
        //通知监听权限
        NOTIFICATION_LISTENER(
                PermissionHelper::isNotificationServiceEnabled,
                c -> buildNotificationListenerIntent()
        ),
        //自启动权限
        AUTO_START(
                PermissionHelper::isAutoStartHinted,
                PermissionHelper::buildAutoStartPermissionIntent
        ),
        ACCESSIBILITY_BOOKKEEPING(
                context -> isAccessibilityServiceEnabled(context, AccessibilityAbService.class),
                PermissionHelper::buildAccessibilityIntent
        ),
        ACCESSIBILITY_PICK(
                context -> isAccessibilityServiceEnabled(context, PickAccessibilityService.class),
                PermissionHelper::buildAccessibilityIntent
        );
        private final Function<Context, Boolean> checker;       //如何检查权限是否授予
        private final Function<Context, Intent> intentBuilder;  //跳转权限界面所需的Intent构建器

        SpecialPermissionType(Function<Context, Boolean> c, Function<Context, Intent> i) {
            this.checker = c;
            this.intentBuilder = i;
        }

        /**
         * 判断权限是否授予
         *
         * @param c 上下文
         * @return 是否授予
         */
        public boolean isGranted(Context c) {
            return checker.apply(c);
        }

        /**
         * 获取跳转到特殊权限设置界面的 Intent
         *
         * @param c 上下文
         * @return 跳转到权限设置界面的 Intent
         */
        @NonNull
        public Intent getIntent(Context c) {
            Intent intent = intentBuilder.apply(c);
            return Objects.requireNonNullElseGet(intent, () -> new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private static class PermissionRequest {
        SpecialPermissionType permission;   //特殊权限
        String customTitle;                 //对话框标题
        String customMessage;               //自定义对话框消息

        PermissionRequest(SpecialPermissionType permission, String title, String message) {
            this.permission = permission;
            this.customTitle = title;
            this.customMessage = message;
        }
    }

    /**
     * 默认构造方法：内部自动注册 Launcher
     */
    public PermissionHelper(@NonNull ComponentActivity activity) {
        this(activity, null);
    }

    /**
     * 支持自定义 Launcher 的构造方法
     *
     * @param customLauncher 如果传入 null，则使用默认的注册逻辑
     */
    public PermissionHelper(@NonNull ComponentActivity activity,
                            @Nullable ActivityResultLauncher<String[]> customLauncher) {
        this.activity = activity;

        //注册活动生命周期监听器，用于申请权限
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                start();    //每当 Activity 重新运行时，都检查一遍权限
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                activity.getLifecycle().removeObserver(this);
            }
        });

        if (customLauncher != null) {
            this.runtimeLauncher = customLauncher;
        } else {
            // 只有在没有提供自定义 Launcher 时才在内部 register
            initDefaultLauncher();
        }
    }

    /**
     * 初始化默认运行时权限申请启动器
     */
    private void initDefaultLauncher() {
        this.runtimeLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    //如果运行时权限被拒绝了，那就不要继续申请
                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();

                        runtimePermissions.remove(permission);
                        if (!entry.getValue() && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            //如果用户没有授权，但是又可以再次申请，说明没有被永久拒绝，转移到再次申请列表中
                            rationaleRuntimePermissions.add(permission);
                        } else {
                            //如果用户没有授权，但是又不能再次申请，则说明权限被永久拒绝，不要再申请，完全删除这个权限
                            rationaleRuntimePermissions.remove(permission);
                        }
                    }
                }
        );
    }

    /**
     * 添加权限请求（使用自定义文案）
     *
     * @param permission 需要申请的权限（类型为{@link String}表示运行时权限，{@link SpecialPermissionType}表示特殊应用权限
     * @param title      对话框标题
     * @param message    提示文本
     */
    public void addPermission(@NonNull SpecialPermissionType permission, String title, String message) {
        if (!permission.isGranted(activity)) {
            specialQueue.add(new PermissionRequest(permission, title, message));
        }
    }

    /**
     * 添加权限申请请求
     *
     * @param permission 运行时权限字符串
     * @param message    用户拒绝过一次后，再次申请前显示的解释对话框的提示信息
     */
    public void addPermission(String permission, String message) {
        //当权限未授予并且在系统中定义时（特别处理“应用列表权限”等原生安卓没有的权限）添加到申请列表中
        if (!isRuntimePermissionGranted(permission, activity)
                && isRuntimePermissionDefined(permission, activity)
        ) {
            runtimePermissions.add(permission);
            runtimePermissionMessageMap.put(permission, message);   //保存运行时权限解释文本的引用
        }
    }

    /**
     * 开始申请权限
     */
    private void start() {
        //检查是否正在处理权限，如果是则直接结束
        if (isProcessing) {
            return;
        }

        if (runtimeLauncher == null) {
            throw new IllegalStateException("RuntimeLauncher has not been initialized. " +
                    "Ensure it was passed in constructor or registered before Activity started.");
        }

        //筛选没有授权的运行时权限
        List<String> deniedPermissions = runtimePermissions.stream()
                .filter(permission -> !isRuntimePermissionGranted(permission, activity))
                .collect(Collectors.toList());

        //先处理运行时权限，再处理需要解释一次的运行时权限，最后处理特殊应用权限
        if (!deniedPermissions.isEmpty()) {
            runtimeLauncher.launch(deniedPermissions.toArray(new String[0]));
        } else if (!rationaleRuntimePermissions.isEmpty()) {
            showRationaleDialog(rationaleRuntimePermissions.toArray(new String[0]));
        } else {
            processNextSpecial();
        }
    }

    /**
     * 处理队下一个特殊应用权限
     */
    private void processNextSpecial() {
        //判断特殊权限是否申请完毕
        if (specialQueue.isEmpty()) {
            Log.i(LogTags.PERMISSION_HELPER.n(), "特殊应用权限申请完毕");
            return;
        }

        //从队列中取出一个特殊应用权限请求
        Log.d(LogTags.PERMISSION_HELPER.n(), "正在处理下个特殊应用权限");
        isProcessing = true;         //标记为正在处理
        PermissionRequest request = specialQueue.poll();
        if (request == null) {
            isProcessing = false;    //如果没有特殊权限了，那就标记为未处理
            return;
        }

        //处理该权限
        handleSpecialPermission(request);
    }

    /**
     * 处理特殊应用权限
     *
     * @param request 权限请求
     */
    private void handleSpecialPermission(@NonNull PermissionRequest request) {
        SpecialPermissionType type = request.permission;
        Log.d(LogTags.PERMISSION_HELPER.n(), request.customTitle);
        if (type.isGranted(activity)) {
            processNextSpecial();
        } else {
            //以后不再提示申请自启动权限
            if (type == SpecialPermissionType.AUTO_START) {
                AppSettingsPreference.setHintAutoStart(true, activity);
            }

            new MaterialAlertDialogBuilder(activity)
                    .setTitle(request.customTitle)
                    .setMessage(request.customMessage)
                    .setPositiveButton("去设置", (d, w) -> {
                        isProcessing = false;    //未直接调用processNextSpecial()，需要标记为未处理

                        SlyCoffer.lockLifecycleObserver();
                        activity.startActivity(type.getIntent(activity));
                    })
                    .setNegativeButton("取消", (d, w) -> processNextSpecial())
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * 显示用户拒绝过一次的运行时权限解释对话框
     *
     * @param permissions 需要一次性申请的运行时权限
     */
    private void showRationaleDialog(@NonNull String[] permissions) {
        //构建解释消息
        StringBuilder messageBuilder = new StringBuilder("为了提供完整的功能，请授予以下权限：  \n");
        for (String permission : permissions) {
            String message = runtimePermissionMessageMap.get(permission);
            if (message != null) {
                messageBuilder.append("- ");
                messageBuilder.append(message);
                messageBuilder.append("  \n");
            }
        }

        //显示对话框
        new MarkdownDialogBuilder(activity, "权限说明", messageBuilder.toString())
                .setPositiveButton("去设置", (dialogInterface, i) -> runtimeLauncher.launch(permissions))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 判断运行时权限是否定义
     *
     * @param permission 运行时权限字符串
     * @param context    上下文
     * @return 权限在系统中是否定义
     */
    public static boolean isRuntimePermissionDefined(String permission, @NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PermissionInfo permissionInfo = pm.getPermissionInfo(permission, 0);
            return permissionInfo != null; // 判断系统是否支持该权限
        } catch (PackageManager.NameNotFoundException e) {
            return false; // 权限未定义，通常是因为系统版本太低或该权限属于特定厂商
        }
    }

    /**
     * 判断运行时权限是否已经授予
     *
     * @param permission 运行时权限字符串
     * @param context    上下文
     * @return 是否授予运行时权限
     */
    public static boolean isRuntimePermissionGranted(String permission, Context context) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查是否有精确闹钟权限
     *
     * @param context 上下文
     * @return 是否拥有精确闹钟权限
     */
    private static boolean isExactAlarmEnabled(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(android.app.AlarmManager.class).canScheduleExactAlarms();
    }

    /**
     * 判断是否提醒了需要开启自启动权限
     *
     * @param context 上下文
     * @return 是否提醒了需要开启自启动权限
     */
    private static boolean isAutoStartHinted(Context context) {
        return !isAutoStartDefined() || AppSettingsPreference.getHintAutoStart(context);
    }

    /**
     * 判断是否定义了自启动权限
     *
     * @return 是否定义了自启动权限
     */
    public static boolean isAutoStartDefined() {
        //如果能被识别但是没有自启动权限，返回false
        if (DeviceOs.isOneUi() || DeviceOs.isMyOs() || DeviceOs.isZuxOs() || DeviceOs.isZui()) {
            return false;
        }

        //类原生没有自启动权限
        return !DeviceOs.getOsName().isEmpty();
    }

    /**
     * 检查通知使用权的授予情况
     *
     * @param context 上下文
     * @return 是否授予通知使用权
     */
    private static boolean isNotificationServiceEnabled(@NonNull Context context) {
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断是否在电池优化白名单中
     *
     * @param context 上下文
     * @return 是否在电池优化白名单
     */
    private static boolean isIgnoringBatteryOptimizations(@NonNull Context context) {
        PowerManager pm = context.getSystemService(PowerManager.class);
        return pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * 判断本应用的无障碍服务是否开启
     *
     * @param context 上下文
     * @return true 表示已开启
     */
    private static boolean isAccessibilityServiceEnabled(@NonNull Context context, Class<? extends AccessibilityService> serviceClass) {
        AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
        if (am == null) return false;

        // 获取所有当前已启用的无障碍服务列表
        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC
                        | AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        String packageName = context.getPackageName().toLowerCase();
        String serviceId = serviceClass.getSimpleName().toLowerCase();

        for (AccessibilityServiceInfo service : enabledServices) {
            String id = service.getId().toLowerCase();
            if (id.contains(packageName) && id.contains(serviceId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否能够跳转到原生忽略电池优化白名单界面
     *
     * @return 是否能跳转原生界面
     */
    private static boolean canSkip2ProtogeneticBatteryOptimizationsPage() {
        return !DeviceOs.isHyperOs() && !DeviceOs.isMiui(); //目前测试只有米米的系统无法跳转，会被拦截至魔改界面
    }

    /**
     * 构建精确闹钟权限Intent
     *
     * @param context 上下文
     * @return 用于申请精确闹钟权限的Intent
     */
    @Nullable
    private static Intent buildExactAlarmIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + context.getPackageName())
            );
        }
        return null;
    }

    /**
     * 构建申请通知监听权限的Intent
     *
     * @return 申请通知监听权限的Intent
     */
    @NonNull
    private static Intent buildNotificationListenerIntent() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * 构建用于申请电池优化白名单的Intent
     *
     * @param context 上下文
     * @return 用于申请电池优化白名单的Intent
     */
    @NonNull
    private static Intent buildIgnoringBatteryOptimizationsIntent(Context context) {
        if (canSkip2ProtogeneticBatteryOptimizationsPage()) {
            @SuppressLint("BatteryLife") Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            return intent;
        } else {
            return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        }
    }

    /**
     * 构建跳转自启动界面的Intent
     *
     * @param context 上下文
     * @return 跳转自启动界面的Intent
     */
    @NonNull
    private static Intent buildAutoStartPermissionIntent(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("xiaomi")) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            return intent;
        } else {
            Toast.makeText(context, "请手动前往自启动界面授权", Toast.LENGTH_SHORT).show();
            return new Intent(Settings.ACTION_SETTINGS);
        }
    }

    /**
     * 生成跳转到无障碍界面的意图
     *
     * @param context 上下文
     * @return 跳转到无障碍设置界面的意图
     */
    @NonNull
    private static Intent buildAccessibilityIntent(@NonNull Context context) {
        // 替换为你的无障碍服务类名（完整路径，例如: com.example.app/com.example.app.MyAccessibilityService）
        String serviceName = context.getPackageName() + "/" + AccessibilityAbService.class.getCanonicalName();

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.putExtra(":settings:fragment_args_key", serviceName);
        intent.putExtra(":settings:show_fragment_args", true);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}