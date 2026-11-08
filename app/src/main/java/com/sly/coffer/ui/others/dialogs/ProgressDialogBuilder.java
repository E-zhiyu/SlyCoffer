package com.sly.coffer.ui.others.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.sly.coffer.databinding.DialogProgressBinding;

import java.util.Locale;

public class ProgressDialogBuilder
        extends CustomDialogBuilderBase<
        DialogProgressBinding,
        DialogInterface.OnClickListener,
        DialogInterface.OnClickListener
        > {
    private final String originSubTitle;    //对话框底部副标题

    public ProgressDialogBuilder(Context context, @Nullable String originSubTitle) {
        super(context);
        this.originSubTitle = originSubTitle == null ? "" : originSubTitle;
    }

    @Override
    protected DialogProgressBinding getCustomView() {
        //创建自定义布局
        return DialogProgressBinding.inflate(
                LayoutInflater.from(builder.getContext())
        );
    }

    @Override
    public CustomDialogBuilderBase<DialogProgressBinding, DialogInterface.OnClickListener, DialogInterface.OnClickListener> setPositiveButton(String btnTitle, DialogInterface.OnClickListener callback) {
        builder.setPositiveButton(btnTitle, callback);
        return this;
    }

    @Override
    public CustomDialogBuilderBase<DialogProgressBinding, DialogInterface.OnClickListener, DialogInterface.OnClickListener> setNegativeButton(String btnTitle, DialogInterface.OnClickListener callback) {
        builder.setNegativeButton(btnTitle, callback);
        return this;
    }

    @Override
    public AlertDialog show() {
        binding.subTitleText.setText(originSubTitle);
        setIndeterminate(true);    //默认不确定进度模式
        return super.show();
    }

    /**
     * 更新进度
     *
     * @param current  当前数量
     * @param total    总数量
     * @param subTitle 对话框底部副标题
     */
    public void updateProgress(int current, int total, String subTitle) {
        //设置进度（0-100）
        int progressPercent = (int) ((current / (float) total) * 100);
        binding.progressIndicator.setProgressCompat(progressPercent, true);

        //更新文本
        binding.progressText.setText(String.format(Locale.getDefault(),
                "%d/%d (%d%%)", current, total, progressPercent));

        //更新副标题
        binding.subTitleText.setText(subTitle != null ? subTitle : this.originSubTitle);
    }

    /**
     * 设置为不确定模式（用于初始阶段）
     *
     * @param indeterminate 是否为不确定模式
     */
    public void setIndeterminate(boolean indeterminate) {
        //如果当前状态与目标状态相同，则不执行任何操作
        if (binding.progressIndicator.isIndeterminate() == indeterminate) {
            return;
        }

        //修改状态
        binding.progressIndicator.setIndeterminate(indeterminate);

        if (indeterminate) {
            binding.progressText.setVisibility(View.GONE);
        } else {
            binding.progressText.setVisibility(View.VISIBLE);
        }
    }
}
