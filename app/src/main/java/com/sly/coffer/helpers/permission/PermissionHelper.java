package com.sly.coffer.helpers.permission;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.device.compat.DeviceOs;
import com.hjq.permissions.OnPermissionDescription;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.sly.coffer.SlyCoffer;

import java.util.List;

public class PermissionHelper {
    public interface PermissionCallback {
        /**
         * 所有权限已经授予的回调
         *
         * @param permissionList 已经授予的权限
         */
        void allGranted(List<IPermission> permissionList);

        /**
         * 权限授予情况回调
         *
         * @param grantedList 授予的权限
         * @param deniedList  拒绝的权限
         */
        void requested(List<IPermission> grantedList, List<IPermission> deniedList);
    }

    /**
     * 判断是否定义了自启动权限
     *
     * @return 是否定义了自启动权限
     */
    public static boolean isAutoStartDefined() {
        //如果能识别到系统名称但是没有自启动权限，返回false
        if (DeviceOs.isOneUi() || DeviceOs.isMyOs() || DeviceOs.isZuxOs() || DeviceOs.isZui()) {
            return false;
        }

        //判断系统名称是否为空，为空说明为类原生系统，否则为拥有自启动设置的定制安卓
        return !DeviceOs.getOsName().isEmpty();
    }

    /**
     * 申请自启动权限
     *
     * @param context 上下文
     */
    public static void requestAutoStartPermission(Context context) {
        if (!isAutoStartDefined()) {
            Toast.makeText(context, "当前系统无需申请自启动权限", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        if (!DeviceOs.isHyperOs() && !DeviceOs.isMiui()) {
            Toast.makeText(context, "请手动前往自启动设置界面授权", Toast.LENGTH_SHORT).show();
            intent = new Intent(Settings.ACTION_SETTINGS);
        } else {
            intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        }

        SlyCoffer.lockLifecycleObserver(true);
        context.startActivity(intent);
    }

    /**
     * 申请权限
     *
     * @param context            上下文
     * @param description        申请权限时向用户展示提示信息的类
     * @param permissionCallback 权限被拒绝时的回调，传递 null 默认为显示权限被拒绝的对话框，并提供前往设置界面的选项
     * @param permissions        需要申请的权限
     */
    public static void requestPermission(Context context, @Nullable OnPermissionDescription description, @Nullable PermissionCallback permissionCallback, @Nullable IPermission... permissions) {
        if (permissions == null || permissions.length == 0) return;

        if (XXPermissions.isGrantedPermissions(context, permissions)) {
            if (permissionCallback != null) {
                permissionCallback.allGranted(List.of(permissions));
            }
        } else {
            SlyCoffer.lockLifecycleObserver(true);
            XXPermissions.with(context)
                    .permissions(permissions)
                    .description(description)
                    .request((grantedList, deniedList) -> {
                        SlyCoffer.lockLifecycleObserver(false);

                        if (permissionCallback == null) {
                            boolean noDenied = deniedList.isEmpty();
                            boolean noGranted = grantedList.isEmpty();
                            if (noDenied && !noGranted) {
                                Toast.makeText(context, "权限已授予", Toast.LENGTH_SHORT).show();
                            } else {
                                String message = noGranted ? "权限申请被拒绝，是否跳转至权限设置界面授予权限？" :
                                        "有权限没有授予，是否跳转至权限设置界面授予权限？";
                                new MaterialAlertDialogBuilder(context)
                                        .setTitle("请求被拒绝")
                                        .setMessage(message)
                                        .setPositiveButton("前往设置", (dialog, which) ->
                                                XXPermissions.startPermissionActivity(context, deniedList)
                                        )
                                        .setNegativeButton("取消", null)
                                        .show();
                            }
                        } else {
                            permissionCallback.requested(grantedList, deniedList);
                        }
                    });
        }
    }

    /**
     * 申请忽略电池优化
     *
     * @param context     上下文
     * @param description 申请权限时用于进行说明的类
     */
    public static void requestBatteryIgnoration(Context context, @Nullable OnPermissionDescription description) {
        IPermission batteryPermission = PermissionLists.getRequestIgnoreBatteryOptimizationsPermission();
        if (XXPermissions.isGrantedPermission(context, batteryPermission)) {
            Toast.makeText(context, "已忽略电池优化", Toast.LENGTH_SHORT).show();
        } else {
            SlyCoffer.lockLifecycleObserver(true);
            if (!DeviceOs.isHyperOs() && !DeviceOs.isMiui()) {
                XXPermissions.with(context)
                        .description(description)
                        .permission(batteryPermission)
                        .request((grantedList, deniedList) -> {
                            SlyCoffer.lockLifecycleObserver(false);

                            boolean noDenied = deniedList.isEmpty();
                            boolean noGranted = grantedList.isEmpty();
                            if (noDenied && !noGranted) {
                                Toast.makeText(context, "已忽略电池优化", Toast.LENGTH_SHORT).show();
                            } else {
                                new MaterialAlertDialogBuilder(context)
                                        .setTitle("请求被拒绝")
                                        .setMessage("请求被拒绝，是否跳转至电池优化界面授予权限？")
                                        .setPositiveButton("前往设置", (dialog, which) -> {
                                            Toast.makeText(context, "请点击开关左侧文本并选择“无限制”", Toast.LENGTH_SHORT).show();
                                            XXPermissions.startPermissionActivity(context, deniedList);
                                        })
                                        .setNegativeButton("取消", null)
                                        .show();
                            }
                        });
            } else {
                Toast.makeText(context, "请点击开关左侧文本并选择“无限制”", Toast.LENGTH_SHORT).show();
                XXPermissions.startPermissionActivity(context, batteryPermission);
            }
        }
    }
}