package com.sly.coffer.ui.others.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;

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
     * @param context  上下文
     * @param hint     文本框顶部的提示
     */
    public EditTextDialogBuilder(Context context, String hint) {
        super(context);
        binding.textLayout.setHint(hint);
    }

    /**
     * 设置输入的文本
     *
     * @param inputText 输入的文本
     * @return 文本框对话框
     */
    public EditTextDialogBuilder setInputText(String inputText) {
        binding.textInput.setText(inputText);
        return this;
    }

    @Override
    protected DialogEdittextBinding getCustomView() {
        return DialogEdittextBinding.inflate(
                LayoutInflater.from(builder.getContext())
        );
    }

    @Override
    public EditTextDialogBuilder setPositiveButton(
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
    public EditTextDialogBuilder setNegativeButton(String btnTitle, DialogInterface.OnClickListener callback) {
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
    public EditTextDialogBuilder setHelpText(String helpText) {
        binding.textLayout.setHelperText(helpText);
        return this;
    }

    /**
     * 设置输入框输入的种类
     *
     * @param inputType 输入种类
     * @return 文本框对话框构建器
     */
    public EditTextDialogBuilder setInputType(int inputType) {
        binding.textInput.setInputType(inputType);
        return this;
    }
}
