package com.sly.coffer.ui.pages.notification.capture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnLongClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.CapturedNotificationUiModel;
import com.sly.coffer.databinding.ViewHolderCapturedNotificationListBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderAdapter;

public class NotificationCaptureListAdapter extends ListAdapter<CapturedNotificationUiModel, RecyclerView.ViewHolder>
        implements StickyHeaderAdapter<String> {
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SEPARATOR = 0;
    private static final DiffUtil.ItemCallback<CapturedNotificationUiModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull CapturedNotificationUiModel oldItem, @NonNull CapturedNotificationUiModel newItem) {
            if (oldItem instanceof CapturedNotificationUiModel.Item && newItem instanceof CapturedNotificationUiModel.Item) {
                CapturedNotificationEntity oldEntity = ((CapturedNotificationUiModel.Item) oldItem).entity;
                CapturedNotificationEntity newEntity = ((CapturedNotificationUiModel.Item) newItem).entity;
                return oldEntity.getNotificationId() == newEntity.getNotificationId();
            } else if (oldItem instanceof CapturedNotificationUiModel.Separator && newItem instanceof CapturedNotificationUiModel.Separator) {
                String oldSeparator = ((CapturedNotificationUiModel.Separator) oldItem).text;
                String newSeparator = ((CapturedNotificationUiModel.Separator) newItem).text;
                return oldSeparator.equals(newSeparator);
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull CapturedNotificationUiModel oldItem, @NonNull CapturedNotificationUiModel newItem) {
            if (oldItem instanceof CapturedNotificationUiModel.Item && newItem instanceof CapturedNotificationUiModel.Item) {
                CapturedNotificationEntity oldEntity = ((CapturedNotificationUiModel.Item) oldItem).entity;
                CapturedNotificationEntity newEntity = ((CapturedNotificationUiModel.Item) newItem).entity;
                return oldEntity.getContent().equals(newEntity.getContent()) &&
                        oldEntity.getPackageName().equals(newEntity.getPackageName()) &&
                        oldEntity.getTitle().equals(newEntity.getTitle()) &&
                        oldEntity.getTime().isEqual(newEntity.getTime());
            } else
                return oldItem instanceof CapturedNotificationUiModel.Separator && newItem instanceof CapturedNotificationUiModel.Separator;
        }
    };

    private final AdapterOnClickListener<CapturedNotificationEntity> clickListener;            //单击监听
    private final AdapterOnLongClickListener<CapturedNotificationEntity> longClickListener;    //长按监听

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        ViewHolderSeparatorTextChipBinding binding;

        public SeparatorViewHolder(@NonNull ViewHolderSeparatorTextChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderCapturedNotificationListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderCapturedNotificationListBinding binding, ViewHolderListener listener) {
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

    public NotificationCaptureListAdapter(
            AdapterOnClickListener<CapturedNotificationEntity> clickListener,
            AdapterOnLongClickListener<CapturedNotificationEntity> longClickListener
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

    @Override
    public boolean isHeader(int position) {
        return getItem(position) instanceof CapturedNotificationUiModel.Separator;
    }

    @Override
    public String getHeaderData(int position, Context context) {
        CapturedNotificationUiModel model = getItem(position);
        if (model instanceof CapturedNotificationUiModel.Separator) {
            return ((CapturedNotificationUiModel.Separator) model).text;
        } else if (model instanceof CapturedNotificationUiModel.Item) {
            return ((CapturedNotificationUiModel.Item) model).entity.getTime().format(CustomDateTimeFormatter.DATE);
        } else {
            return context.getString(R.string.not_applicable);
        }
    }

    @Override
    public int getItemViewType(int position) {
        CapturedNotificationUiModel item = getItem(position);
        if (item instanceof CapturedNotificationUiModel.Item) return TYPE_ITEM;
        return TYPE_SEPARATOR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            ViewHolderCapturedNotificationListBinding binding = ViewHolderCapturedNotificationListBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ItemViewHolder(
                    binding,
                    new ViewHolderListener() {
                        @Override
                        public void onClick(int position, View anchor) {
                            CapturedNotificationUiModel model = getItem(position);
                            if (model instanceof CapturedNotificationUiModel.Item) {
                                clickListener.onClick(((CapturedNotificationUiModel.Item) model).entity, anchor);
                            }
                        }

                        @Override
                        public void onLongClick(int position, View anchor) {
                            CapturedNotificationUiModel model = getItem(position);
                            if (model instanceof CapturedNotificationUiModel.Item) {
                                longClickListener.onLongClick(((CapturedNotificationUiModel.Item) model).entity, anchor);
                            }
                        }

                        @Override
                        public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                        }
                    }
            );
        } else {
            ViewHolderSeparatorTextChipBinding binding = ViewHolderSeparatorTextChipBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new SeparatorViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CapturedNotificationUiModel model = getItem(position);
        if (model instanceof CapturedNotificationUiModel.Separator && holder instanceof SeparatorViewHolder) {
            String relationship = ((CapturedNotificationUiModel.Separator) model).text;
            ((SeparatorViewHolder) holder).binding.separatorText.setText(relationship);
        } else if (model instanceof CapturedNotificationUiModel.Item && holder instanceof ItemViewHolder) {
            CapturedNotificationEntity entity = ((CapturedNotificationUiModel.Item) model).entity;
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.binding.titleText.setText(entity.getTitle());        //通知标题
            itemHolder.binding.contentText.setText(entity.getContent());    //通知内容
            itemHolder.binding.appNameText.setText(entity.getAppName());    //应用名称
            itemHolder.binding.timeText.setText(entity.getTime().format(CustomDateTimeFormatter.TIME)); //时间

            //设置圆角
            setRadius(itemHolder.binding.getRoot(), position);
        }
    }

    /**
     * 设置圆角
     *
     * @param view     需要设置圆角的视图
     * @param position 该视图所处的位置
     */
    private void setRadius(View view, int position) {
        if (position == 0) {    //第0个不参与圆角设置，因为它是日期分隔视图
            return;
        }

        //不需要考虑当前是分隔视图的情况，因为不是Shapable不会执行任何操作
        CapturedNotificationUiModel front = getItem(position - 1);
        if (position == getItemCount() - 1) {   //处理最后一个卡片的圆角
            if (front instanceof CapturedNotificationUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.SINGLE); //前一个是分隔视图，判断为单独类型
            } else {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.BOTTOM); //前一个不是分隔视图，判断为底部类型
            }
        } else {
            CapturedNotificationUiModel behind = getItem(position + 1);

            if (front instanceof CapturedNotificationUiModel.Separator && behind instanceof CapturedNotificationUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.SINGLE); //前后都是分隔视图，判断为单独类型
            } else if (front instanceof CapturedNotificationUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.TOP);    //前一个是分隔但后一个不是，判断为顶部类型
            } else if (behind instanceof CapturedNotificationUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.BOTTOM); //后一个是分隔但前一个不是，判断为底部类型
            } else {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.MIDDLE); //前后都不是分隔视图，判断为中间类型
            }
        }
    }
}
