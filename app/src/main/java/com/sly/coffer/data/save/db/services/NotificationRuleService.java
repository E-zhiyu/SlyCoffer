package com.sly.coffer.data.save.db.services;

import com.sly.coffer.auxiliary.enums.types.MoveDirectionType;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleAndGroupUnionModel;

import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class NotificationRuleService {
    /**
     * 添加新的通知规则
     *
     * @param rule        通知规则
     * @param transfer    转账类型的通知规则的转入转出账户数据
     * @param tagIdList   标签编号列表
     * @param groupIdList 规则分组编号列表
     * @param db          数据库实例
     * @return 是否完成
     */
    public static Completable addNewNotificationRule(
            NotificationRuleEntity rule,
            NotificationRuleTransferEntity transfer,
            List<Long> tagIdList,
            List<Long> groupIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.notificationRuleDao().addNotificationRule(rule, transfer, tagIdList, groupIdList);
            return Completable.complete();
        });
    }

    /**
     * 修改通知规则
     *
     * @param rule        修改后的通知规则
     * @param transfer    修改后的转账账户数据
     * @param tagIdList   修改后的标签编号列表
     * @param groupIdList 规则分组编号列表
     * @param db          数据库实例
     * @return 是否完成l
     */
    public static Completable modifyNotificationRule(
            NotificationRuleEntity rule,
            NotificationRuleTransferEntity transfer,
            List<Long> tagIdList,
            List<Long> groupIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.notificationRuleDao().modifyNotificationRule(rule, transfer, tagIdList, groupIdList);
            return Completable.complete();
        });
    }

    /**
     * 通过编号有序获取通知规则
     *
     * @param idList 通知规则编号列表
     * @param db     数据库实例
     * @return 依照规则列表中的排序获取到的通知规则
     */
    public static Single<List<NotificationRuleEntity>> getNotificationByIdInOrder(
            List<Long> idList,
            BookkeepingDb db
    ) {
        return Single.defer(() -> {
            List<NotificationRuleEntity> list = db.notificationRuleDao().getNotificationRuleByIdInOrder(idList);
            return Single.just(list);
        });
    }

    /**
     * 添加通知规则分组
     *
     * @param group      通知规则分组
     * @param ruleIdList 通知规则编号列表
     * @param db         数据库实例
     * @return 是否完成
     */
    public static Completable addNotificationRuleGroup(
            NotificationRuleGroupEntity group,
            List<Long> ruleIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.notificationRuleDao().addRuleGroup(group, ruleIdList);
            return Completable.complete();
        });
    }

    /**
     * 修改通知规则分组
     *
     * @param group      修改后的通知规则分组
     * @param ruleIdList 通知规则编号列表
     * @param db         数据库实例
     * @return 是否完成
     */
    public static Completable modifyNotificationRuleGroup(
            NotificationRuleGroupEntity group,
            List<Long> ruleIdList,
            BookkeepingDb db
    ) {
        return Completable.defer(() -> {
            db.notificationRuleDao().modifyRuleGroup(group, ruleIdList);
            return Completable.complete();
        });
    }

    /**
     * 通过分组编号获取通知规则分组及其包含的规则
     *
     * @param groupId 通知规则分组编号
     * @param db      数据库实例
     * @return 该编号对应的规则分组及其包含的规则
     */
    public static Single<Optional<NotificationRuleAndGroupUnionModel>> getRuleGroupAndRuleByGroupId(long groupId, BookkeepingDb db) {
        return Single.defer(() -> {
            NotificationRuleAndGroupUnionModel model = db.notificationRuleDao().getRuleGroupAndRuleByGroupId(groupId);
            if (model == null) {
                return Single.just(Optional.empty());
            } else {
                return Single.just(Optional.of(model));
            }
        });
    }

    /**
     * 移动规则分组
     *
     * @param groupId   需要移动的分组的编号
     * @param direction 移动的方向种类
     * @param db        数据库实例
     * @return 是否完成
     */
    public static Completable moveRuleGroup(long groupId, MoveDirectionType direction, BookkeepingDb db) {
        return Completable.defer(() -> {
            db.notificationRuleDao().moveRuleGroup(groupId, direction);
            return Completable.complete();
        });
    }
}
