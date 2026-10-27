package com.sly.coffer.data.save.db.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.TagDao;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.TagGroupUiModel;
import com.sly.coffer.data.save.db.entities.composite.ui.TagListUiModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class TagService {
    final static int NO_GROUP_KEY = -1; //没有分组的分组外键值

    /**
     * 获取分组后的标签数据
     *
     * @param db             数据库实例
     * @param pow            标签作用域标识符，传递0以获取所有作用域内的标签
     * @param exceptedTagIds 被排除的标签的 ID
     * @return 带有分组名称分隔符的标签列表，已按照分组编号分组
     */
    public static Flowable<List<TagGroupUiModel>> getGroupedTagFlowable(@NonNull BookkeepingDb db, int pow, @Nullable long[] exceptedTagIds) {
        TagDao dao = db.tagDao();
        return Flowable.combineLatest(
                dao.getAllTagFlowable(pow, exceptedTagIds),
                dao.getAllTagGroupFlowable(),
                (tagList, groupList) -> {
                    //将标签数据按照分组编号分组
                    Map<Long, List<TagEntity>> groupedTagMap = tagList.stream()
                            .collect(Collectors.groupingBy(
                                    tag -> tag.getGroupId() != null ? tag.getGroupId() : NO_GROUP_KEY,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    //获取实际的标签分组数据
                    Map<Long, String> groupNameMap = groupList.stream()
                            .collect(Collectors.toMap(
                                    TagGroupEntity::getGroupId,
                                    TagGroupEntity::getName
                            ));

                    //生成带有分隔符的标签列表
                    List<TagGroupUiModel> resultList = new ArrayList<>();
                    for (Map.Entry<Long, List<TagEntity>> entry : groupedTagMap.entrySet()) {
                        Long groupId = entry.getKey();
                        String groupName;
                        if (groupId != NO_GROUP_KEY) {
                            groupName = groupNameMap.getOrDefault(groupId, "<未知分组>");
                        } else {
                            groupName = "默认分组";
                        }
                        resultList.add(new TagGroupUiModel.Separator(groupName));

                        resultList.add(new TagGroupUiModel.Item(entry.getValue()));
                    }

                    return resultList;
                }
        );
    }

    /**
     * 获取标签列表数据
     *
     * @param db 数据库实例
     * @return 带有分隔符的标签列表数据，支持响应式更新
     */
    public static Flowable<List<TagListUiModel>> getTagListFlowable(@NonNull BookkeepingDb db) {
        TagDao dao = db.tagDao();
        return Flowable.combineLatest(
                dao.getAllTagFlowable(0, null),
                dao.getAllTagGroupFlowable(),
                (tagList, groupList) -> {
                    //按照分组编号分组
                    Map<Long, TagGroupEntity> groupMap = groupList.stream()
                            .collect(Collectors.toMap(
                                    TagGroupEntity::getGroupId,
                                    Function.identity()
                            ));

                    //构建标签分组 Map，并先填入已有的分组
                    Map<Long, List<TagEntity>> groupedTagMap = groupList.stream()
                            .collect(Collectors.toMap(
                                    TagGroupEntity::getGroupId,
                                    group -> new ArrayList<>()
                            ));

                    //将有分组的标签填入分组 Map
                    List<TagEntity> tagWithGroupList = tagList.stream()
                            .filter(tag -> tag.getGroupId() != null)
                            .collect(Collectors.toList());
                    for (TagEntity tag : tagWithGroupList) {
                        Long groupId = tag.getGroupId();
                        List<TagEntity> listInMap = groupedTagMap.get(groupId);
                        if (listInMap != null) {
                            listInMap.add(tag);
                        } else {
                            List<TagEntity> newList = new ArrayList<>();
                            newList.add(tag);
                            groupedTagMap.put(groupId, newList);
                        }
                    }

                    //填充没有分组的标签
                    List<TagListUiModel> resultList = tagList.stream()
                            .filter(tag -> tag.getGroupId() == null)
                            .map(TagListUiModel.Item::new)
                            .collect(Collectors.toList());

                    //填充有分组的标签
                    for (Map.Entry<Long, List<TagEntity>> entry : groupedTagMap.entrySet()) {
                        Long groupId = entry.getKey();

                        //插入分组
                        TagGroupEntity group = groupMap.get(groupId);
                        if (group != null) {
                            resultList.add(new TagListUiModel.Group(group));
                        } else {
                            TagGroupEntity unknowGroup = new TagGroupEntity("<未知分组>");
                            unknowGroup.setGroupId(groupId);
                            resultList.add(new TagListUiModel.Group(unknowGroup));
                        }

                        //插入标签
                        List<TagListUiModel.Item> itemList = entry.getValue().stream()
                                .map(TagListUiModel.Item::new)
                                .collect(Collectors.toList());
                        resultList.addAll(itemList);
                    }

                    return resultList;
                }
        );
    }

    /**
     * 添加标签
     *
     * @param tag       新添加的标签实体
     * @param groupName 用户输入的分组名称
     * @param db        数据库实例
     * @return 是否完成
     */
    public static Completable addTag(TagEntity tag, String groupName, BookkeepingDb db) {
        return Completable.defer(() -> {
            db.tagDao().addTag(tag, groupName);
            return Completable.complete();
        });
    }

    /**
     * 修改标签
     *
     * @param tag       修改后的标签实体
     * @param groupName 用户输入的分组名称
     * @param db        数据库实例
     * @return 是否完成
     */
    public static Completable modifyTag(TagEntity tag, String groupName, BookkeepingDb db) {
        return Completable.defer(() -> {
            db.tagDao().modifyTag(tag, groupName);
            return Completable.complete();
        });
    }

    /**
     * 合并标签分组
     *
     * @param mergedGroup 被合并的标签分组
     * @param targetGroup 合并到的标签分组
     * @param db          数据库实例
     * @return 是否完成
     */
    public static Completable mergeTagGroup(TagGroupEntity mergedGroup, TagGroupEntity targetGroup, BookkeepingDb db) {
        return Completable.defer(() -> {
            db.tagDao().mergeGroup(mergedGroup, targetGroup);
            return Completable.complete();
        });
    }

    /**
     * 合并标签
     *
     * @param mergedTag 被合并的标签
     * @param targetTag 合并到的目标标签
     * @param db        数据库实例
     * @return 是否完成
     */
    public static Completable mergeTag(TagEntity mergedTag, TagEntity targetTag, BookkeepingDb db) {
        return Completable.defer(() -> {
            db.tagDao().mergeTag(mergedTag, targetTag);
            return Completable.complete();
        });
    }
}
