package com.sly.coffer.ui.pages.budget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnLongClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.databinding.ViewHolderBudgetBinding;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import java.util.Locale;

public class BudgetListAdapter extends ListAdapter<BudgetEntity, BudgetListAdapter.BudgetViewHolder> {
    private final static DiffUtil.ItemCallback<BudgetEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull BudgetEntity oldItem, @NonNull BudgetEntity newItem) {
            return oldItem.getBudgetId() == newItem.getBudgetId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BudgetEntity oldItem, @NonNull BudgetEntity newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getInitAmount() == newItem.getInitAmount() &&
                    oldItem.getBalance() == newItem.getBalance() &&
                    oldItem.getStartDate().isEqual(newItem.getStartDate()) &&
                    oldItem.getResetFrequency() == newItem.getResetFrequency();
        }
    };
    private final AdapterOnClickListener<BudgetEntity> clickListener;
    private final AdapterOnLongClickListener<BudgetEntity> longClickListener;

    public static class BudgetViewHolder extends RecyclerView.ViewHolder {
        ViewHolderBudgetBinding binding;

        public BudgetViewHolder(@NonNull ViewHolderBudgetBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸动画
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //设置点击监听
            binding.getRoot().setOnClickListener(v ->
                    listener.onClick(getBindingAdapterPosition(), binding.getRoot())
            );

            //设置长按监听
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onLongClick(getBindingAdapterPosition(), binding.getRoot());
                return true;
            });
        }
    }

    public BudgetListAdapter(AdapterOnClickListener<BudgetEntity> clickListener, AdapterOnLongClickListener<BudgetEntity> longClickListener) {
        super(ITEM_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;

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
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderBudgetBinding binding = ViewHolderBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new BudgetViewHolder(
                binding,
                new ViewHolderListener() {
                    @Override
                    public void onClick(int pos, View anchor) {
                        BudgetEntity budget = getItem(pos);
                        clickListener.onClick(budget, anchor);
                    }

                    @Override
                    public void onLongClick(int pos, View anchor) {
                        BudgetEntity budget = getItem(pos);
                        longClickListener.onLongClick(budget, anchor);
                    }

                    @Override
                    public void onCheckedChange(int pos, boolean finalStat, View anchor) {

                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetEntity budget = getItem(position);

        //名称
        String name = budget.getName();
        holder.binding.nameText.setText(name);

        //起算日期
        String startDate = budget.getStartDate().format(CustomDateTimeFormatter.DATE);
        holder.binding.startDateText.setText(startDate);

        //余额和初始金额
        double initAmount = budget.getInitAmount();
        double balance = budget.getBalance();
        String amountStr = String.format(
                Locale.getDefault(),
                "%s/%s",
                TextHelper.abbreviate(balance),
                TextHelper.abbreviate(initAmount)
        );
        holder.binding.amountText.setText(amountStr);

        //重置频率
        ResetFrequency resetFrequency = ResetFrequency.values()[budget.getResetFrequency()];
        holder.binding.resetFrequencyText.setText(resetFrequency.getTitle());

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.itemView, getItemCount(), position);
    }
}
