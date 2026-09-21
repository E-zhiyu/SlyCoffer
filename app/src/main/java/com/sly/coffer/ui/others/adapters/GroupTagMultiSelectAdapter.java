package com.sly.coffer.ui.others.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.sly.coffer.auxiliary.interfaces.adapter.AdapterOnChipCheckedChangeListener;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.TagGroupUiModel;
import com.sly.coffer.databinding.ViewHolderGroupTagItemBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextviewBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupTagMultiSelectAdapter extends ListAdapter<TagGroupUiModel, RecyclerView.ViewHolder> {
    private static final DiffUtil.ItemCallback<TagGroupUiModel> ITEM_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull TagGroupUiModel oldItem, @NonNull TagGroupUiModel newItem) {
            if (oldItem instanceof TagGroupUiModel.Item && newItem instanceof TagGroupUiModel.Item) {
                TagGroupUiModel.Item oldI = (TagGroupUiModel.Item) oldItem;
                TagGroupUiModel.Item newI = (TagGroupUiModel.Item) newItem;
                List<Long> oldItemRoleIdList = oldI.tagList.stream()
                        .map(TagEntity::getTagId)
                        .collect(Collectors.toList());
                List<Long> newItemRoleIdList = newI.tagList.stream()
                        .map(TagEntity::getTagId)
                        .collect(Collectors.toList());
                return oldItemRoleIdList.equals(newItemRoleIdList);
            } else if (oldItem instanceof TagGroupUiModel.Separator && newItem instanceof TagGroupUiModel.Separator) {
                TagGroupUiModel.Separator oldS = (TagGroupUiModel.Separator) oldItem;
                TagGroupUiModel.Separator newS = (TagGroupUiModel.Separator) newItem;
                return oldS.text.equals(newS.text);
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull TagGroupUiModel oldItem, @NonNull TagGroupUiModel newItem) {
            if (oldItem instanceof TagGroupUiModel.Item && newItem instanceof TagGroupUiModel.Item) {
                return true;
            } else
                return oldItem instanceof TagGroupUiModel.Separator && newItem instanceof TagGroupUiModel.Separator;
        }
    };
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SEPARATOR = 0;
    private final AdapterOnChipCheckedChangeListener<TagEntity> checkedChangeListener;
    private final HashSet<Long> checkedTagSet = new HashSet<>();

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ViewHolderGroupTagItemBinding binding;

        public ItemViewHolder(@NonNull ViewHolderGroupTagItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 刷新 ChipGroup 中的标签 Chip
         *
         * @param tagList         标签列表
         * @param checkedTagIdSet 默认选中的标签 ID 的集合
         * @param listener        标签点击后触发的监听器
         */
        public void refreshChipGroup(@NonNull List<TagEntity> tagList, Set<Long> checkedTagIdSet, AdapterOnChipCheckedChangeListener<TagEntity> listener) {
            //删除之前的视图
            binding.chipGroup.removeAllViews();

            //添加新的视图
            for (TagEntity tag : tagList) {
                //实例化 Chip
                Chip chip = new Chip(binding.getRoot().getContext());
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setChecked(checkedTagIdSet.contains(tag.getTagId()));

                //设置显示名称
                chip.setText(tag.getName());

                //添加到视图
                binding.chipGroup.addView(chip);

                //绑定点击监听器
                chip.setOnCheckedChangeListener((compoundButton, b) ->
                        listener.onCheckedChange(tag, b, binding.getRoot())
                );
            }
        }
    }

    public static class GroupRoleSeparatorViewHolder extends RecyclerView.ViewHolder {
        ViewHolderSeparatorTextviewBinding binding;

        public GroupRoleSeparatorViewHolder(@NonNull ViewHolderSeparatorTextviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public GroupTagMultiSelectAdapter(Set<Long> chekcedTagSet, AdapterOnChipCheckedChangeListener<TagEntity> checkedChangeListener) {
        super(ITEM_CALLBACK);
        this.checkedChangeListener = checkedChangeListener;
        this.checkedTagSet.addAll(chekcedTagSet);
    }

    @Override
    public int getItemViewType(int position) {
        TagGroupUiModel item = getItem(position);
        if (item instanceof TagGroupUiModel.Item) return TYPE_ITEM;
        return TYPE_SEPARATOR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            ViewHolderGroupTagItemBinding binding = ViewHolderGroupTagItemBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ItemViewHolder(binding);
        } else {
            ViewHolderSeparatorTextviewBinding binding = ViewHolderSeparatorTextviewBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new GroupRoleSeparatorViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TagGroupUiModel dataItem = getItem(position);
        if (dataItem instanceof TagGroupUiModel.Item && holder instanceof ItemViewHolder) {
            TagGroupUiModel.Item item = (TagGroupUiModel.Item) dataItem;
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.refreshChipGroup(item.tagList, checkedTagSet, checkedChangeListener);
        } else if (dataItem instanceof TagGroupUiModel.Separator && holder instanceof GroupRoleSeparatorViewHolder) {
            TagGroupUiModel.Separator separator = (TagGroupUiModel.Separator) dataItem;
            GroupRoleSeparatorViewHolder separatorHolder = (GroupRoleSeparatorViewHolder) holder;

            separatorHolder.binding.text.setText(separator.text);
        }
    }
}
