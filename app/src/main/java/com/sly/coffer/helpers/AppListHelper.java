package com.sly.coffer.helpers;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.sly.coffer.auxiliary.classes.AppInfo;
import com.sly.coffer.helpers.appearence.ImageHelper;

import java.util.ArrayList;
import java.util.List;

public class AppListHelper {
    /**
     * 加载应用列表
     *
     * @param isSysAppIncluded 是否包含系统应用
     * @param context          上下文
     * @return 读取到的应用信息列表
     */
    @NonNull
    public static List<AppInfo> getInstalledApps(boolean isSysAppIncluded, @NonNull Context context) {
        List<AppInfo> appInfoList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        @SuppressLint("QueryPermissionsNeeded")
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if (!isSysAppIncluded && ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0))
                continue;   //动态排除系统应用

            String appName = app.loadLabel(pm).toString();              //获取应用名称
            String packageName = app.packageName;                       //获取包名
            Drawable originDrawable = app.loadIcon(pm);                 //获取应用图标

            //转换为Bitmap并缩放
            Bitmap scaledBitmap = ImageHelper.getRoundedCornerIcon(context, originDrawable);
            AppInfo appInfo = new AppInfo(appName, packageName, scaledBitmap);
            appInfoList.add(appInfo);
        }

        return appInfoList;
    }

    /**
     * 通过包名获取应用名称
     *
     * @param packageName 包名
     * @param context     上下文
     * @return 该包名对应的应用名，未找到返回“<未知应用>”
     */
    @NonNull
    public static String getAppNameByPackageName(String packageName, @NonNull Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "<未知应用>";
        }
    }

    /**
     * 判断是否为活动名称
     *
     * @param componentName 待判断的字符串
     * @return 是否为活动名称
     */
    public static boolean isActivity(ComponentName componentName, @NonNull Context context) {
        try {
            // 尝试通过PackageManager获取Activity信息
            context.getPackageManager().getActivityInfo(componentName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false; // 说明这个组件不是Activity
        }
    }
}
