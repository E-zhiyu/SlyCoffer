package com.sly.coffer.helpers;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;

public class ExceptionHelper {
    public static void showExceptionDialog(Context context, @NonNull Throwable e) {
        String errMessage = e.getMessage();
        new MaterialAlertDialogBuilder(context)
                .setTitle("运行出错")
                .setMessage(errMessage)
                .setPositiveButton("复制错误信息", (dialog, which) -> {
                    String label = context.getString(R.string.app_name) + "错误信息";
                    ImmHelper.copyToClipboard(label, errMessage, context);
                    Toast.makeText(context, "已将错误信息复制到剪贴板", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setCancelable(false)   //无法点击空白区域取消
                .setNegativeButton("关闭", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
