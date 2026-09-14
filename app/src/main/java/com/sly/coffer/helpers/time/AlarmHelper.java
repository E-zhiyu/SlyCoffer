package com.sly.coffer.helpers.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sly.coffer.automation.broadcast.BudgetResetReceiver;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.PendingRequestCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AlarmHelper {
    /**
     * 安排精确闹钟
     *
     * @param dateTime 闹钟触发时间
     * @param intent   闹钟触发后执行的意图
     * @param context  上下文
     */
    public static void setAlarm(@NonNull LocalDateTime dateTime, int requestCode, Intent intent, @NonNull Context context) {
        //转换为时间戳
        long timeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();   //使用当前时区转换为时间戳
        long systemMillis = System.currentTimeMillis();
        Log.d(LogTags.ALARM_HELPER.n(), "已安排定时任务，时间：" + dateTime);
        Log.d(LogTags.ALARM_HELPER.n(), "安排的任务的时间戳：" + timeMillis);
        Log.d(LogTags.ALARM_HELPER.n(), "系统时间戳：" + systemMillis);

        //获取闹钟管理器
        AlarmManager am = context.getSystemService(AlarmManager.class);

        //设置单一的闹钟
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExact(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pi
                );
                Log.i(LogTags.ALARM_HELPER.n(), "已设置定时任务");
            } else {
                Log.e(LogTags.ALARM_HELPER.n(), "无法设置定时任务");
            }
        } else {
            am.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pi
            );
            Log.i(LogTags.ALARM_HELPER.n(), "已设置定时任务（旧版本）");
        }
    }

    /**
     * 设置预算重置闹钟
     *
     * @param context 上下文
     */
    public static void setBudgetCheckAlarm(Context context) {
        Intent intent = new Intent(context, BudgetResetReceiver.class);
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        setAlarm(tomorrowStart, PendingRequestCode.BUDGET_RESET_ALARM.ordinal(), intent, context);
    }

    /**
     * 取消已设置的定时任务
     *
     * @param requestCode 已设置的定时任务的请求代码
     * @param intent      已设置的定时任务的意图对象
     * @param context     上下文
     */
    public static void cancelAlarm(int requestCode, Intent intent, Context context) {
        // 重新构建 PendingIntent（注意：这里的 Flag 可以是 FLAG_NO_CREATE 或 FLAG_UPDATE_CURRENT）
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE 表示如果不存在就不创建
        );

        // 如果 PendingIntent 存在，调用 cancel
        if (pendingIntent != null) {
            AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
            alarmManager.cancel(pendingIntent);

            // 彻底释放这个 PendingIntent
            pendingIntent.cancel();
            Log.d(LogTags.ALARM_HELPER.n(), "定时任务已成功取消");
        } else {
            Log.d(LogTags.ALARM_HELPER.n(), "没有找到匹配的定时任务，无需取消");
        }
    }
}
