package com.sly.coffer.automation.workers.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.data.save.preference.AutoBackupPreference;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.helpers.file.ZipHelper;

import org.jspecify.annotations.NonNull;

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
        boolean includeMedia = choices == null || choices[BackupDataType.RUNNING_ACCOUNT.ordinal()];
        return Completable.merge(taskList)
                .andThen(ZipHelper.createBackupFile(finalBackupFileUri, context, includeMedia))
                .subscribeOn(Schedulers.io())
                .toSingleDefault(Result.success())
                .onErrorReturn(e -> {
                    Log.e(LogTags.BACKUP_WORKER.n(), "备份失败");
                    return Result.failure();
                })
                .doFinally(() -> NotificationHelper.cancelNotification(NotificationID.BACKUP_AND_RESTORE.ordinal(), context));
    }
}
