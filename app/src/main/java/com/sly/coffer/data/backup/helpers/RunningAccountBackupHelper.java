package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.RunningAccountDataMap;
import com.sly.coffer.data.backup.maps.old.OldRunningAccountDataMap;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccountTransferPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RunningAccountBackupHelper extends BackupHelperBase<BookkeepingDb, RunningAccountDataMap> {
    public RunningAccountBackupHelper(Context context) {
        super(context);
    }

    @Override
    protected Class<RunningAccountDataMap> getMapClass() {
        return RunningAccountDataMap.class;
    }

    @Override
    protected BookkeepingDb getDatabase(Context context) {
        return BookkeepingDb.getInstance(context);
    }

    @Override
    protected RunningAccountDataMap getAllDataInMap() {
        return db.dataBackupDao().exportRunningAccountData();
    }

    @Override
    protected void saveDataInMapToDb(Context context, RunningAccountDataMap map) {
        db.dataBackupDao().importRunningAccountData(context, map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.RUNNING_ACCOUNT.getFileName();
    }

    @Override
    protected RunningAccountDataMap convertOldData(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        OldRunningAccountDataMap oldMap = mapper.readValue(json, OldRunningAccountDataMap.class);

        //流水数据
        List<AccountPojo> accountPojoList = oldMap.getBasic_data().stream()
                .map(old -> {
                    AccountPojo pojo = new AccountPojo();
                    pojo.setAccountId(old.getRno());    //编号
                    pojo.setAmount(old.getAmount());    //金额
                    pojo.setRemark(old.getRemark());    //备注
                    pojo.setType(                       //种类
                            AccountType.fromOldValue(old.getType()).ordinal()
                    );
                    pojo.setDateTime(                   //日期和时间
                            DateTimeConverter.fromLocalDateTime(LocalDateTime.parse(old.getDate_time(), CustomDateTimeFormatter.DATE_TIME))
                    );
                    return pojo;
                })
                .collect(Collectors.toList());

        //流水记录和标签映射
        List<AccountTagRefPojo> accountTagRefPojoList = oldMap.getBasic_data().stream()
                .filter(old -> old.getTag_no() != 0)
                .map(old -> {
                    AccountTagRefPojo pojo = new AccountTagRefPojo();
                    pojo.setAccountId(old.getRno());
                    pojo.setTagId(old.getTag_no());
                    return pojo;
                })
                .collect(Collectors.toList());

        //流水记录的转账账户
        List<AccountTransferPojo> accountTransferPojoList = oldMap.getTransfer_data().stream()
                .map(old -> {
                    AccountTransferPojo pojo = new AccountTransferPojo();
                    pojo.setAccountId(old.getRno());
                    pojo.setExportAccount(old.getExport_account());
                    pojo.setImportAccount(old.getImport_account());
                    return pojo;
                })
                .collect(Collectors.toList());

        //媒体
        List<MediaPojo> mediaPojoList = oldMap.getPicture_data().stream()
                .map(old -> {
                    MediaPojo pojo = new MediaPojo();
                    pojo.setAccountId(old.getRno());
                    pojo.setMediaId(old.getPno());
                    String oldUriStr = old.getUri();
                    String newUriStr = oldUriStr.replace("com.manager.assitant/files/pictures", "com.sly.coffer/files/medias");
                    pojo.setFileUri(newUriStr);
                    return pojo;
                })
                .collect(Collectors.toList());

        //标签分组
        List<TagGroupPojo> tagGroupPojoList = oldMap.getTag_group_data().stream()
                .map(old -> {
                    TagGroupPojo pojo = new TagGroupPojo();
                    pojo.setGroupId(old.getGroup_no() == 0 ? -1 : old.getGroup_no());
                    pojo.setName(old.getGroup_name());
                    return pojo;
                })
                .collect(Collectors.toList());

        //标签
        List<TagPojo> tagPojoList = oldMap.getTag_data().stream()
                .map(old -> {
                    TagPojo pojo = new TagPojo();
                    pojo.setGroupId(old.getGroup_no() == 0 ? -1 : old.getGroup_no());
                    pojo.setTagId(old.getTno());
                    pojo.setName(old.getName());
                    pojo.setScope(old.getScope());
                    return pojo;
                })
                .collect(Collectors.toList());

        //实例化数据集合
        RunningAccountDataMap map = new RunningAccountDataMap();
        map.setAccountList(accountPojoList);
        map.setAccountTransferList(accountTransferPojoList);
        map.setAccountTagRefList(accountTagRefPojoList);
        map.setMediaList(mediaPojoList);
        map.setTagGroupList(tagGroupPojoList);
        map.setTagList(tagPojoList);
        return map;
    }
}
