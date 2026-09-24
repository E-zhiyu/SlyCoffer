package com.sly.coffer.ui.others.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.sly.coffer.databinding.DialogEdittextBinding;
import com.sly.coffer.helpers.ImmHelper;

public class EditTextDialogBuilder
        extends CustomDialogBuilderBase<
        DialogEdittextBinding,
        EditTextDialogBuilder.OnClickedListener,
        DialogInterface.OnClickListener
        > {
    public interface OnClickedListener {
        void onClicked(String inputStr);
    }

    /**
     * 文本框对话框构造方法
     *
     * @param context     上下文
     * @param dialogTitle 对话框标题
     * @param hint        输入框的 hint 提示
     */
    public EditTextDialogBuilder(@NonNull Context context, String dialogTitle, String hint) {
        this(context, dialogTitle, hint, "");
    }

    public EditTextDialogBuilder(Context context, String dialogTitle, String hint, String initText) {
        super(context, dialogTitle);
        binding.textLayout.setHint(hint);
        binding.textInput.setText(initText);
    }

    @Override
    protected DialogEdittextBinding getCustomView() {
        return DialogEdittextBinding.inflate(
                LayoutInflater.from(builder.getContext())
        );
    }

    @Override
    public CustomDialogBuilderBase<DialogEdittextBinding, OnClickedListener, DialogInterface.OnClickListener> setPositiveButton(
            String btnTitle,
            OnClickedListener callback
    ) {
        if (callback != null) {
            builder.setPositiveButton(btnTitle, (dialogInterface, i) ->
                    callback.onClicked(String.valueOf(binding.textInput.getText()))
            );
        } else {
            builder.setPositiveButton(btnTitle, null);
        }

        return this;
    }

    @Override
    public CustomDialogBuilderBase<DialogEdittextBinding, OnClickedListener, DialogInterface.OnClickListener> setNegativeButton(String btnTitle, DialogInterface.OnClickListener callback) {
        builder.setNegativeButton(btnTitle, callback);
        return this;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        ImmHelper.showImm(binding.textInput);
        return dialog;
    }

    /**
     * 设置输入框底部的帮助文本
     *
     * @param helpText 帮助文本内容
     * @return 对话框构建器
     */
    public CustomDialogBuilderBase<DialogEdittextBinding,
            EditTextDialogBuilder.OnClickedListener,
            DialogInterface.OnClickListener> setHelpText(String helpText) {
        binding.textLayout.setHelperText(helpText);
        return this;
    }
}
