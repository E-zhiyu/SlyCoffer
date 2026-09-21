package com.sly.coffer.helpers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

public class ImmHelper {
    /**
     * 显示输入法
     *
     * @param view 需要输入文本的视图
     */
    public static void showImm(@NonNull View view) {
        Context context = view.getContext();
        view.requestFocus();
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        if (imm == null) {
            return;
        }

        boolean success = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        if (!success) { //失败后再尝试
            view.postDelayed(() -> {
                view.requestFocus();
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }, 300); // 稍微长一点的延迟，确保布局彻底稳定
        }
    }

    /**
     * 复制到剪贴板
     *
     * @param label   复制内容的标题
     * @param content 复制的内容
     * @param context 上下文
     */
    public static void copyToClipboard(String label, String content, @NonNull Context context) {
        //获取系统剪贴板服务
        ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);

        //创建 ClipData 对象
        ClipData clip = ClipData.newPlainText(label, content);

        //设置剪贴板内容
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }
}
