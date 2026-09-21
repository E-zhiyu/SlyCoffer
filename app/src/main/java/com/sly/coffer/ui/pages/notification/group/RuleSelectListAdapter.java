package com.sly.coffer.ui.pages.notification.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnChipCheckedChangeListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ChipViewHolderListener;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.databinding.ViewHolderChipTextBinding;

import java.util.List;

public class RuleSelectListAdapter extends ListAdapter<NotificationRuleEntity, RuleSelectListAdapter.ItemViewHolder> {
    private static final DiffUtil.ItemCallback<NotificationRuleEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationRuleEntity oldItem, @NonNull NotificationRuleEntity newItem) {
            return oldItem.getRuleId() == newItem.getRuleId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationRuleEntity oldItem, @NonNull NotificationRuleEntity newItem) {
            return oldItem.getName().equals(newItem.getName());
        }
    };
    private final AdapterOnChipCheckedChangeListener<NotificationRuleEntity> checkedChangeListener;
    @Nullable
    private final List<Long> initCheckedList;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderChipTextBinding binding;
        boolean isBlocked = false;  //监听器是否被屏蔽

        public ItemViewHolder(@NonNull ViewHolderChipTextBinding binding, ChipViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            binding.chip.setCloseIconVisible(false);
            binding.chip.setCheckable(true);
            binding.chip.setCheckedIconVisible(true);

            //关闭监听
            binding.chip.setOnCheckedChangeListener((compoundButton, b) -> {
                if (!isBlocked) {
                    listener.onCheckedChanged(getBindingAdapterPosition(), b, binding.getRoot());
                }
            });
        }
    }

    RuleSelectListAdapter(@Nullable List<Long> initCheckedList, AdapterOnChipCheckedChangeListener<NotificationRuleEntity> checkedChangeListener) {
        super(ITEM_CALLBACK);
        this.checkedChangeListener = checkedChangeListener;
        this.initCheckedList = initCheckedList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderChipTextBinding binding = ViewHolderChipTextBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ItemViewHolder(
                binding,
                new ChipViewHolderListener() {
                    @Override
                    public void onClick(int pos, View anchor) {
                    }

                    @Override
                    public void onClose(int pos, View anchor) {
                    }

                    @Override
                    public void onCheckedChanged(int pos, boolean isChecked, View anchor) {
                        checkedChangeListener.onCheckedChange(getItem(pos), isChecked, anchor);
                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        NotificationRuleEntity rule = getItem(position);

        //名称
        holder.binding.chip.setText(rule.getName());

        //设置选择状态
        holder.isBlocked = true;
        boolean isChecked = initCheckedList != null && initCheckedList.contains(rule.getRuleId());
        holder.binding.chip.setChecked(isChecked);
        holder.isBlocked = false;
    }
}
