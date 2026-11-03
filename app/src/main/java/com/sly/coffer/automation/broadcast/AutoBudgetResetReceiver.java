package com.sly.coffer.automation.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.helpers.time.AlarmHelper;
import com.sly.coffer.ui.pages.budget.ResetFrequency;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AutoBudgetResetReceiver extends BroadcastReceiver {
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LogTags.BUDGET_RESET_RECEIVER.n(), "预算重置闹钟已触发");

        final PendingResult syncResult = goAsync();
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        disposable.add(db.budgetDao().getAllBudgetSingle()
                .flatMapCompletable(budgetList -> {
                    List<Long> resetBudgetIdList = getResetBudgetIdList(budgetList);
                    return db.budgetDao().resetBudgetByIdCompletable(resetBudgetIdList);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                            syncResult.finish();
                            disposable.dispose();
                        },
                        e -> {
                            syncResult.finish();
                            disposable.dispose();
                            Log.e(LogTags.BUDGET_RESET_RECEIVER.n(), "预算重置出错\n" + e.getMessage());
                        }
                )
        );

        AlarmHelper.setBudgetCheckAlarm(context);
    }

    /**
     * 获取需要重置的预算的编号
     *
     * @param budgetList 原始预算数据
     * @return 需要重置的预算的编号列表
     */
    @NonNull
    private List<Long> getResetBudgetIdList(@NonNull List<BudgetEntity> budgetList) {
        List<Long> result = new ArrayList<>();

        ResetFrequency[] frequencies = ResetFrequency.values();
        LocalDate today = LocalDate.now();
        for (BudgetEntity budget : budgetList) {
            long id = budget.getBudgetId();
            ResetFrequency frequency = frequencies[budget.getResetFrequency()];
            LocalDate startDate = budget.getStartDate();
            long dateDifference = ChronoUnit.DAYS.between(today, startDate);

            switch (frequency) {
                case EVERY_DAY:
                    result.add(id);
                    break;
                case EVERY_WEEK:
                    if (dateDifference >= 7) {
                        result.add(id);
                    }
                    break;
                case EVERY_MONTH:
                    if (today.getDayOfMonth() == 1) {
                        result.add(id);
                    }
                    break;
            }
        }

        return result;
    }
}
