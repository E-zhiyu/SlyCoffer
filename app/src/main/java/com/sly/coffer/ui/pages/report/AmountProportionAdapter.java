package com.sly.coffer.ui.pages.report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.AmountProportionInfo;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.databinding.ViewHolderAmountProportionBinding;

import java.util.Locale;

public class AmountProportionAdapter extends ListAdapter<AmountProportionInfo, AmountProportionAdapter.AmountProportionViewHolder> {
    private final static DiffUtil.ItemCallback<AmountProportionInfo> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AmountProportionInfo oldItem, @NonNull AmountProportionInfo newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AmountProportionInfo oldItem, @NonNull AmountProportionInfo newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getPercentage() == newItem.getPercentage() &&
                    oldItem.getName().equals(newItem.getName());
        }
    };
    private final AdapterOnClickListener<AmountProportionInfo> clickListener;

    public static class AmountProportionViewHolder extends RecyclerView.ViewHolder {
        ViewHolderAmountProportionBinding binding;

        public AmountProportionViewHolder(@NonNull ViewHolderAmountProportionBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //点击监听
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));
        }
    }

    public AmountProportionAdapter(AdapterOnClickListener<AmountProportionInfo> clickListener) {
        super(ITEM_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public AmountProportionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderAmountProportionBinding binding = ViewHolderAmountProportionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AmountProportionViewHolder(
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
    public void onBindViewHolder(@NonNull AmountProportionViewHolder holder, int position) {
        AmountProportionInfo info = getItem(position);

        //名称
        String name = info.getName();
        holder.binding.sourceNameText.setText(name);

        //金额
        double amount = info.getAmount();
        holder.binding.amountText.setText(String.format(Locale.getDefault(), "%.2f", amount));
        if (amount < 0) {
            holder.binding.percentageBar.setIndicatorColor(holder.itemView.getContext().getColor(R.color.md_theme_error));
        }

        //百分比
        int percentage = info.getPercentage();
        String percentageStr = String.format(Locale.getDefault(), "%d%%", percentage);
        holder.binding.percentageText.setText(percentageStr);
        holder.binding.percentageBar.setProgress(percentage);
    }
}
