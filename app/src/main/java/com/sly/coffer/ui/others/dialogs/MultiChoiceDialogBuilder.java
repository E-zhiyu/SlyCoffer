package com.sly.coffer.ui.others.dialogs;

import android.content.Context;
import android.view.LayoutInflater;

import com.sly.coffer.databinding.DialogMultichoiceBinding;
import com.sly.coffer.ui.others.adapters.MultiChoiceDialogAdapter;

import java.util.List;

public class MultiChoiceDialogBuilder
        extends CustomDialogBuilderBase<
        DialogMultichoiceBinding,
        MultiChoiceDialogBuilder.OnClickedListener,
        MultiChoiceDialogBuilder.OnClickedListener
        > {
    private final MultiChoiceDialogAdapter adapter; //多选列表适配器

    public static class ChoiceItem {
        private final boolean initStat;   //初始状态
        private final String title;       //显示名称
        private final boolean enabled;    //是否启用

        public ChoiceItem(boolean initStat, String title, boolean enabled) {
            this.initStat = initStat;
            this.title = title;
            this.enabled = enabled;
        }

        public boolean getInitStat() {
            return initStat;
        }

        public String getTitle() {
            return title;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public interface OnClickedListener {
        /**
         * 确认按钮点击回调
         *
         * @param checkedStatList 选项选择状态
         */
        void onConfirmed(List<Boolean> checkedStatList);
    }

    /**
     * 自定义对话框构造器基类构造方法
     *
     * @param context  上下文
     * @param itemList 选项列表
     */
    public MultiChoiceDialogBuilder(Context context, List<ChoiceItem> itemList) {
        super(context);

        //实例化多选列表的适配器
        adapter = new MultiChoiceDialogAdapter(itemList);
        binding.itemRecycler.setAdapter(adapter);
    }

    @Override
    protected DialogMultichoiceBinding getCustomView() {
        return DialogMultichoiceBinding.inflate(
                LayoutInflater.from(builder.getContext())
        );
    }

    @Override
    public CustomDialogBuilderBase<DialogMultichoiceBinding, OnClickedListener, OnClickedListener> setPositiveButton(String btnTitle, OnClickedListener callback) {
        if (callback != null) {
            builder.setPositiveButton(btnTitle, (dialogInterface, i) ->
                    callback.onConfirmed(adapter.getCheckedStatList())
            );
        } else {
            builder.setPositiveButton(btnTitle, null);
        }
        return this;
    }

    @Override
    public CustomDialogBuilderBase<DialogMultichoiceBinding, OnClickedListener, OnClickedListener> setNegativeButton(String btnTitle, OnClickedListener callback) {
        if (callback != null) {
            builder.setNegativeButton(btnTitle, (dialogInterface, i) ->
                    callback.onConfirmed(adapter.getCheckedStatList())
            );
        } else {
            builder.setNegativeButton(btnTitle, null);
        }
        return this;
    }
}
