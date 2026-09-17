package com.sly.coffer.ui.pages.notification.rule;

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
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.databinding.ViewHolderChipTextBinding;

import java.util.Set;

public class NotiRuleGroupSelectListAdapter extends ListAdapter<NotificationRuleGroupEntity, NotiRuleGroupSelectListAdapter.ItemViewHolder> {
    private final static DiffUtil.ItemCallback<NotificationRuleGroupEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationRuleGroupEntity oldItem, @NonNull NotificationRuleGroupEntity newItem) {
            return oldItem.getGroupId() == newItem.getGroupId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationRuleGroupEntity oldItem, @NonNull NotificationRuleGroupEntity newItem) {
            return oldItem.getName().equals(newItem.getName());
        }
    };
    private final AdapterOnChipCheckedChangeListener<NotificationRuleGroupEntity> checkedChangeListener;
    @Nullable
    private final Set<Long> initCheckSet;

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

    /**
     * @param initCheckSet          初始选中的项的编号集合
     * @param checkedChangeListener 选择状态变更监听器
     */
    NotiRuleGroupSelectListAdapter(@Nullable Set<Long> initCheckSet, AdapterOnChipCheckedChangeListener<NotificationRuleGroupEntity> checkedChangeListener) {
        super(ITEM_CALLBACK);
        this.initCheckSet = initCheckSet;
        this.checkedChangeListener = checkedChangeListener;
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
        NotificationRuleGroupEntity entity = getItem(position);
        holder.binding.chip.setText(entity.getName());

        //设置选择状态
        holder.isBlocked = true;
        boolean isChecked = initCheckSet != null && initCheckSet.contains(entity.getGroupId());
        holder.binding.chip.setChecked(isChecked);
        holder.isBlocked = false;
    }
}
