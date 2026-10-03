package com.sly.coffer.helpers.appearence;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

public class VisibilityHelper {
    /**
     * 使用淡入淡出动画切换视图可见性
     *
     * @param view      需要切换可见性的视图
     * @param isVisible 是否可见
     */
    public static void toggleVisibilityWithFade(View view, boolean isVisible) {
        toggleVisibilityWithFade(view, isVisible, 250);
    }

    /**
     * 使用淡入淡出动画切换视图可见性
     *
     * @param view      需要切换可见性的视图
     * @param isVisible 是否可见
     * @param duration  动画持续时间
     */
    public static void toggleVisibilityWithFade(View view, boolean isVisible, int duration) {
        if (isVisible && view.getVisibility() == View.GONE) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        } else if (!isVisible && view.getVisibility() == View.VISIBLE) {
            view.animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .withEndAction(() -> view.setVisibility(View.GONE))
                    .start();
        }
    }

    /**
     * 通用的视图折叠/展开（显示/隐藏）动画方法
     *
     * @param sceneRoot  动画作用的父容器（如 AppBarLayout, CoordinatorLayout, LinearLayout 等）
     * @param isVisible  true 为展开(VISIBLE)，false 为折叠(GONE)
     * @param endAction  动画结束后的回调闭包（可用于清空数据、释放资源等），可传 null
     * @param targetView 要显示或隐藏的根目标视图
     */
    public static void toggleViewExpansion(
            @NonNull ViewGroup sceneRoot,
            boolean isVisible,
            @Nullable Runnable endAction,
            @NonNull View... targetView) {
        toggleViewExpansion(sceneRoot, isVisible, 250, endAction, targetView);
    }

    /**
     * 通用的视图折叠/展开（显示/隐藏）动画方法
     *
     * @param sceneRoot   动画作用的父容器（如 AppBarLayout, CoordinatorLayout, LinearLayout 等）
     * @param isVisible   true 为展开(VISIBLE)，false 为折叠(GONE)
     * @param duration    动画时长（毫秒）
     * @param endAction   动画结束后的回调闭包（可用于清空数据、释放资源等），可传 null
     * @param targetViews 要显示或隐藏的根目标视图
     */
    public static void toggleViewExpansion(
            @NonNull ViewGroup sceneRoot,
            boolean isVisible,
            long duration,
            @Nullable Runnable endAction,
            View... targetViews) {
        if (targetViews == null || targetViews.length == 0) return;

        int targetVisibility = isVisible ? View.VISIBLE : View.GONE;
        boolean needsAnimation = false;

        // 1. 检查是否真的需要执行动画（只要有任何一个 View 状态发生变化即可）
        for (View targetView : targetViews) {
            if (targetView.getVisibility() != targetVisibility) {
                needsAnimation = true;
                break;
            }
        }

        // 如果所有的 View 都已经是目标状态，直接执行结束操作并返回
        if (!needsAnimation) {
            if (endAction != null) {
                endAction.run();
            }
            return;
        }

        // 2. 组装动画集
        TransitionSet transitionSet = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setDuration(duration);

        // ChangeBounds 负责父容器平滑折叠，以及其他被挤压/拉伸的兄弟 View 的位置移动。
        transitionSet.addTransition(new ChangeBounds());

        // Fade 负责控制透明度
        Fade fadeTransition = new Fade(isVisible ? Fade.IN : Fade.OUT);

        // 3. 【核心修正】只给 Fade 动画添加目标！这样可以防止 sceneRoot 中其他的子 View 发生不必要的透明度闪烁。
        for (View targetView : targetViews) {
            if (targetView.getVisibility() != targetVisibility) {
                fadeTransition.addTarget(targetView);
            }
        }
        transitionSet.addTransition(fadeTransition);

        // 4. 设置动画结束回调（挂载在整个 TransitionSet 上）
        if (endAction != null) {
            transitionSet.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    endAction.run();
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionStart(@NonNull Transition transition) {
                }

                @Override
                public void onTransitionCancel(@NonNull Transition transition) {
                }

                @Override
                public void onTransitionPause(@NonNull Transition transition) {
                }

                @Override
                public void onTransitionResume(@NonNull Transition transition) {
                }
            });
        }

        // 5. 【核心修正】通知 TransitionManager 开始捕获变化（必须在改变可见性之前，且整个过程只调用一次！）
        TransitionManager.beginDelayedTransition(sceneRoot, transitionSet);

        // 6. 执行可见性变更（系统会自动计算旧状态到新状态的差异，并播放动画）
        for (View targetView : targetViews) {
            if (targetView.getVisibility() != targetVisibility) {
                targetView.setVisibility(targetVisibility);
            }
        }
    }
}
