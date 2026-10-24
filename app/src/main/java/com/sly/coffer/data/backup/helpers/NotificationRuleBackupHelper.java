package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.ImportResult;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.NotificationRuleDataMap;
import com.sly.coffer.data.backup.maps.old.extra_v80.ExtraNotificationRuleDataMap;
import com.sly.coffer.data.backup.maps.old.room_v62.RoomNotificationRuleDataMap;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NotificationRuleBackupHelper extends BackupHelperBase<BookkeepingDb, NotificationRuleDataMap> {
    public NotificationRuleBackupHelper(Context context) {
        super(context);
    }

    @Override
    protected Class<NotificationRuleDataMap> getMapClass() {
        return NotificationRuleDataMap.class;
    }

    @Override
    protected BookkeepingDb getDatabase(Context context) {
        return BookkeepingDb.getInstance(context);
    }

    @Override
    protected NotificationRuleDataMap getAllDataInMap() {
        return db.dataBackupDao().exportNotificationRuleData();
    }

    @Override
    protected ImportResult saveDataInMapToDb(Context context, NotificationRuleDataMap map) {
        return db.dataBackupDao().importNotificationRuleData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.NOTIFICATION_RULE.getLatestVersionFileName();
    }

    @Override
    protected NotificationRuleDataMap convertOldDataBeforeRoom(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        RoomNotificationRuleDataMap oldMap = mapper.readValue(json, RoomNotificationRuleDataMap.class);

        //通知规则
        List<NotificationRulePojo> notificationRulePojoList = oldMap.getRule_data().stream()
                .map(old -> {
                    NotificationRulePojo notificationRulePojo = new NotificationRulePojo();
                    notificationRulePojo.setRuleId(old.getRuleNo());            //编号
                    notificationRulePojo.setName(old.getRuleName());            //名称
                    notificationRulePojo.setType(                               //种类
                            AccountType.fromOldValue(old.getType()).ordinal()
                    );
                    notificationRulePojo.setPackageName(old.getPackageName());  //包名
                    notificationRulePojo.setTargetTitle(old.getTitle());        //标题
                    notificationRulePojo.setContentRegex(old.getContent());
                    notificationRulePojo.setCaptureGroupPos(1);
                    notificationRulePojo.setEnabled(true);
                    return notificationRulePojo;
                })
                .collect(Collectors.toList());

        //转账账户
        Map<Long, NotificationRulePojo> ruleIdMap = notificationRulePojoList.stream()
                .collect(Collectors.toMap(
                        NotificationRulePojo::getRuleId,
                        Function.identity()
                ));
        List<ExtraTransferPojo> notificationRuleTransferPojoList = oldMap.getRule_account().stream()
                .map(old -> {
                    long oldRuleNo = old.getRuleNo();

                    //更新 NotificationRulePojo 的 transferId
                    NotificationRulePojo rulePojo = ruleIdMap.get(oldRuleNo);
                    if (rulePojo != null) {
                        rulePojo.setTransferId(oldRuleNo);
                    }

                    //创建转账账户记录 Pojo 对象
                    ExtraTransferPojo pojo = new ExtraTransferPojo();
                    pojo.setTransferId(oldRuleNo);
                    pojo.setExportAccount(old.getExportAccount());
                    pojo.setImportAccount(old.getImportAccount());
                    return pojo;
                })
                .collect(Collectors.toList());

        //通知规则标签映射关系
        List<NotificationRuleTagRefPojo> notificationRuleTagRefPojoList = oldMap.getRule_data().stream()
                .filter(old -> old.getTag_no() != 0)
                .map(old -> {
                    NotificationRuleTagRefPojo pojo = new NotificationRuleTagRefPojo();
                    pojo.setRuleId(old.getRuleNo());
                    pojo.setTagId(old.getTag_no());
                    return pojo;
                })
                .collect(Collectors.toList());

        //实例化数据集合
        NotificationRuleDataMap map = new NotificationRuleDataMap();
        map.setNotificationRuleList(notificationRulePojoList);
        map.setExtraTransferList(notificationRuleTransferPojoList);
        map.setNotificationRuleTagRefList(notificationRuleTagRefPojoList);
        return map;
    }

    @Override
    protected NotificationRuleDataMap convertOldDataBeforeExtra(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        ExtraNotificationRuleDataMap oldMap = mapper.readValue(json, ExtraNotificationRuleDataMap.class);

        //通知规则
        List<NotificationRulePojo> ruleList = oldMap.getNotificationRuleList().stream()
                .map(old -> {
                    NotificationRulePojo rule = new NotificationRulePojo();
                    rule.setRuleId(old.getRuleId());
                    rule.setName(old.getName());
                    rule.setType(old.getType());
                    rule.setPackageName(old.getPackageName());
                    rule.setTargetTitle(old.getTargetTitle());
                    rule.setContentRegex(old.getContentRegex());
                    rule.setCaptureGroupPos(old.getCaptureGroupPos());
                    rule.setEnabled(old.isEnabled());

                    return rule;
                })
                .collect(Collectors.toList());

        //标签映射
        List<NotificationRuleTagRefPojo> tagRefList = oldMap.getNotificationRuleTagRefList().stream()
                .map(old -> {
                    NotificationRuleTagRefPojo ref = new NotificationRuleTagRefPojo();
                    ref.setRuleId(old.getRuleId());
                    ref.setTagId(old.getTagId());

                    return ref;
                })
                .collect(Collectors.toList());

        //转账账户
        Map<Long, NotificationRulePojo> ruleIdMap = ruleList.stream()
                .collect(Collectors.toMap(
                        NotificationRulePojo::getRuleId,
                        Function.identity()
                ));
        List<ExtraTransferPojo> transferList = oldMap.getNotificationRuleTransferList().stream()
                .map(old -> {
                    long oldRuleId = old.getRuleId();

                    //更新外键
                    NotificationRulePojo rule = ruleIdMap.get(oldRuleId);
                    if (rule != null) {
                        rule.setTransferId(oldRuleId);
                    }

                    //转账账户
                    ExtraTransferPojo transfer = new ExtraTransferPojo();
                    transfer.setImportAccount(old.getImportAccount());
                    transfer.setExportAccount(old.getExportAccount());
                    transfer.setTransferId(oldRuleId);

                    return transfer;
                })
                .collect(Collectors.toList());

        //规则分组
        List<NotificationRuleGroupPojo> groupList = oldMap.getNotificationRuleGroupList().stream()
                .map(old -> {
                    NotificationRuleGroupPojo group = new NotificationRuleGroupPojo();
                    group.setGroupId(old.getGroupId());
                    group.setName(old.getName());
                    group.setOrder(old.getOrder());

                    return group;
                })
                .collect(Collectors.toList());

        //规则分组映射
        List<NotificationRuleGroupRefPojo> groupRefList = oldMap.getNotificationRuleGroupRefList().stream()
                .map(old -> {
                    NotificationRuleGroupRefPojo ref = new NotificationRuleGroupRefPojo();
                    ref.setGroupId(old.getGroupId());
                    ref.setRuleId(old.getRuleId());

                    return ref;
                })
                .collect(Collectors.toList());

        NotificationRuleDataMap map = new NotificationRuleDataMap();
        map.setNotificationRuleList(ruleList);
        map.setNotificationRuleTagRefList(tagRefList);
        map.setExtraTransferList(transferList);
        map.setNotificationRuleGroupList(groupList);
        map.setNotificationRuleGroupRefList(groupRefList);
        return map;
    }
}
