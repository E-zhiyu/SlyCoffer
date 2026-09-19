package com.sly.coffer.ui.pages.notification.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnLongClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.databinding.ViewHolderNotificationRuleInGroupInputBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

/**
 * 分组输入界面的规则列表适配器
 */
public class RuleListAdapter extends ListAdapter<NotificationRuleEntity, RuleListAdapter.ItemViewHolder> {
    private final static DiffUtil.ItemCallback<NotificationRuleEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationRuleEntity oldItem, @NonNull NotificationRuleEntity newItem) {
            return oldItem.getRuleId() == newItem.getRuleId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationRuleEntity oldItem, @NonNull NotificationRuleEntity newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getType() == newItem.getType() &&
                    oldItem.getPackageName().equals(newItem.getPackageName());
        }
    };
    private final AdapterOnClickListener<NotificationRuleEntity> clickListener;
    private final AdapterOnLongClickListener<NotificationRuleEntity> longClickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderNotificationRuleInGroupInputBinding binding;

        public ItemViewHolder(@NonNull ViewHolderNotificationRuleInGroupInputBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸动画
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //点击监听器
            binding.getRoot().setOnClickListener(v ->
                    listener.onClick(getBindingAdapterPosition(), binding.getRoot())
            );

            //长按监听器
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onLongClick(getBindingAdapterPosition(), binding.getRoot());
                return true;
            });
        }
    }

    RuleListAdapter(AdapterOnClickListener<NotificationRuleEntity> clickListener, AdapterOnLongClickListener<NotificationRuleEntity> longClickListener) {
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
                notifyItemChanged(toPosition + 1);      //更新后面的
            }
        });
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderNotificationRuleInGroupInputBinding binding = ViewHolderNotificationRuleInGroupInputBinding.inflate(
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
                        longClickListener.onLongClick(getItem(pos), anchor);
                    }

                    @Override
                    public void onCheckedChange(int pos, boolean finalStat, View anchor) {

                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        NotificationRuleEntity rule = getItem(position);

        //名称
        holder.binding.nameText.setText(rule.getName());

        //类型
        holder.binding.typeText.setText(AccountType.values()[rule.getType()].getTitle());

        //包名
        holder.binding.packageNameText.setText(rule.getPackageName());

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.itemView, getItemCount(), position);
    }
}
