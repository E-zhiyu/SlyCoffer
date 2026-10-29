package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.classes.ImportResult;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.types.ImportDataVersionType;
import com.sly.coffer.data.backup.maps.BudgetDataMap;
import com.sly.coffer.data.backup.maps.old.extra_v80.ExtraBudgetDataMap;
import com.sly.coffer.data.backup.maps.old.room_v62.RoomBudgetDataMap;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomBudgetPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.ui.pages.budget.ResetFrequency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetBackupHelper extends BackupHelperBase<BookkeepingDb, BudgetDataMap>
        implements OldDataConverter<BudgetDataMap> {
    public BudgetBackupHelper(Context context) {
        super(context);
    }

    @Override
    protected Class<BudgetDataMap> getMapClass() {
        return BudgetDataMap.class;
    }

    @Override
    protected BookkeepingDb getDatabase(Context context) {
        return BookkeepingDb.getInstance(context);
    }

    @Override
    protected BudgetDataMap getAllDataInMap() {
        return db.dataBackupDao().exportBudgetData();
    }

    @Override
    protected ImportResult saveDataInMapToDb(Context context, BudgetDataMap map) {
        return db.dataBackupDao().importBudgetData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.BUDGET.getLatestVersionFileName();
    }

    @Override
    protected BudgetDataMap getDataMap(String json, ImportDataVersionType versionType) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        if (versionType == ImportDataVersionType.BEFORE_ROOM) {
            return convertOldDataBeforeRoom(json);
        } else if (versionType == ImportDataVersionType.BEFORE_EXTRA) {
            return convertOldDataBeforeExtra(json);
        } else {
            return mapper.readValue(json, mapClass);
        }
    }

    @Override
    public BudgetDataMap convertOldDataBeforeRoom(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        RoomBudgetDataMap oldMap = mapper.readValue(json, RoomBudgetDataMap.class);

        //预算
        List<BudgetPojo> budgetPojoList = oldMap.getBudget_data().stream()
                .map(old -> {
                    BudgetPojo budgetPojo = new BudgetPojo();
                    budgetPojo.setBudgetId(old.getBno());           //编号
                    budgetPojo.setName(old.getName());              //名称
                    budgetPojo.setInitAmount(old.getInitAmount());  //初始金额
                    budgetPojo.setBalance(old.getLeftAmount());  //余额
                    budgetPojo.setStartDate(DateTimeConverter.fromLocalDate(    //起算日期
                            LocalDate.parse(old.getStartDate(), CustomDateTimeFormatter.DATE)
                    ));
                    budgetPojo.setResetFrequency(                   //重置频率
                            ResetFrequency.fromOldValue(old.getResetFrequency()).ordinal()
                    );
                    budgetPojo.setLowBalanceRatio(10);              //低余额比例
                    return budgetPojo;
                })
                .collect(Collectors.toList());

        //预算和标签的映射
        List<BudgetTagRefPojo> budgetTagRefPojoList = new ArrayList<>();
        for (RoomBudgetPojo old : oldMap.getBudget_data()) {
            long budgetId = old.getBno();
            for (long tagId : old.getTagNoList()) {
                BudgetTagRefPojo budgetTagRefPojo = new BudgetTagRefPojo();
                budgetTagRefPojo.setBudgetId(budgetId);
                budgetTagRefPojo.setTagId(tagId);
                budgetTagRefPojoList.add(budgetTagRefPojo);
            }
        }

        //实例化 Map
        BudgetDataMap map = new BudgetDataMap();
        map.setBudgetList(budgetPojoList);
        map.setBudgetTagRefList(budgetTagRefPojoList);
        return map;
    }

    @Override
    public BudgetDataMap convertOldDataBeforeExtra(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        ExtraBudgetDataMap oldMap = mapper.readValue(json, ExtraBudgetDataMap.class);

        //预算数据
        List<BudgetPojo> budgetList = oldMap.getBudgetList().stream()
                .map(old -> {
                    BudgetPojo pojo = new BudgetPojo();
                    pojo.setBudgetId(old.getBudgetId());
                    pojo.setName(old.getName());
                    pojo.setInitAmount(old.getInitAmount());
                    pojo.setBalance(old.getBalance());
                    pojo.setStartDate(old.getStartDate());
                    pojo.setResetFrequency(old.getResetFrequency());
                    pojo.setLowBalanceRatio(old.getLowBalanceRatio());

                    return pojo;
                })
                .collect(Collectors.toList());

        //与标签的映射数据
        List<BudgetTagRefPojo> tagRefList = oldMap.getBudgetTagRefList().stream()
                .map(old -> {
                    BudgetTagRefPojo pojo = new BudgetTagRefPojo();
                    pojo.setBudgetId(old.getBudgetId());
                    pojo.setTagId(old.getTagId());

                    return pojo;
                })
                .collect(Collectors.toList());

        BudgetDataMap map = new BudgetDataMap();
        map.setBudgetList(budgetList);
        map.setBudgetTagRefList(tagRefList);
        return map;
    }
}
