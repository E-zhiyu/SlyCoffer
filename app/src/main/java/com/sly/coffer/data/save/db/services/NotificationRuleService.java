package com.sly.coffer.data.save.db.services;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;

import java.util.List;

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
}
