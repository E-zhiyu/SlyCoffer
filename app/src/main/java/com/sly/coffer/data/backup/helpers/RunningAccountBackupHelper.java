package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.RunningAccountDataMap;
import com.sly.coffer.data.backup.maps.old.extra_v80.ExtraRunningAccountDataMap;
import com.sly.coffer.data.backup.maps.old.room_v62.RoomRunningAccountDataMap;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    protected RunningAccountDataMap convertOldDataBeforeRoom(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        RoomRunningAccountDataMap oldMap = mapper.readValue(json, RoomRunningAccountDataMap.class);

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
        List<ExtraTransferPojo> extraTransferPojoList = oldMap.getTransfer_data().stream()
                .map(old -> {
                    ExtraTransferPojo pojo = new ExtraTransferPojo();
                    pojo.setTransferId(old.getRno());
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
        map.setExtraTransferList(extraTransferPojoList);
        map.setAccountTagRefList(accountTagRefPojoList);
        map.setMediaList(mediaPojoList);
        map.setTagGroupList(tagGroupPojoList);
        map.setTagList(tagPojoList);
        return map;
    }

    @Override
    protected RunningAccountDataMap convertOldDataBeforeExtra(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        ExtraRunningAccountDataMap oldMap = mapper.readValue(json, ExtraRunningAccountDataMap.class);

        //流水记录
        List<AccountPojo> accountList = oldMap.getAccountList().stream()
                .map(old -> {
                    AccountPojo account = new AccountPojo();
                    account.setAccountId(old.getAccountId());
                    account.setAmount(old.getAmount());
                    account.setRemark(old.getRemark());
                    account.setType(old.getType());
                    account.setDateTime(old.getDateTime());
                    account.setAutoTag(old.getAutoTag());

                    return account;
                })
                .collect(Collectors.toList());

        //标签映射
        List<AccountTagRefPojo> tagRefList = oldMap.getAccountTagRefList().stream()
                .map(old -> {
                    AccountTagRefPojo ref = new AccountTagRefPojo();
                    ref.setAccountId(old.getAccountId());
                    ref.setTagId(old.getTagId());

                    return ref;
                })
                .collect(Collectors.toList());

        //转账账户
        Map<Long, AccountPojo> accountIdMap = accountList.stream()
                .collect(Collectors.toMap(
                        AccountPojo::getAccountId,
                        Function.identity()
                ));
        List<ExtraTransferPojo> transferList = oldMap.getAccountTransferList().stream()
                .map(old -> {
                    long oldAccountId = old.getAccountId();

                    //修改外键
                    AccountPojo account = accountIdMap.get(oldAccountId);
                    if (account != null) {
                        account.setTransferId(oldAccountId);
                    }

                    //转账账户
                    ExtraTransferPojo transfer = new ExtraTransferPojo();
                    transfer.setTransferId(old.getAccountId());
                    transfer.setExportAccount(old.getExportAccount());
                    transfer.setImportAccount(old.getImportAccount());

                    return transfer;
                })
                .collect(Collectors.toList());

        //媒体
        List<MediaPojo> mediaList = oldMap.getMediaList().stream()
                .map(old -> {
                    MediaPojo media = new MediaPojo();
                    media.setAccountId(old.getAccountId());
                    media.setMediaId(old.getMediaId());
                    media.setFileUri(old.getFileUri());

                    return media;
                })
                .collect(Collectors.toList());

        //标签
        List<TagPojo> tagList = oldMap.getTagList().stream()
                .map(old -> {
                    TagPojo tag = new TagPojo();
                    tag.setTagId(old.getTagId());
                    tag.setName(old.getName());
                    tag.setGroupId(old.getGroupId());
                    tag.setScope(old.getScope());

                    return tag;
                })
                .collect(Collectors.toList());

        //标签分组
        List<TagGroupPojo> tagGroupList = oldMap.getTagGroupList().stream()
                .map(old -> {
                    TagGroupPojo group = new TagGroupPojo();
                    group.setGroupId(old.getGroupId());
                    group.setName(old.getName());

                    return group;
                })
                .collect(Collectors.toList());

        RunningAccountDataMap map = new RunningAccountDataMap();
        map.setAccountList(accountList);
        map.setAccountTagRefList(tagRefList);
        map.setMediaList(mediaList);
        map.setExtraTransferList(transferList);
        map.setTagGroupList(tagGroupList);
        map.setTagList(tagList);
        return map;
    }
}
