package com.sly.coffer.ui.others.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ChipViewHolderListener;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.databinding.ViewHolderChipTextBinding;

public class RelatedLoanSelectAdapter extends ListAdapter<AccountEntity, RelatedLoanSelectAdapter.ItemViewHolder> {
    private final static DiffUtil.ItemCallback<AccountEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AccountEntity oldItem, @NonNull AccountEntity newItem) {
            return oldItem.getAccountId() == newItem.getAccountId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AccountEntity oldItem, @NonNull AccountEntity newItem) {
            return false;
        }
    };
    private final AdapterOnClickListener<AccountEntity> clickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderChipTextBinding binding;
        boolean isBlocked = false;  //监听器是否被屏蔽

        public ItemViewHolder(@NonNull ViewHolderChipTextBinding binding, ChipViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //点击监听
            binding.getRoot().setOnClickListener(view ->
                    listener.onClick(getBindingAdapterPosition(), binding.getRoot())
            );
        }
    }

    public RelatedLoanSelectAdapter(AdapterOnClickListener<AccountEntity> clickListener) {
        super(ITEM_CALLBACK);
        this.clickListener = clickListener;
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
                        clickListener.onClick(getItem(pos), anchor);
                    }

                    @Override
                    public void onClose(int pos, View anchor) {
                    }

                    @Override
                    public void onCheckedChanged(int pos, boolean isChecked, View anchor) {
                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        AccountEntity account = getItem(position);

        String remark = account.getRemark();
        String dateTimeStr = account.getDateTime().format(CustomDateTimeFormatter.DATE_TIME);
        String display = remark.isEmpty()? "流水记录·" + dateTimeStr : remark;
        holder.binding.chip.setText(display);
    }
}
