package com.sly.coffer.automation.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.helpers.time.AlarmHelper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.d(LogTags.BOOT_RECEIVER.n(), "设备已开机");
            AlarmHelper.setBudgetCheckAlarm(context);
        }
    }
}
