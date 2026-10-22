package com.sly.coffer.ui.pages.debt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.databinding.ViewHolderRunningAccountListBinding;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import java.util.Locale;

public class RepaymentAccountListAdapter extends ListAdapter<AccountEntity, RepaymentAccountListAdapter.ItemViewHolder> {
    private final static DiffUtil.ItemCallback<AccountEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AccountEntity oldItem, @NonNull AccountEntity newItem) {
            return oldItem.getAccountId() == newItem.getAccountId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AccountEntity oldItem, @NonNull AccountEntity newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getRemark().equals(newItem.getRemark()) &&
                    oldItem.getType() == newItem.getType() &&
                    oldItem.getDateTime().isEqual(newItem.getDateTime());
        }
    };
    private final AdapterOnClickListener<AccountEntity> clickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderRunningAccountListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderRunningAccountListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置点击监听
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));

            //设置触摸监听
            AppearanceHelper.attachMorphAnimation(binding.getRoot());
        }
    }

    RepaymentAccountListAdapter(AdapterOnClickListener<AccountEntity> clickListener) {
        super(ITEM_CALLBACK);
        this.clickListener = clickListener;

        //注册数据变更监听器，用于自动更新圆角
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemChanged(positionStart - 1);           //更新前面的
                notifyItemChanged(positionStart + itemCount);   //更新后面的
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemChanged(positionStart - 1);   //更新前面的
                notifyItemChanged(positionStart);               //更新后面的
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                notifyItemChanged(fromPosition - 1);    //更新前面的
                notifyItemChanged(fromPosition);                //更新后面的

                notifyItemChanged(toPosition - 1);      //更新前面的
                notifyItemChanged(toPosition);                  //更新自己
                notifyItemChanged(toPosition + 1);      //更新后面的
            }
        });
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderRunningAccountListBinding binding = ViewHolderRunningAccountListBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RepaymentAccountListAdapter.ItemViewHolder(
                binding,
                new ViewHolderListener() {
                    @Override
                    public void onClick(int pos, View anchor) {
                        clickListener.onClick(getItem(pos), anchor);
                    }

                    @Override
                    public void onLongClick(int pos, View anchor) {
                    }

                    @Override
                    public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        AccountEntity account = getItem(position);

        //获取流水数据
        String type = AccountType.values()[account.getType()].getTitle();
        String datetime = account.getDateTime().format(CustomDateTimeFormatter.TIME);
        String remark = account.getRemark();
        double amount = account.getAmount();

        //生成自动记账种类字符串
        String autoBookkeepingType;
        int autoTag = account.getAutoTag();
        if (autoTag == AutoBookkeepingType.NOTIFICATION.ordinal()) {
            autoBookkeepingType = "通知记账 · ";
        } else if (autoTag == AutoBookkeepingType.ACCESSIBILITY.ordinal()) {
            autoBookkeepingType = "无障碍记账 · ";
        } else {
            autoBookkeepingType = "";
        }

        //初始化文本视图
        holder.binding.amountText.setText(TextHelper.abbreviate(amount));
        holder.binding.remarkText.setText(remark.isEmpty() ? "<无备注>" : remark);
        String typeTimeAutoTag = String.format(
                Locale.getDefault(),
                "%s%s · %s",
                autoBookkeepingType, type, datetime
        );
        holder.binding.typeTimeAutoTagText.setText(typeTimeAutoTag);

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.binding.getRoot(), getItemCount(), position);
    }
}
