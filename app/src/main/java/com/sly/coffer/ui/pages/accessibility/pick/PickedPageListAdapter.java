package com.sly.coffer.ui.pages.accessibility.pick;

import android.content.Context;
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
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.databinding.ViewHolderPickedPageListBinding;
import com.sly.coffer.helpers.AppListHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

public class PickedPageListAdapter extends ListAdapter<PickedPageEntity, RecyclerView.ViewHolder> {
    private static final DiffUtil.ItemCallback<PickedPageEntity> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull PickedPageEntity oldItem, @NonNull PickedPageEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PickedPageEntity oldItem, @NonNull PickedPageEntity newItem) {
            return oldItem.getRemark().equals(newItem.getRemark()) &&
                    oldItem.getActivityName().equals(newItem.getActivityName()) &&
                    oldItem.getPackageName().equals(newItem.getPackageName());
        }
    };

    private final AdapterOnClickListener<PickedPageEntity> clickListener;            //单击监听
    private final AdapterOnLongClickListener<PickedPageEntity> longClickListener;    //长按监听

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderPickedPageListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderPickedPageListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸监听器
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //设置点击监听
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));

            //设置长按监听
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onLongClick(getBindingAdapterPosition(), binding.getRoot());
                return true;
            });
        }
    }

    public PickedPageListAdapter(
            AdapterOnClickListener<PickedPageEntity> clickListener,
            AdapterOnLongClickListener<PickedPageEntity> longClickListener
    ) {
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderPickedPageListBinding binding = ViewHolderPickedPageListBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ItemViewHolder(
                binding,
                new ViewHolderListener() {
                    @Override
                    public void onClick(int position, View anchor) {
                        clickListener.onClick(getItem(position), anchor);
                    }

                    @Override
                    public void onLongClick(int position, View anchor) {
                        longClickListener.onLongClick(getItem(position), anchor);
                    }

                    @Override
                    public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                    }
                }
        );
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PickedPageEntity page = getItem(position);
        Context context = holder.itemView.getContext();

        ItemViewHolder itemHolder = (ItemViewHolder) holder;

        itemHolder.binding.remarkText.setText(page.getRemark());                        //备注
        itemHolder.binding.appNameText.setText(AppListHelper.getAppNameByPackageName(   //应用名称
                page.getPackageName(), context
        ));

        //界面名称
        String[] parts = page.getActivityName().split("\\.");
        String activityName = parts.length > 1 ? parts[parts.length - 1] : "<未知界面>";
        itemHolder.binding.activityNameText.setText(activityName);

        //设置圆角
        AppearanceHelper.setRecyclerItemRadius(holder.itemView, getItemCount(), position);
    }
}
