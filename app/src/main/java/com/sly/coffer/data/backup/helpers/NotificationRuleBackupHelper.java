package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.NotificationRuleDataMap;
import com.sly.coffer.data.backup.maps.old.OldNotificationRuleDataMap;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTransferPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;

import java.util.List;
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
    protected void saveDataInMapToDb(Context context, NotificationRuleDataMap map) {
        db.dataBackupDao().importNotificationRuleData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.NOTIFICATION_RULE.getFileName();
    }

    @Override
    protected NotificationRuleDataMap convertOldData(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        OldNotificationRuleDataMap oldMap = mapper.readValue(json, OldNotificationRuleDataMap.class);

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

        //通知规则转账账户
        List<NotificationRuleTransferPojo> notificationRuleTransferPojoList = oldMap.getRule_account().stream()
                .map(old -> {
                    NotificationRuleTransferPojo pojo = new NotificationRuleTransferPojo();
                    pojo.setRuleId(old.getRuleNo());
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
        map.setNotificationRuleTransferList(notificationRuleTransferPojoList);
        map.setNotificationRuleTagRefList(notificationRuleTagRefPojoList);
        return map;
    }
}
