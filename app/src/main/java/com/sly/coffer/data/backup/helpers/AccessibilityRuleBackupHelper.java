package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.AccessibilityRuleDataMap;
import com.sly.coffer.data.backup.maps.old.extra_v80.ExtraAccessibilityRuleDataMap;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AccessibilityRuleBackupHelper extends BackupHelperBase<BookkeepingDb, AccessibilityRuleDataMap> {
    public AccessibilityRuleBackupHelper(Context context) {
        super(context);
    }

    @Override
    protected Class<AccessibilityRuleDataMap> getMapClass() {
        return AccessibilityRuleDataMap.class;
    }

    @Override
    protected BookkeepingDb getDatabase(Context context) {
        return BookkeepingDb.getInstance(context);
    }

    @Override
    protected AccessibilityRuleDataMap getAllDataInMap() {
        return db.dataBackupDao().exportAccessibilityRuleData();
    }

    @Override
    protected int saveDataInMapToDb(Context context, AccessibilityRuleDataMap map) {
        return db.dataBackupDao().importAccessibilityRuleData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.ACCESSIBILITY_RULE.getLatestVersionFileName();
    }

    @Override
    protected AccessibilityRuleDataMap convertOldDataBeforeRoom(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, mapClass);
    }

    @Override
    protected AccessibilityRuleDataMap convertOldDataBeforeExtra(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        ExtraAccessibilityRuleDataMap oldMap = mapper.readValue(json, ExtraAccessibilityRuleDataMap.class);

        //无障碍规则
        List<AccessibilityRulePojo> rulePojoList = oldMap.getRulePojoList().stream()
                .map(old -> {
                    AccessibilityRulePojo rulePojo = new AccessibilityRulePojo();
                    rulePojo.setRuleId(old.getRuleId());
                    rulePojo.setName(old.getName());
                    rulePojo.setType(old.getType());
                    rulePojo.setEnabled(old.isEnabled());
                    rulePojo.setPackageName(old.getPackageName());
                    rulePojo.setActivityName(old.getActivityName());

                    return rulePojo;
                })
                .collect(Collectors.toList());

        //标签映射数据
        List<AccessibilityRuleTagRefPojo> tagRefList = oldMap.getTagRefPojoList().stream()
                .map(old -> {
                    AccessibilityRuleTagRefPojo ref = new AccessibilityRuleTagRefPojo();
                    ref.setRuleId(old.getRuleId());
                    ref.setTagId(old.getTagId());

                    return ref;
                })
                .collect(Collectors.toList());

        //转账账户数据
        Map<Long, AccessibilityRulePojo> ruleIdMap = rulePojoList.stream()
                .collect(Collectors.toMap(
                        AccessibilityRulePojo::getRuleId,
                        Function.identity()
                ));
        List<ExtraTransferPojo> transferList = oldMap.getTransferPojoList().stream()
                .map(old -> {
                    long relatedRuleId = old.getRuleId();

                    //修改 AccessibilityRulePojo 的 transferId
                    AccessibilityRulePojo rulePojo = ruleIdMap.get(relatedRuleId);
                    if (rulePojo != null) {
                        rulePojo.setTransferId(relatedRuleId);
                    }

                    ExtraTransferPojo transfer = new ExtraTransferPojo();
                    transfer.setTransferId(relatedRuleId);
                    transfer.setExportAccount(old.getExportAccount());
                    transfer.setImportAccount(old.getImportAccount());
                    return transfer;
                })
                .collect(Collectors.toList());

        //关键词组合数据
        List<AccessibilityRuleKeywordGroupPojo> keywordGroupList = oldMap.getKeywordGroupPojoList().stream()
                .map(old -> {
                    AccessibilityRuleKeywordGroupPojo keywordGroupPojo = new AccessibilityRuleKeywordGroupPojo();
                    keywordGroupPojo.setKeywordId(old.getKeywordId());
                    keywordGroupPojo.setRuleId(old.getRuleId());
                    keywordGroupPojo.setContent(old.getContent());

                    return keywordGroupPojo;
                })
                .collect(Collectors.toList());

        AccessibilityRuleDataMap map = new AccessibilityRuleDataMap();
        map.setRuleList(rulePojoList);
        map.setTagRefList(tagRefList);
        map.setExtraTransferList(transferList);
        map.setKeywordGroupList(keywordGroupList);
        return map;
    }
}
