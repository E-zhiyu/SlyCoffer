package com.sly.coffer.data.save.db.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleUnionModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NotificationRuleDao {
    /**
     * 获取通知规则数量
     *
     * @return 通知规则数量，支持响应式更新
     */
    @Query("SELECT COUNT(*) FROM notificationRules")
    Flowable<Integer> getNotificationRuleCountFlowable();

    /**
     * 获取所有通知规则
     *
     * @return 由通知规则组成的列表，支持响应式更新
     */
    @Query("SELECT * FROM notificationRules ORDER BY type")
    Flowable<List<NotificationRuleEntity>> getAllNotificationRuleFlowable();

    /**
     * 通过规则 ID 查询通知规则的详细数据
     *
     * @param ruleId 需要查询的规则编号
     * @return 通知规则的详细数据
     */
    @Transaction
    @Query("SELECT * FROM notificationRules WHERE ruleId = :ruleId")
    Single<Optional<NotificationRuleUnionModel>> getNotificationRuleWithDetailSingleById(long ruleId);

    /**
     * 通过编号获取规则数据
     *
     * @param id 规则编号
     * @return 该编号对应的规则数据
     */
    @Query("SELECT * FROM notificationRules WHERE ruleId = :id")
    Optional<NotificationRuleEntity> getNotificationRuleOptionalById(long id);

    /**
     * 获取已启用的通知规则
     *
     * @return 已启用的通知规则，带有标签和转账账户等信息
     */
    @Transaction
    @Query("SELECT * FROM notificationRules WHERE enabled = 1")
    Flowable<List<NotificationRuleUnionModel>> getEnabledNotificationRuleFlowable();

    /**
     * 插入通知规则
     *
     * @param rule 需要插入的通知规则
     * @return 插入后分配的主键值
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insertNotificationRule(NotificationRuleEntity rule);

    /**
     * 插入通知规则的转账账户记录
     *
     * @param transfer 需要插入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertNotificationTransfer(NotificationRuleTransferEntity transfer);

    /**
     * 插入通知规则与标签的映射关系数据
     *
     * @param refList 需要插入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertNotificationTagRef(List<NotificationRuleTagRefEntity> refList);

    /**
     * 插入通知规则与分组的映射关系
     *
     * @param refList 需要插入的映射关系数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertNotificationRuleGroupRef(List<NotificationRuleGroupRefEntity> refList);

    /**
     * 新增通知规则事务
     *
     * @param rule        新增的通知规则
     * @param transfer    通知规则的转账账户数据
     * @param tagIdList   标签编号列表
     * @param groupIdList 规则分组编号列表
     */
    @Transaction
    default void addNotificationRule(
            NotificationRuleEntity rule,
            NotificationRuleTransferEntity transfer,
            List<Long> tagIdList,
            List<Long> groupIdList
    ) {
        if (rule == null) return;
        long ruleId = insertNotificationRule(rule);

        //转入转出账户
        if (rule.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setRuleId(ruleId);
            insertNotificationTransfer(transfer);
        }

        //标签
        List<NotificationRuleTagRefEntity> tagRefList = tagIdList.stream()
                .map(id -> new NotificationRuleTagRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertNotificationTagRef(tagRefList);

        //分组
        List<NotificationRuleGroupRefEntity> groupRefList = groupIdList.stream()
                .map(id -> new NotificationRuleGroupRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertNotificationRuleGroupRef(groupRefList);
    }

    /**
     * 更新通知规则
     *
     * @param rule 更新后的通知规则数据
     */
    @Update
    void updateNotificationRule(NotificationRuleEntity rule);

    /**
     * 通过通知规则 ID 删除通知规则的转账账户数据
     *
     * @param ruleId 需要删除转账账户数据的通知规则 ID
     */
    @Query("DELETE FROM notificationruletransfers WHERE ruleId = :ruleId")
    void deleteNotificationRuleTransferByRuleId(long ruleId);

    /**
     * 通过规则 ID 删除规则与标签的映射关系
     *
     * @param ruleId 规则 ID
     */
    @Query("DELETE FROM notificationRuleTagRef WHERE ruleId = :ruleId")
    void deleteNotificationRuleTagRefByRuleId(long ruleId);

    /**
     * 通过规则 ID 删除规则与分组的映射关系
     *
     * @param ruleId 规则 ID
     */
    @Query("DELETE FROM notificationRuleGroupRef WHERE ruleId = :ruleId")
    void deleteNotificationRuleGroupRefByRuleId(long ruleId);

    /**
     * 修改通知规则事务
     *
     * @param rule        修改后的通知规则
     * @param transfer    修改后的转账账户数据
     * @param tagIdList   修改后的标签 ID 列表
     * @param groupIdList 规则分组编号列表
     */
    @Transaction
    default void modifyNotificationRule(
            NotificationRuleEntity rule,
            NotificationRuleTransferEntity transfer,
            List<Long> tagIdList,
            List<Long> groupIdList
    ) {
        if (rule == null) return;
        long ruleId = rule.getRuleId();

        //获取旧数据
        Optional<NotificationRuleEntity> optional = getNotificationRuleOptionalById(ruleId);
        optional.ifPresent(oldRule -> rule.setEnabled(oldRule.isEnabled()));

        //更新数据
        updateNotificationRule(rule);

        //转账账户数据
        deleteNotificationRuleTransferByRuleId(ruleId);
        if (rule.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setRuleId(ruleId);
            insertNotificationTransfer(transfer);
        }

        //标签
        deleteNotificationRuleTagRefByRuleId(ruleId);
        List<NotificationRuleTagRefEntity> tagRefList = tagIdList.stream()
                .map(id -> new NotificationRuleTagRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertNotificationTagRef(tagRefList);

        //分组
        deleteNotificationRuleGroupRefByRuleId(ruleId);
        List<NotificationRuleGroupRefEntity> groupRefList = groupIdList.stream()
                .map(id -> new NotificationRuleGroupRefEntity(ruleId, id))
                .collect(Collectors.toList());
        insertNotificationRuleGroupRef(groupRefList);
    }

    /**
     * 删除通知规则
     *
     * @param entity 待删除的通知规则
     * @return 是否完成
     */
    @Delete
    Completable deleteNotificationRule(NotificationRuleEntity entity);

    /**
     * 设置通知规则是否启用
     *
     * @param enabled 是否启用
     * @param ruleId  需要更新的规则的 ID
     * @return 是否完成
     */
    @Query("UPDATE notificationRules SET enabled = :enabled WHERE ruleId = :ruleId")
    Completable setRuleEnabled(boolean enabled, long ruleId);

    /**
     * 获取所有符合搜索条件的被捕获的通知
     *
     * @param keyword         搜索关键词
     * @param useSearchFilter 是否需要过滤搜索条件
     * @return 捕获的通知列表，支持响应式更新
     */
    @Query("SELECT * FROM capturedNotifications " +
            "WHERE :useSearchFilter = 0 " +
            "OR title LIKE '%' || :keyword || '%' ESCAPE '/' " +
            "OR content LIKE '%' || :keyword || '%' ESCAPE '/' " +
            "OR appName LIKE '%' || :keyword || '%' ESCAPE '/' " +
            "ORDER BY time DESC")
    Flowable<List<CapturedNotificationEntity>> getAllCapturedNotificationFlowable(String keyword, int useSearchFilter);

    /**
     * 清空捕获的通知
     *
     * @return 是否完成
     */
    @Query("DELETE FROM capturedNotifications")
    Completable clearCapturedNotification();

    /**
     * 添加捕获的通知
     *
     * @param notification 被捕获的通知
     * @return 是否完成
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertCapturedNotification(CapturedNotificationEntity notification);

    /**
     * 删除捕获的通知
     *
     * @param notification 需要删除的通知
     * @return 是否完成
     */
    @Delete
    Completable deleteCapturedNotification(CapturedNotificationEntity notification);

    /**
     * 通过通知编号获取捕获的通知
     *
     * @param id 通知编号
     * @return 该编号对应的通知
     */
    @Query("SELECT * FROM capturedNotifications WHERE notificationId = :id")
    Single<Optional<CapturedNotificationEntity>> getCapturedNotificationById(long id);

    /**
     * 通过分组编号获取通知规则分组
     *
     * @param idSet 分组编号集合
     * @return 编号处于集合中的通知规则分组
     */
    @Query("SELECT * FROM notificationRuleGroups WHERE groupId IN (:idSet)")
    Single<List<NotificationRuleGroupEntity>> getRuleGroupById(Set<Long> idSet);

    /**
     * 获取所有通知规则分组数据
     *
     * @return 所有通知规则分组，支持响应式更新
     */
    @Query("SELECT * FROM notificationRuleGroups")
    Flowable<List<NotificationRuleGroupEntity>> getRuleGroupFlowable();

    /**
     * 添加通知规则分组
     *
     * @param group 待添加的通知规则分组
     * @return 是否完成
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable addRuleGroupCompletable(NotificationRuleGroupEntity group);
}
