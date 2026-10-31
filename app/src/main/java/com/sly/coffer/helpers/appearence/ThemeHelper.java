package com.sly.coffer.helpers.appearence;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.settings.ThemeMode;

public class ThemeHelper {
    /**
     * 应用当前选定的主题
     *
     * @param themeMode {@link ThemeMode}的枚举序数
     */
    @SuppressLint("WrongConstant")
    public static void applyTheme(int themeMode) {
        int mode = ThemeMode.values()[themeMode].getCode();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * 切换深色模式并播放过渡动画
     *
     * @param activity  当前显示的活动界面
     * @param modeIndex {@link ThemeMode}的枚举序数
     */
    public static void switchNightModeWithAnimation(@NonNull Activity activity, int modeIndex) {
        SlyCoffer.lockLifecycleObserver(true);

        // 1. 获取根布局
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();

        // 2. 创建覆盖 View
        View overlay = new View(activity);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setAlpha(0f);
        rootView.addView(overlay);

        // 3. 淡入动画
        overlay.animate()
                .alpha(1f)
                .setDuration(200) // 可以调整时长
                .withEndAction(() -> {
                    // 4. 切换夜间模式
                    applyTheme(modeIndex);

                    // 5. 淡出动画
                    overlay.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction(() -> rootView.removeView(overlay))
                            .start();
                })
                .start();
    }

    /**
     * 开关动态配色
     *
     * @param activity 当前显示的活动界面
     * @param isOpened 是否启用动态配色
     */
    public static void applyDynamicColor(@NonNull Activity activity, boolean isOpened) {
        SlyCoffer app = (SlyCoffer) activity.getApplication();
        DynamicColorsOptions options;
        if (isOpened) {
            options = new DynamicColorsOptions.Builder()
                    .setThemeOverlay(R.style.Theme_ManagerAssistant_Dynamic)
                    .build();
        } else {
            options = new DynamicColorsOptions.Builder()
                    .setThemeOverlay(R.style.Theme_ManagerAssistant_Static)
                    .build();
        }
        DynamicColors.applyToActivitiesIfAvailable(app, options);
        activity.recreate();
    }

    /**
     * 开关动态配色并播放过渡动画
     *
     * @param activity 当前显示的活动界面
     * @param isOpened 是否启用动态配色
     */
    public static void switchDynamicColorWithAnimation(@NonNull Activity activity, boolean isOpened) {
        SlyCoffer.lockLifecycleObserver(true);

        // 1. 获取根布局
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();

        // 2. 创建覆盖 View
        View overlay = new View(activity);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setAlpha(0f);
        rootView.addView(overlay);

        // 3. 淡入动画
        overlay.animate()
                .alpha(1f)
                .setDuration(200) // 可以调整时长
                .withEndAction(() -> {
                    // 4. 切换夜间模式
                    applyDynamicColor(activity, isOpened);

                    // 5. 淡出动画
                    overlay.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction(() -> rootView.removeView(overlay))
                            .start();
                })
                .start();
    }
}
