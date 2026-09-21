package com.sly.coffer.ui.pages.tag;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnLongClickListener;
import com.sly.coffer.auxiliary.interfaces.adapter.ViewHolderListener;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.TagListUiModel;
import com.sly.coffer.databinding.ViewHolderTagListBinding;
import com.sly.coffer.databinding.ViewHolderTagListGroupBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

public class TagListAdapter extends ListAdapter<TagListUiModel, RecyclerView.ViewHolder> {
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SEPARATOR = 0;
    private final AdapterOnClickListener<TagEntity> itemClickListener;           //点击监听器
    private final AdapterOnLongClickListener<TagEntity> itemLongClickListener;   //长按监听器
    private final AdapterOnLongClickListener<TagGroupEntity> separatorLongClickListener; //分隔符长按监听器
    private static final DiffUtil.ItemCallback<TagListUiModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull TagListUiModel oldItem, @NonNull TagListUiModel newItem) {
            if (oldItem instanceof TagListUiModel.Item && newItem instanceof TagListUiModel.Item) {
                TagListUiModel.Item oldI = (TagListUiModel.Item) oldItem;
                TagListUiModel.Item newI = (TagListUiModel.Item) newItem;
                return oldI.entity.getTagId() == newI.entity.getTagId();
            } else if (oldItem instanceof TagListUiModel.Group && newItem instanceof TagListUiModel.Group) {
                TagListUiModel.Group oldS = (TagListUiModel.Group) oldItem;
                TagListUiModel.Group newS = (TagListUiModel.Group) newItem;
                return oldS.group.getGroupId() == newS.group.getGroupId();
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull TagListUiModel oldItem, @NonNull TagListUiModel newItem) {
            if (oldItem instanceof TagListUiModel.Item && newItem instanceof TagListUiModel.Item) {
                TagListUiModel.Item oldI = (TagListUiModel.Item) oldItem;
                TagListUiModel.Item newI = (TagListUiModel.Item) newItem;
                return oldI.entity.getName().equals(newI.entity.getName()) &&
                        oldI.entity.getScope() == newI.entity.getScope();
            } else if (oldItem instanceof TagListUiModel.Group && newItem instanceof TagListUiModel.Group) {
                TagListUiModel.Group oldS = (TagListUiModel.Group) oldItem;
                TagListUiModel.Group newS = (TagListUiModel.Group) newItem;
                return oldS.group.getName().equals(newS.group.getName());
            } else {
                return false;
            }
        }
    };

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        ViewHolderTagListBinding binding;

        public TagViewHolder(@NonNull ViewHolderTagListBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸动画
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //设置点击监听
            binding.getRoot().setOnClickListener(view -> listener.onClick(getBindingAdapterPosition(), binding.getRoot()));

            //设置长按监听
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onLongClick(getBindingAdapterPosition(), view);
                return true;
            });
        }
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        ViewHolderTagListGroupBinding binding;

        public SeparatorViewHolder(@NonNull ViewHolderTagListGroupBinding binding, ViewHolderListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            //设置触摸监听
            AppearanceHelper.attachMorphAnimation(binding.getRoot());

            //设置长按监听
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onLongClick(getBindingAdapterPosition(), binding.getRoot());
                return true;
            });
        }
    }

    /***
     * @param itemClickListener 点击监听器
     * @param itemLongClickListener 长按监听器
     */
    public TagListAdapter(
            AdapterOnClickListener<TagEntity> itemClickListener,
            AdapterOnLongClickListener<TagEntity> itemLongClickListener, AdapterOnLongClickListener<TagGroupEntity> separatorLongClickListener
    ) {
        super(ITEM_CALLBACK);
        this.itemClickListener = itemClickListener;
        this.itemLongClickListener = itemLongClickListener;
        this.separatorLongClickListener = separatorLongClickListener;

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
    public int getItemViewType(int position) {
        TagListUiModel item = getItem(position);
        if (item instanceof TagListUiModel.Item) return TYPE_ITEM;
        return TYPE_SEPARATOR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            ViewHolderTagListBinding binding = ViewHolderTagListBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new TagViewHolder(
                    binding,
                    new ViewHolderListener() {
                        @Override
                        public void onClick(int position, View anchor) {
                            TagListUiModel tag = getItem(position);
                            if (tag instanceof TagListUiModel.Item) {
                                itemClickListener.onClick(((TagListUiModel.Item) tag).entity, anchor);
                            }
                        }

                        @Override
                        public void onLongClick(int position, View view) {
                            TagListUiModel tag = getItem(position);
                            if (tag instanceof TagListUiModel.Item) {
                                itemLongClickListener.onLongClick(((TagListUiModel.Item) tag).entity, view);
                            }
                        }

                        @Override
                        public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                        }
                    }
            );
        } else {
            ViewHolderTagListGroupBinding binding = ViewHolderTagListGroupBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new SeparatorViewHolder(
                    binding,
                    new ViewHolderListener() {
                        @Override
                        public void onClick(int pos, View anchor) {
                        }

                        @Override
                        public void onLongClick(int pos, View anchor) {
                            TagListUiModel group = getItem(pos);
                            if (group instanceof TagListUiModel.Group) {
                                separatorLongClickListener.onLongClick(((TagListUiModel.Group) group).group, anchor);
                            }
                        }

                        @Override
                        public void onCheckedChange(int pos, boolean finalStat, View anchor) {
                        }
                    }
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TagListUiModel model = getItem(position);

        if (model instanceof TagListUiModel.Item && holder instanceof TagViewHolder) {
            TagEntity tag = ((TagListUiModel.Item) model).entity;
            TagViewHolder itemHolder = (TagViewHolder) holder;

            //标签名称
            itemHolder.binding.nameText.setText(tag.getName());

            //标签作用域
            int scope = tag.getScope();
            StringBuilder scopeBuilder = new StringBuilder();
            for (AccountType accountType : AccountType.values()) {
                int pow = (int) Math.pow(2, accountType.ordinal());
                if ((scope & pow) == 0) {
                    if (scopeBuilder.length() > 0) {
                        scopeBuilder.append("、");
                    }

                    scopeBuilder.append(accountType.getTitle());
                }
            }
            itemHolder.binding.scopeText.setText(scopeBuilder.length() > 0 ? scopeBuilder.toString() : "<无作用域>");

            //设置圆角
            setRadius(itemHolder.binding.getRoot(), position);
        } else if (model instanceof TagListUiModel.Group && holder instanceof SeparatorViewHolder) {
            String text = ((TagListUiModel.Group) model).group.getName();
            SeparatorViewHolder sHolder = (SeparatorViewHolder) holder;

            sHolder.binding.nameText.setText(text);
            setRadius(sHolder.binding.getRoot(), position);
        }
    }

    /**
     * 设置圆角
     *
     * @param view     需要设置圆角的视图
     * @param position 该视图所处的位置
     */
    private void setRadius(@NonNull View view, int position) {
        Context context = view.getContext();
        TagListUiModel model = getItem(position);

        if (model instanceof TagListUiModel.Group) {
            if (position == getItemCount() - 1 || getItem(position + 1) instanceof TagListUiModel.Group) {
                AppearanceHelper.setRadius(
                        context,
                        view,
                        AppearanceHelper.MEDIUM_CARD_RADIUS,
                        AppearanceHelper.MEDIUM_CARD_RADIUS,
                        AppearanceHelper.MEDIUM_CARD_RADIUS,
                        AppearanceHelper.MEDIUM_CARD_RADIUS
                );
            } else {
                AppearanceHelper.setRadius(
                        context,
                        view,
                        AppearanceHelper.MEDIUM_CARD_RADIUS,
                        AppearanceHelper.MEDIUM_CARD_RADIUS,
                        AppearanceHelper.SMALL_CARD_RADIUS,
                        AppearanceHelper.SMALL_CARD_RADIUS
                );
            }
        } else if (model instanceof TagListUiModel.Item) {
            if (position == getItemCount() - 1 || getItem(position + 1) instanceof TagListUiModel.Group) {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.BOTTOM);
            } else {
                AppearanceHelper.setRadiusStyle(view, RadiusStyle.MIDDLE);
            }
        }
    }
}
