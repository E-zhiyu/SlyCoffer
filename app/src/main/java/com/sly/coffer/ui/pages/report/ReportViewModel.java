package com.sly.coffer.ui.pages.report;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.DateRangeType;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.composite.union.AccountUnionModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class ReportViewModel extends ViewModel {
    private final MutableLiveData<DateRangeType> rangeTypeLiveData = new MutableLiveData<>(DateRangeType.MONTH);
    private final MutableLiveData<Pair<LocalDate, LocalDate>> dateRangeLiveData;
    private final BehaviorProcessor<DateRangeType> rangeTypeProcessor =
            BehaviorProcessor.createDefault(DateRangeType.MONTH);                           //用户通过下拉框选择的日期范围种类
    private final BehaviorProcessor<Pair<LocalDate, LocalDate>> selectedDateRangeProcessor; //用户通过日期选择对话框选择的日期范围
    private final BehaviorProcessor<LocalDate> selectedDateProcessor =
            BehaviorProcessor.createDefault(LocalDate.now());                               //用户通过日期选择对话框选择的日期
    private final BehaviorProcessor<Set<Long>> includedAccountIdProcessor =
            BehaviorProcessor.createDefault(new HashSet<>());                               //需要参与报表统计的流水记录 ID 的集合


    public ReportViewModel() {
        //计算初始日期范围
        LocalDate defaultStart = LocalDate.now().withDayOfMonth(1);
        LocalDate defaultEnd = defaultStart.plusMonths(1);
        Pair<LocalDate, LocalDate> defaultDateRangePair = new Pair<>(defaultStart, defaultEnd);

        selectedDateRangeProcessor = BehaviorProcessor.createDefault(defaultDateRangePair);  //用户选择的日期范围
        dateRangeLiveData = new MutableLiveData<>(new Pair<>(defaultStart, defaultEnd));
    }

    public MutableLiveData<DateRangeType> getRangeTypeLiveData() {
        return rangeTypeLiveData;
    }

    public MutableLiveData<Pair<LocalDate, LocalDate>> getDateRangeLiveData() {
        return dateRangeLiveData;
    }

    public DateRangeType getRangeType() {
        return rangeTypeProcessor.getValue();
    }

    public LocalDate getSelectedDate() {
        return selectedDateProcessor.getValue();
    }

    public Pair<LocalDate, LocalDate> getDateRange() {
        return selectedDateRangeProcessor.getValue();
    }

    public BehaviorProcessor<Set<Long>> getIncludedAccountIdProcessor() {
        return includedAccountIdProcessor;
    }

    /**
     * 更新日期范围种类
     *
     * @param rangeType 更新后的日期范围种类
     */
    public void updateRangeType(DateRangeType rangeType) {
        rangeTypeProcessor.onNext(rangeType);
    }

    /**
     * 更新选中的日期
     *
     * @param selectedDate 选中的日期
     */
    public void updateSelectedDate(LocalDate selectedDate) {
        selectedDateProcessor.onNext(selectedDate);
    }

    /**
     * 更新日期范围（包含起始日期和结束日期）
     *
     * @param pair 更新后的日期范围，日期大小顺序可随意
     */
    public void updateDateRange(@NonNull Pair<LocalDate, LocalDate> pair) {
        LocalDate first = pair.first;
        LocalDate second = pair.second;
        if (!first.isAfter(second)) {
            selectedDateRangeProcessor.onNext(pair);
        } else {
            selectedDateRangeProcessor.onNext(new Pair<>(second, first));
        }
    }

    /**
     * 更新用于展示统计数据的流水记录的编号集合
     *
     * @param idSet 用于展示统计数据的流水记录的编号集合
     */
    public void updateIncludedAccountId(Set<Long> idSet) {
        includedAccountIdProcessor.onNext(idSet);
    }

    /**
     * 获取展示的流水记录数据（不包含每月流水统计）
     *
     * @param db 数据库实例
     * @return 用于展示报表信息的流水记录数据
     */
    public Flowable<List<AccountUnionModel>> getRunningAccountDataFlowable(BookkeepingDb db) {
        return includedAccountIdProcessor.debounce(50, TimeUnit.MILLISECONDS)
                .flatMap(idSet -> {
                    if (!idSet.isEmpty()) {
                        return db.accountDao().getAccountWithDetailFlowableById(idSet);
                    } else {
                        return Flowable.just(new ArrayList<>());
                    }
                });
    }

    /**
     * 获取展示的流水记录的 ID
     *
     * @param db 数据库实例
     * @return 在指定日期范围内的流水记录 ID
     */
    public Flowable<List<Long>> processAccountId(BookkeepingDb db) {
        return Flowable.combineLatest(
                        selectedDateRangeProcessor,
                        rangeTypeProcessor,
                        selectedDateProcessor,
                        (pair, type, selectedDate) -> {
                            rangeTypeLiveData.postValue(type);
                            Log.d(LogTags.REPORT_VIEW_MODEL.n(), "流水种类 : " + type.getTitle());

                            if (type != DateRangeType.CUSTOM) {
                                LocalDate start, end;
                                switch (type) {
                                    case MONTH:
                                        start = selectedDate.withDayOfMonth(1);
                                        end = start.plusMonths(1);
                                        break;
                                    case YEAR:
                                        start = selectedDate.withDayOfYear(1);
                                        end = start.plusYears(1);
                                        break;
                                    case THAT_DAY:
                                    default:
                                        start = selectedDate;
                                        end = start.plusDays(1);
                                        break;
                                }
                                return new Pair<>(start, end);
                            } else {
                                return new Pair<>(pair.first, pair.second.plusDays(1));
                            }
                        }
                )
                .debounce(50, TimeUnit.MILLISECONDS)
                .flatMap(dateRangePair -> {
                    String log = String.format(
                            Locale.getDefault(),
                            "日期范围 : %s ~ %s",
                            dateRangePair.first.format(CustomDateTimeFormatter.DATE_SHORT),
                            dateRangePair.second.format(CustomDateTimeFormatter.DATE_SHORT)
                    );
                    Log.d(LogTags.REPORT_VIEW_MODEL.n(), "日期范围 : " + log);
                    dateRangeLiveData.postValue(new Pair<>(dateRangePair.first, dateRangePair.second.plusDays(-1)));
                    return db.accountDao().getAccountIdFlowableByDateRange(dateRangePair.first, dateRangePair.second);
                })
                .flatMap(idList -> {
                    includedAccountIdProcessor.onNext(new HashSet<>(idList));
                    return Flowable.just(idList);
                });
    }

    /**
     * 获取显示每月流水记录的数据
     *
     * @param db 数据库实例
     * @return 用于显示每月流水的流水数据详情
     */
    public Flowable<List<AccountUnionModel>> getMonthAccountDataFlowable(BookkeepingDb db) {
        return Flowable.combineLatest(
                        rangeTypeProcessor,
                        selectedDateProcessor,
                        (type, selectedDate) -> {
                            if (type != DateRangeType.CUSTOM) {
                                LocalDate yearStart = selectedDate.withDayOfYear(1);
                                LocalDate end = yearStart.plusYears(1);
                                return new Pair<>(yearStart, end);
                            } else {
                                return new Pair<>(LocalDate.now(), LocalDate.now());
                            }
                        }
                )
                .flatMap(dateRangePair ->
                        db.accountDao().getAccountWithDetailFlowableByDateRange(
                                dateRangePair.first,
                                dateRangePair.second
                        ))
                .debounce(50, TimeUnit.MILLISECONDS);
    }
}
