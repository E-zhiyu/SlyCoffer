package com.sly.coffer.automation.workers.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.file.ZipHelper;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RestoreWorker extends RxWorker {
    public RestoreWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @Override
    public @NonNull Single<Result> createWork() {
        //获取输入的数据
        Context context = getApplicationContext();
        Data inputData = getInputData();
        String inputUriStr = inputData.getString(KeyStrings.BACKUP_TARGET.v());
        boolean[] choices = inputData.getBooleanArray(KeyStrings.BACKUP_CHOICES.v());
        boolean isOldData = inputData.getBoolean(KeyStrings.RESTORE_IS_OLD_DATA.v(), false);

        //判断输入参数是否为空
        if (inputUriStr == null) {
            Log.e(LogTags.RESTORE_WORKER.n(), "无法获取恢复文件的位置");
            return Single.just(Result.failure());
        }

        //获取需要解压的文件名列表
        List<String> allowedFileNameList = Arrays.stream(BackupDataType.values())
                .filter(type -> choices == null || choices[type.ordinal()])
                .map(BackupDataType::getFileName)
                .collect(Collectors.toList());
        boolean includeMedia = choices == null || choices[BackupDataType.RUNNING_ACCOUNT.ordinal()];

        //解压文件并写入数据
        Uri uri = Uri.parse(inputUriStr);
        return ZipHelper.unpackBackupFileWithFilter(context, uri, allowedFileNameList, includeMedia)
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(file -> {
                    //根据文件名判断数据类型
                    BackupDataType type;
                    if (isOldData) {
                        type = BackupDataType.fromOldFileName(file.getName());
                    } else {
                        type = BackupDataType.fromFileName(file.getName());
                    }

                    //使用对应的备份Helper导入数据
                    if (type != null) {
                        BackupHelperBase<?, ?> helper = type.createBackupHelper(context);
                        return helper.importDataFromTempFile(context, file, isOldData);
                    } else {
                        return Completable.complete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .toSingleDefault(Result.success())
                .onErrorReturn(e -> {
                    Log.e(LogTags.RESTORE_WORKER.n(), "导入失败");
                    return Result.failure();
                })
                .doFinally(() -> NotificationHelper.cancelNotification(NotificationID.BACKUP_AND_RESTORE.ordinal(), context));
    }
}
