package com.sly.coffer.automation.workers.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.data.backup.maps.DataInfoMap;
import com.sly.coffer.data.save.preference.AutoBackupPreference;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.helpers.file.ZipHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BackupWorker extends RxWorker {
    public BackupWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @Override
    public @NonNull Single<Result> createWork() {
        //获取传入的参数
        Context context = getApplicationContext();
        Data inputData = getInputData();
        String inputUriStr = inputData.getString(KeyStrings.BACKUP_TARGET.v());
        boolean[] choices = inputData.getBooleanArray(KeyStrings.BACKUP_CHOICES.v());

        //添加备份任务
        List<Completable> taskList = new ArrayList<>();
        if (choices != null) {
            for (BackupDataType type : BackupDataType.values()) {
                if (!choices[type.ordinal()]) continue;

                BackupHelperBase<?, ?> backupHelper = type.createBackupHelper(context);
                taskList.add(backupHelper.exportDataToTempFile(context));
            }
        } else {
            //处理自动备份不传递选择状态的情况
            for (BackupDataType type : BackupDataType.values()) {
                BackupHelperBase<?, ?> backupHelper = type.createBackupHelper(context);
                taskList.add(backupHelper.exportDataToTempFile(context));
            }
        }

        //确定备份位置
        Uri finalBackupFileUri;  //最终的备份文件 Uri
        if (inputUriStr != null) {
            finalBackupFileUri = Uri.parse(inputUriStr);
        } else {
            String savedBackupTarget = AutoBackupPreference.getBackupDirectoryUri(context);
            if (savedBackupTarget.isEmpty()) {
                Log.e(LogTags.BACKUP_WORKER.n(), "无法获取备份目标位置");
                return Single.just(Result.failure());
            } else {
                //创建备份文件
                String fileName = FileHelper.generateBackupFileName();
                DocumentFile backupDir = DocumentFile.fromTreeUri(context, Uri.parse(savedBackupTarget));
                if (backupDir == null) {
                    Log.e(LogTags.BACKUP_WORKER.n(), "无法将目标位置 Uri 转换为 DocumentFile");
                    return Single.just(Result.failure());
                }
                DocumentFile backupFile = backupDir.createFile("application/zip", fileName);
                if (backupFile == null) return Single.just(Result.failure());
                Log.d(LogTags.BACKUP_WORKER.n(), "备份文件成功创建");

                finalBackupFileUri = backupFile.getUri();
            }
        }

        //执行备份逻辑
        sendProgressNotification(context);  //发送通知
        boolean includeMedia = choices == null || choices[BackupDataType.RUNNING_ACCOUNT.ordinal()];
        return writeDataInfoToFile(context, choices)
                .andThen(Completable.merge(taskList))
                .andThen(ZipHelper.createBackupFile(finalBackupFileUri, context, includeMedia))
                .subscribeOn(Schedulers.io())
                .toSingleDefault(Result.success())
                .onErrorReturn(e -> {
                    Log.e(LogTags.BACKUP_WORKER.n(), "备份失败\n" + e.getMessage());
                    return Result.failure();
                })
                .doFinally(() -> {
                    //发送通知
                    String channelID = ChannelInfo.BACKUP_AND_RESTORE.getId();
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(context.getString(R.string.export_data))
                            .setContentText("数据导出完成")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_CALL)
                            .setTimeoutAfter(5000)
                            .setAutoCancel(true);
                    NotificationHelper.sendNotification(NotificationID.BACKUP_AND_RESTORE.ordinal(), notificationBuilder, context);
                });
    }

    /**
     * 将备份数据信息写入到 JSON 文件中
     *
     * @param context 上下文
     * @param choice  备份数据类型的选择状况
     * @return 是否成功
     */
    public Completable writeDataInfoToFile(Context context, @Nullable boolean[] choice) {
        return Completable.defer(() -> {
            try {
                DataInfoMap infoMap = BackupHelperBase.generateDataInfo(context, choice);
                ObjectMapper mapper = new ObjectMapper();
                String content = mapper.writeValueAsString(infoMap);
                FileHelper.writeContentToTempDataFile(context, BackupHelperBase.DATA_INFO_JSON, content);

                return Completable.complete();
            } catch (IOException e) {
                return Completable.error(e);
            }
        });
    }

    /**
     * 发送进度条通知
     *
     * @param context 上下文
     */
    private void sendProgressNotification(Context context) {
        //构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ChannelInfo.BACKUP_AND_RESTORE.getId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.export_data))
                .setContentText("正在导出数据……")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        //发送通知
        NotificationHelper.sendNotification(
                NotificationID.BACKUP_AND_RESTORE.ordinal(),
                builder,
                context
        );
    }
}
