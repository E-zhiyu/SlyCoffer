package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.data.backup.maps.BudgetDataMap;
import com.sly.coffer.data.backup.maps.old.OldBudgetDataMap;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;
import com.sly.coffer.data.backup.pojo.old.OldBudgetPojo;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.ui.pages.budget.ResetFrequency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetBackupHelper extends BackupHelperBase<BookkeepingDb, BudgetDataMap> {
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
    protected void saveDataInMapToDb(Context context, BudgetDataMap map) {
        db.dataBackupDao().importBudgetData(map);
    }

    @Override
    protected String getTempDataFileName() {
        return BackupDataType.BUDGET.getFileName();
    }

    @Override
    protected BudgetDataMap convertOldData(String json) throws JsonProcessingException {
        //获取旧数据的集合
        ObjectMapper mapper = new ObjectMapper();
        OldBudgetDataMap oldMap = mapper.readValue(json, OldBudgetDataMap.class);

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
        for (OldBudgetPojo old : oldMap.getBudget_data()) {
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
}
