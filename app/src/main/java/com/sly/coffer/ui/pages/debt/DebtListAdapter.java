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
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccountLoanUnionModel;
import com.sly.coffer.databinding.ViewHolderDebtListBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.compute.DebtHelper;

import java.time.LocalDate;
import java.util.Locale;

public class DebtListAdapter extends ListAdapter<AccountLoanUnionModel, DebtListAdapter.ItemViewHolder> {
    private static final DiffUtil.ItemCallback<AccountLoanUnionModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AccountLoanUnionModel oldItem, @NonNull AccountLoanUnionModel newItem) {
            return oldItem.getAccount().getAccountId() == newItem.getAccount().getAccountId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AccountLoanUnionModel oldItem, @NonNull AccountLoanUnionModel newItem) {
            AccountEntity oldAccount = oldItem.getAccount();
            AccountEntity newAccount = newItem.getAccount();
            ExtraLoanEntity oldLoan = oldItem.getLoan();
            ExtraLoanEntity newLoan = newItem.getLoan();

            return oldAccount.getRemark().equals(newAccount.getRemark()) &&
                    oldAccount.getType() == newAccount.getType() &&
                    oldLoan != null && oldLoan.equals(newLoan) &&
                    oldLoan.getLoanTargetName().equals(newLoan.getLoanTargetName()) &&
                    oldItem.getRepaymentList().equals(newItem.getRepaymentList());
        }
    };
    private final AdapterOnClickListener<AccountLoanUnionModel> clickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderDebtListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderDebtListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //绑定触摸监听器
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //绑定点击监听器
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));
        }
    }

    DebtListAdapter(AdapterOnClickListener<AccountLoanUnionModel> clickListener) {
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
        ViewHolderDebtListBinding binding = ViewHolderDebtListBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ItemViewHolder(
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
        AccountLoanUnionModel model = getItem(position);

        String targetAndRemark = String.format(
                Locale.getDefault(),
                "%s · %s",
                model.getLoan() != null ? model.getLoan().getLoanTargetName() : "<未知对象>",
                model.getAccount().getRemark()
        );
        holder.binding.remarkTargetText.setText(targetAndRemark);           //借款对象和备注
        LocalDate repayDate = model.getLoan() != null ? model.getLoan().getRepayDate() : null;
        String repayDateStr;
        if (repayDate != null) {
            repayDateStr = String.format(
                    Locale.getDefault(),
                    "应在%s还清",
                    repayDate.format(CustomDateTimeFormatter.DATE)
            );
        } else {
            repayDateStr = "<未规定应还日期>";
        }
        holder.binding.repayDateText.setText(repayDateStr);                 //应还日期
        AccountType type = AccountType.values()[model.getAccount().getType()];
        holder.binding.typeText.setText(type.getTitle());                   //流水种类
        Double debt = DebtHelper.computeDebt(model);
        String debtStr;
        if (debt == null) {
            debtStr = "<无法计算金额>";
        } else if (debt == 0) {
            debtStr = "已还清";
        } else {
            debtStr = String.format(
                    Locale.getDefault(),
                    "%s%s",
                    type == AccountType.BORROW ? "待还款" : "待收债",
                    debt
            );
        }
        holder.binding.debtText.setText(debtStr);                           //债务金额
        int repaymentCount = model.getRepaymentList().size();
        String countStr;
        if (repaymentCount == 0) {
            countStr = "无还款记录";
        } else {
            countStr = repaymentCount + "条还款记录";
        }
        holder.binding.repaymentCountText.setText(countStr);                //还款记录数量

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.binding.getRoot(), getItemCount(), holder.getBindingAdapterPosition());
    }
}
