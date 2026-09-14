package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.AccessibilityRuleDataMap;
import com.sly.coffer.data.save.db.BookkeepingDb;

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
    protected void saveDataInMapToDb(Context context, AccessibilityRuleDataMap map) {
        db.dataBackupDao().importAccessibilityRuleData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.ACCESSIBILITY_RULE.getFileName();
    }

    @Override
    protected AccessibilityRuleDataMap convertOldData(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, mapClass);
    }
}
