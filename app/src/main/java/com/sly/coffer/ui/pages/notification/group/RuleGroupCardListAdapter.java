package com.sly.coffer.ui.pages.notification.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnLongClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleGroupListUnionModel;
import com.sly.coffer.databinding.ViewHolderNotificationRuleGroupListBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import java.util.Locale;

/**
 * 列表界面的适配器
 */
public class RuleGroupCardListAdapter extends ListAdapter<NotificationRuleGroupListUnionModel, RuleGroupCardListAdapter.ItemViewHolder> {
    private static final DiffUtil.ItemCallback<NotificationRuleGroupListUnionModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationRuleGroupListUnionModel oldItem, @NonNull NotificationRuleGroupListUnionModel newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationRuleGroupListUnionModel oldItem, @NonNull NotificationRuleGroupListUnionModel newItem) {
            return false;
        }
    };
    private final AdapterOnClickListener<NotificationRuleGroupListUnionModel> clickListener;
    private final AdapterOnLongClickListener<NotificationRuleGroupListUnionModel> longClickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderNotificationRuleGroupListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderNotificationRuleGroupListBinding binding, ViewHolderListener listener) {
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

    public RuleGroupCardListAdapter(AdapterOnClickListener<NotificationRuleGroupListUnionModel> clickListener, AdapterOnLongClickListener<NotificationRuleGroupListUnionModel> longClickListener) {
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
        ViewHolderNotificationRuleGroupListBinding binding = ViewHolderNotificationRuleGroupListBinding.inflate(
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
        NotificationRuleGroupListUnionModel model = getItem(position);

        //名称
        holder.binding.nameText.setText(model.getGroup().getName());

        //数量
        String count = String.format(
                Locale.getDefault(),
                "包含%d个规则",
                model.getCount()
        );
        holder.binding.countText.setText(count);

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.itemView, getItemCount(), position);
    }
}
