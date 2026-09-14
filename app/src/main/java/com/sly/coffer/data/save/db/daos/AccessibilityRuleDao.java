package com.sly.coffer.data.save.db.daos;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.composite.AccessibilityRuleWithDetailModel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AccessibilityRuleDao {
    /**
     * 获取无障碍规则总数
     *
     * @return 无障碍规则总数，支持响应式更新
     */
    @Query("SELECT COUNT(*) FROM accessibilityRules")
    Flowable<Integer> getAccessibilityRuleCountFlowable();

    @Transaction
    @Query("SELECT * FROM accessibilityRules WHERE enabled = 1")
    Flowable<List<AccessibilityRuleWithDetailModel>> getOpenedAccessibilityRuleWithDetailFlowable();

    /**
     * 删除无障碍规则
     *
     * @param rule 需要删除的无障碍规则
     * @return 是否完成
     */
    @Delete
    Completable deleteAccessibilityRule(AccessibilityRuleEntity rule);

    /**
     * 读取所有无障碍规则
     *
     * @return 无障碍规则列表，支持响应式更新
     */
    @Query("SELECT * FROM accessibilityRules ORDER BY type")
    Flowable<List<AccessibilityRuleEntity>> getAllAccessibilityRuleFlowable();

    /**
     * 设置通知规则是否启用
     *
     * @param enabled 是否启用
     * @param ruleId  需要更新的规则的 ID
     * @return 是否完成
     */
    @Query("UPDATE accessibilityRules SET enabled = :enabled WHERE ruleId = :ruleId")
    Completable setRuleEnabled(boolean enabled, long ruleId);

    /**
     * 通过规则编号获取规则
     *
     * @param id 编号
     * @return 该编号对应的规则
     */
    @Transaction
    @Query("SELECT * FROM accessibilityRules WHERE ruleId = :id")
    Single<Optional<AccessibilityRuleWithDetailModel>> getRuleWithDetailById(long id);

    /**
     * 插入一条新的无障碍规则
     *
     * @param rule 新的无障碍规则
     * @return 自动分配的编号
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertAccessibilityRule(AccessibilityRuleEntity rule);

    /**
     * 插入无障碍规则的转账账户数据
     *
     * @param transfer 转账账户数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAccessibilityTransfer(AccessibilityRuleTransferEntity transfer);

    /**
     * 插入无障碍规则的标签映射
     *
     * @param tagList 标签映射列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAccessibilityTagRef(List<AccessibilityRuleTagRefEntity> tagList);

    /**
     * 插入无障碍关键词组合
     *
     * @param list 关键词组合列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertKeywordGroup(List<AccessibilityRuleKeywordGroupEntity> list);

    /**
     * 添加无障碍规则事务
     *
     * @param rule             新规则
     * @param transfer         新规则对应的转账数据
     * @param keywordGroupList 关键词组合列表
     * @param tagIdList        新规则对应的标签编号列表
     */
    @Transaction
    default void addAccessibilityRule(
            AccessibilityRuleEntity rule,
            AccessibilityRuleTransferEntity transfer,
            List<AccessibilityRuleKeywordGroupEntity> keywordGroupList,
            List<Long> tagIdList
    ) {
        long ruleId = insertAccessibilityRule(rule);

        //转账账户数据
        if (rule.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setRuleId(ruleId);
            insertAccessibilityTransfer(transfer);
        }

        //关键词组合
        for (AccessibilityRuleKeywordGroupEntity group : keywordGroupList) {
            group.setRuleId(ruleId);
        }
        insertKeywordGroup(keywordGroupList);

        //标签数据
        List<AccessibilityRuleTagRefEntity> tagRefList = tagIdList.stream()
                .map(id -> new AccessibilityRuleTagRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertAccessibilityTagRef(tagRefList);
    }

    /**
     * 通过编号查询规则
     *
     * @param ruleId 规则编号
     * @return 该编号对应的规则
     */
    @Query("SELECT * FROM accessibilityRules WHERE ruleId = :ruleId")
    Optional<AccessibilityRuleEntity> getAccessibilityRuleOptionalById(long ruleId);

    /**
     * 更新无障碍规则
     *
     * @param rule 更新后的无障碍规则
     */
    @Update
    void updateAccessibilityRule(AccessibilityRuleEntity rule);

    /**
     * 通过规则编号删除转账账户数据
     *
     * @param ruleId 规则编号
     */
    @Query("DELETE FROM accessibilityRuleTransfers WHERE ruleId = :ruleId")
    void deleteAccessibilityRuleTransferByRuleId(long ruleId);

    /**
     * 通过规则编号删除标签映射
     *
     * @param ruleId 规则编号
     */
    @Query("DELETE FROM accessibilityRuleTagRef WHERE ruleId = :ruleId")
    void deleteAccessibilityRuleTagRefByRuleId(long ruleId);

    /**
     * 根据无障碍规则编号删除关键词组合
     *
     * @param ruleId 需要删除的组合所属的无障碍规则编号
     */
    @Query("DELETE FROM accessibilityRuleKeywordGroups WHERE ruleId = :ruleId")
    void deleteKeywordGroupByRuleId(long ruleId);

    /**
     * 修改无障碍规则
     *
     * @param rule             修改后的无障碍规则
     * @param transfer         转账账户
     * @param keywordGroupList 关键词组合列表
     * @param tagIdList        标签编号列表
     */
    @Transaction
    default void modifyAccessibilityRule(
            @NonNull AccessibilityRuleEntity rule,
            AccessibilityRuleTransferEntity transfer,
            List<AccessibilityRuleKeywordGroupEntity> keywordGroupList,
            List<Long> tagIdList
    ) {
        long ruleId = rule.getRuleId();

        //获取旧数据
        Optional<AccessibilityRuleEntity> optional = getAccessibilityRuleOptionalById(ruleId);
        optional.ifPresent(oldRule -> rule.setEnabled(oldRule.isEnabled()));

        //更新规则
        updateAccessibilityRule(rule);

        //转账账户
        deleteAccessibilityRuleTransferByRuleId(ruleId);
        if (rule.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setRuleId(ruleId);
            insertAccessibilityTransfer(transfer);
        }

        //关键词组合
        deleteKeywordGroupByRuleId(ruleId);
        for (AccessibilityRuleKeywordGroupEntity group : keywordGroupList) {
            group.setRuleId(ruleId);
        }
        insertKeywordGroup(keywordGroupList);

        //标签数据
        deleteAccessibilityRuleTagRefByRuleId(ruleId);
        List<AccessibilityRuleTagRefEntity> tagRefList = tagIdList.stream()
                .map(id -> new AccessibilityRuleTagRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertAccessibilityTagRef(tagRefList);
    }

    /**
     * 插入拾取的视图记录
     *
     * @param view 需要插入的视图记录
     * @return 自动分配的编号
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPickedPage(PickedPageEntity view);

    /**
     * 获取拾取的视图总数
     *
     * @return 拾取的视图总数，支持响应式更新
     */
    @Query("SELECT COUNT(*) FROM pickedPages")
    Flowable<Integer> getPickedPageCountFlowable();

    /**
     * 通过包名、活动名和视图 ID 获取旧视图数据
     *
     * @param packageName  应用包名
     * @param activityName 视图所在界面的活动名
     * @return 旧视图数据
     */
    @Query("SELECT * FROM pickedPages WHERE packageName = :packageName AND activityName = :activityName")
    Optional<PickedPageEntity> getOldPickedPage(String packageName, String activityName);

    /**
     * 更新拾取的视图
     *
     * @param view 更新后的拾取的视图
     */
    @Update
    void updatePickedPage(PickedPageEntity view);

    /**
     * 更新拾取的视图
     *
     * @param view 更新后的拾取的视图
     * @return 是否完成
     */
    @Update
    Completable updatePickedPageCompletable(PickedPageEntity view);

    /**
     * 添加拾取的视图
     *
     * @param view 需要添加的视图记录
     * @return 为新视图记录分配的编号
     */
    @Transaction
    default long addPickedPage(@NonNull PickedPageEntity view) {
        String packageName = view.getPackageName();
        String activityName = view.getActivityName();

        //获取旧数据
        Optional<PickedPageEntity> optional = getOldPickedPage(packageName, activityName);

        //写入数据
        if (optional.isEmpty()) {
            return insertPickedPage(view);
        } else {
            PickedPageEntity oldView = optional.get();
            view.setId(oldView.getId());
            view.setRemark(oldView.getRemark());
            updatePickedPage(view);
            return oldView.getId();
        }
    }

    /**
     * s拿出拾取的视图
     *
     * @param view 需要删除的视图
     * @return 是否完成
     */
    @Delete
    Completable deletePickedPageCompletable(PickedPageEntity view);

    /**
     * 删除所有拾取的界面
     *
     * @return 是否完成
     */
    @Query("DELETE FROM pickedPages")
    Completable deleteAllPickedPageCompletable();


    /**
     * 获取所有符合搜索条件的拾取的视图
     *
     * @param keyword         搜索关键词
     * @param useSearchFilter 是否需要过滤搜索条件
     * @return 拾取的视图列表，支持响应式更新
     */
    @Query(
            "SELECT * FROM pickedPages " +
                    "WHERE :useSearchFilter = 0 " +
                    "OR remark LIKE '%' || :keyword || '%' ESCAPE '/' " +
                    "OR activityName LIKE '%' || :keyword || '%' ESCAPE '/' " +
                    "ORDER BY packageName, dateTime DESC"
    )
    Flowable<List<PickedPageEntity>> getAllPickedPageFlowable(String keyword, int useSearchFilter);

    /**
     * 获取所有拾取的视图
     *
     * @return 所有已拾取的视图，支持响应式更新
     */
    @Query("SELECT * FROM pickedPages ORDER BY packageName, dateTime DESC")
    Flowable<List<PickedPageEntity>> getAllPickedPageFlowable();

    /**
     * 通过 ID 获取拾取的视图
     *
     * @param id 拾取的视图的 ID
     * @return 该 ID 对应的拾取的视图
     */
    @Query("SELECT * FROM pickedPages WHERE id = :id")
    Single<Optional<PickedPageEntity>> getPickedPageSingleById(long id);
}
