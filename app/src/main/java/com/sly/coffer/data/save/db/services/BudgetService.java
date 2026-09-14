package com.sly.coffer.data.save.db.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.ui.pages.budget.BudgetListActivity;

import org.jetbrains.annotations.Contract;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;

public class BudgetService {
    /**
     * 添加预算
     *
     * @param budget    待添加的预算
     * @param tagIdList 与该预算绑定的标签的 ID 编号
     * @param context   上下文
     * @return 是否完成
     */
    public static Completable addBudget(BudgetEntity budget, List<Long> tagIdList, Context context) {
        return Completable.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            db.budgetDao().addBudget(budget, tagIdList);

            //检查预算
            checkAndSendLowBalanceNotification(context);

            return Completable.complete();
        });
    }

    /**
     * 修改预算
     *
     * @param budget    修改后的预算
     * @param tagIdList 与该预算绑定的标签的 ID 编号
     * @param context   上下文
     * @return 是否完成
     */
    public static Completable modifyBudget(BudgetEntity budget, List<Long> tagIdList, Context context) {
        return Completable.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            db.budgetDao().modifyBudget(budget, tagIdList);

            //检查预算
            checkAndSendLowBalanceNotification(context);

            return Completable.complete();
        });
    }

    /**
     * 删除预算
     *
     * @param budget  需要删除的预算
     * @param context 上下文
     * @return 是否完成
     */
    public static Completable deleteBudget(BudgetEntity budget, Context context) {
        return Completable.defer(() -> {
            BookkeepingDb db = BookkeepingDb.getInstance(context);
            db.budgetDao().deleteBudget(budget);

            //检查预算
            checkAndSendLowBalanceNotification(context);

            return Completable.complete();
        });
    }

    /**
     * 检查是否有低余额的预算并发送提醒通知
     *
     * @param context 上下文
     */
    @Contract(pure = true)
    public static void checkAndSendLowBalanceNotification(Context context) {
        BookkeepingDb db = BookkeepingDb.getInstance(context);
        List<BudgetEntity> lowBalanceBudgetList = db.budgetDao().getLowBalanceBudget();
        if (lowBalanceBudgetList.isEmpty()) return;

        //构建通知内容
        StringBuilder contentBuilder = getBudgetBalanceContentBuilder(lowBalanceBudgetList);

        //生成通知点击的 PendingIntent
        Intent skip2BudgetList = new Intent(context, BudgetListActivity.class);
        PendingIntent clickPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(skip2BudgetList)
                .getPendingIntent(
                        PendingRequestCode.BUDGET_LOW_BALANCE.ordinal(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        //生成通知构建器
        int notificationId = NotificationID.BUDGET_AMOUNT_WARNING.ordinal();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ChannelInfo.BUDGET_BALANCE.getId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("预算提醒")
                .setContentText(contentBuilder.toString())
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(clickPendingIntent)
                .setAutoCancel(true);
        NotificationHelper.sendNotification(notificationId, notificationBuilder, context);
    }

    /**
     * 获取预算余额低通知内容的{@link StringBuilder}对象
     *
     * @param lowBalanceBudgetList 余额低的预算的列表
     * @return 通知内容构文本建器
     */
    @NonNull
    private static StringBuilder getBudgetBalanceContentBuilder(@NonNull List<BudgetEntity> lowBalanceBudgetList) {
        int count = lowBalanceBudgetList.size();
        StringBuilder contentBuilder = new StringBuilder();
        if (count <= 4) {
            int i = 1;
            for (BudgetEntity budget : lowBalanceBudgetList) {
                contentBuilder.append("“").append(budget.getName()).append("”");
                if (i < count - 1) {
                    contentBuilder.append("、");
                } else if (i < count) {
                    contentBuilder.append("和");
                }

                i++;
            }

            contentBuilder.append("的余额低于设定的值。");
        } else {
            final int LIMIT = 3;
            int i = 1;
            for (BudgetEntity budget : lowBalanceBudgetList) {
                contentBuilder.append("“").append(budget.getName()).append("”");
                if (i < LIMIT - 1) {
                    contentBuilder.append("、");
                } else if (i < LIMIT) {
                    contentBuilder.append("和");
                }

                i++;
            }

            contentBuilder.append("等共").append(count).append("条预算余额低于设定的值。");
        }
        return contentBuilder;
    }
}
