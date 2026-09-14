package com.sly.coffer.ui.pages.report;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.databinding.ViewHolderRunningAccountSelectListBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderAdapter;

import java.util.List;
import java.util.Locale;

public class AccountSelectListAdapter extends ListAdapter<AccountUiModel, RecyclerView.ViewHolder>
        implements StickyHeaderAdapter<String> {
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SEPARATOR = 0;
    public static final DiffUtil.ItemCallback<AccountUiModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AccountUiModel oldItem, @NonNull AccountUiModel newItem) {
            if (oldItem instanceof AccountUiModel.Item && newItem instanceof AccountUiModel.Item) {
                AccountUiModel.Item oldI = (AccountUiModel.Item) oldItem;
                AccountUiModel.Item newI = (AccountUiModel.Item) newItem;
                return oldI.entity.getAccountId() == newI.entity.getAccountId();
            } else if (oldItem instanceof AccountUiModel.Separator && newItem instanceof AccountUiModel.Separator) {
                AccountUiModel.Separator oldS = (AccountUiModel.Separator) oldItem;
                AccountUiModel.Separator newS = (AccountUiModel.Separator) newItem;
                return oldS.text.equals(newS.text);
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull AccountUiModel oldItem, @NonNull AccountUiModel newItem) {
            if (oldItem instanceof AccountUiModel.Item && newItem instanceof AccountUiModel.Item) {
                AccountUiModel.Item oldI = (AccountUiModel.Item) oldItem;
                AccountUiModel.Item newI = (AccountUiModel.Item) newItem;
                AccountEntity oldAccount = oldI.entity;
                AccountEntity newAccount = newI.entity;
                return oldAccount.getAmount() == newAccount.getAmount() &&
                        oldAccount.getRemark().equals(newAccount.getRemark()) &&
                        oldAccount.getType() == newAccount.getType() &&
                        oldAccount.getDateTime().isEqual(newAccount.getDateTime());
            } else
                return oldItem instanceof AccountUiModel.Separator && newItem instanceof AccountUiModel.Separator;
        }
    };
    private SelectionTracker<Long> selectionTracker;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderRunningAccountSelectListBinding binding;

        public ItemViewHolder(@NonNull ViewHolderRunningAccountSelectListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置点击监听
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));

            //设置触摸监听
            AppearanceHelper.attachMorphAnimation(binding.getRoot());
        }

        /**
         * 为 Selection 库提供信息
         *
         * @return Selection 库的 Item 信息
         */
        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return getBindingAdapterPosition();
                }

                @Nullable
                @Override
                public Long getSelectionKey() {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return null;

                    if (getBindingAdapter() instanceof AccountSelectListAdapter) {
                        List<AccountUiModel> currentList = ((AccountSelectListAdapter) getBindingAdapter()).getCurrentList();
                        if (currentList.get(pos) instanceof AccountUiModel.Item) {
                            return pos < getBindingAdapter().getItemCount() ?
                                    ((AccountUiModel.Item) currentList.get(pos)).entity.getAccountId() :
                                    null;
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            };
        }
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        ViewHolderSeparatorTextChipBinding binding;

        public SeparatorViewHolder(@NonNull ViewHolderSeparatorTextChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 为 Selection 库提供信息
         *
         * @return Selection 库的 Item 信息
         */
        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return getBindingAdapterPosition();
                }

                @Nullable
                @Override
                public Long getSelectionKey() {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return null;

                    if (getBindingAdapter() instanceof AccountSelectListAdapter) {
                        List<AccountUiModel> currentList = ((AccountSelectListAdapter) getBindingAdapter()).getCurrentList();
                        if (currentList.get(pos) instanceof AccountUiModel.Separator) {
                            return pos < getBindingAdapter().getItemCount() ?
                                    -Math.abs((long) ((AccountUiModel.Separator) currentList.get(pos)).text.hashCode()) :
                                    null;
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            };
        }
    }

    public AccountSelectListAdapter() {
        super(ITEM_CALLBACK);

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

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    @Override
    public boolean isHeader(int position) {
        AccountUiModel model = getItem(position);
        return model instanceof AccountUiModel.Separator;
    }

    @Override
    public String getHeaderData(int position, Context context) {
        AccountUiModel model = getItem(position);
        if (model instanceof AccountUiModel.Separator) {
            return ((AccountUiModel.Separator) model).text;
        } else if (model instanceof AccountUiModel.Item) {
            return ((AccountUiModel.Item) model).entity.getDateTime().format(CustomDateTimeFormatter.DATE_WITH_WEEK);
        } else {
            return context.getString(R.string.not_applicable);
        }
    }

    @Override
    public int getItemViewType(int position) {
        AccountUiModel item = getItem(position);
        if (item instanceof AccountUiModel.Item) return TYPE_ITEM;
        return TYPE_SEPARATOR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            ViewHolderRunningAccountSelectListBinding binding = ViewHolderRunningAccountSelectListBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ItemViewHolder(
                    binding,
                    new ViewHolderListener() {
                        @Override
                        public void onClick(int pos, View anchor) {
                            AccountUiModel model = getItem(pos);
                            if (model instanceof AccountUiModel.Item && selectionTracker != null) {
                                selectionTracker.select(((AccountUiModel.Item) model).entity.getAccountId());
                            }
                        }

                        @Override
                        public void onLongClick(int pos, View anchor) {
                        }

                        @Override
                        public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                        }
                    });
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
        AccountUiModel model = getItem(position);
        if (model instanceof AccountUiModel.Item && holder instanceof ItemViewHolder) {
            AccountEntity account = ((AccountUiModel.Item) model).entity;
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            //选择状态
            itemHolder.binding.checkedText.setChecked(
                    selectionTracker != null && selectionTracker.isSelected(account.getAccountId())
            );

            //获取流水数据
            String type = AccountType.values()[account.getType()].getTitle();
            String datetime = account.getDateTime().format(CustomDateTimeFormatter.TIME);
            String remark = account.getRemark();
            double amount = account.getAmount();

            //生成自动记账种类字符串
            String autoBookkeepingType;
            int autoTag = account.getAutoTag();
            if (autoTag == AutoBookkeepingType.NOTIFICATION.ordinal()) {
                autoBookkeepingType = "通知记账·";
            } else if (autoTag == AutoBookkeepingType.ACCESSIBILITY.ordinal()) {
                autoBookkeepingType = "无障碍记账·";
            } else {
                autoBookkeepingType = "";
            }

            //初始化文本视图
            itemHolder.binding.amountText.setText(TextHelper.abbreviate(amount, 1));
            itemHolder.binding.remarkText.setText(remark.isEmpty() ? "<无备注>" : remark);
            String typeTimeAutoTag = String.format(
                    Locale.getDefault(),
                    "%s%s·%s",
                    autoBookkeepingType, type, datetime
            );
            itemHolder.binding.typeTimeAutoTagText.setText(typeTimeAutoTag);

            //设置圆角
            setRadius(itemHolder.binding.getRoot(), holder.getBindingAdapterPosition());
        } else if (model instanceof AccountUiModel.Separator && holder instanceof SeparatorViewHolder) {
            SeparatorViewHolder separatorHolder = (SeparatorViewHolder) holder;
            separatorHolder.binding.separatorText.setText(((AccountUiModel.Separator) model).text);
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
        AccountUiModel front = getItem(position - 1);
        if (position == getItemCount() - 1) {   //处理最后一个卡片的圆角
            if (front instanceof AccountUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.SINGLE); //前一个是分隔视图，判断为单独类型
            } else {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.BOTTOM); //前一个不是分隔视图，判断为底部类型
            }
        } else {
            AccountUiModel behind = getItem(position + 1);

            if (front instanceof AccountUiModel.Separator && behind instanceof AccountUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.SINGLE); //前后都是分隔视图，判断为单独类型
            } else if (front instanceof AccountUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.TOP);    //前一个是分隔但后一个不是，判断为顶部类型
            } else if (behind instanceof AccountUiModel.Separator) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.BOTTOM); //后一个是分隔但前一个不是，判断为底部类型
            } else {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.MIDDLE); //前后都不是分隔视图，判断为中间类型
            }
        }
    }
}
