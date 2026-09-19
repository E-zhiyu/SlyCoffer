package com.sly.coffer.ui.pages.app_list;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.classes.AppInfo;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.databinding.ViewHolderAppListBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

public class AppListAdapter extends ListAdapter<AppInfo, AppListAdapter.AppInfoViewHolder> {
    private final static DiffUtil.ItemCallback<AppInfo> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getPackageName().equals(newItem.getPackageName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getAppName().equals(newItem.getAppName());
        }
    };
    private final AdapterOnClickListener<AppInfo> clickListener;  //应用条目点击监听器

    public static class AppInfoViewHolder extends RecyclerView.ViewHolder {
        ViewHolderAppListBinding binding;

        public AppInfoViewHolder(@NonNull ViewHolderAppListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸动画
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //设置点击监听
            binding.getRoot().setOnClickListener(v ->
                    listener.onClick(getBindingAdapterPosition(), binding.getRoot())
            );
        }
    }

    public AppListAdapter(AdapterOnClickListener<AppInfo> clickListener) {
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
    public AppInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolderAppListBinding binding = ViewHolderAppListBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AppInfoViewHolder(
                binding,
                new ViewHolderListener() {
                    @Override
                    public void onClick(int pos, View anchor) {
                        AppInfo appInfo = getItem(pos);
                        clickListener.onClick(appInfo, anchor);
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
    public void onBindViewHolder(@NonNull AppInfoViewHolder holder, int position) {
        //获取应用信息数据
        AppInfo appInfo = getItem(position);

        //应用名称
        String appName = appInfo.getAppName();
        holder.binding.appNameText.setText(appName);

        //包名
        String packageName = appInfo.getPackageName();
        holder.binding.packageNameText.setText(packageName);

        //图标
        Bitmap appIcon = appInfo.getAppIcon();
        holder.binding.appIconView.setImageBitmap(appIcon);

        //设置视图圆角
        AppearanceHelper.setRecyclerItemRadius(holder.itemView, getItemCount(), position);
    }
}
