package com.sly.coffer.ui.pages.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnChipCloseListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ChipViewHolderListener;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.databinding.ViewHolderChipTextBinding;

/**
 * 规则输入界面用于显示已选分组的列表的适配器
 */
public class RuleGroupChipListAdapter extends ListAdapter<NotificationRuleGroupEntity, RuleGroupChipListAdapter.ItemViewHolder> {
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

    private final AdapterOnChipCloseListener<NotificationRuleGroupEntity, RuleGroupChipListAdapter> closeListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderChipTextBinding binding;

        public ItemViewHolder(@NonNull ViewHolderChipTextBinding binding, ChipViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            binding.chip.setCloseIconVisible(true);

            //关闭监听
            binding.chip.setOnCloseIconClickListener(view ->
                    listener.onClose(getBindingAdapterPosition(), binding.getRoot())
            );
        }
    }

    public RuleGroupChipListAdapter(AdapterOnChipCloseListener<NotificationRuleGroupEntity, RuleGroupChipListAdapter> closeListener) {
        super(ITEM_CALLBACK);
        this.closeListener = closeListener;
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
                        NotificationRuleGroupEntity tag = getItem(pos);
                        closeListener.onClose(tag, anchor, RuleGroupChipListAdapter.this);
                    }

                    @Override
                    public void onCheckedChanged(int pos, boolean isChecked, View anchor) {
                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        NotificationRuleGroupEntity entity = getItem(position);
        holder.binding.chip.setText(entity.getName());
    }
}
