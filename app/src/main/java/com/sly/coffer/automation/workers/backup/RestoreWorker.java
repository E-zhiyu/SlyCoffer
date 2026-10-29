package com.sly.coffer.automation.workers.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.ImportResult;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.types.ImportDataVersionType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.file.ZipHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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

        //判断输入参数是否为空
        if (inputUriStr == null) {
            Log.e(LogTags.RESTORE_WORKER.n(), "无法获取恢复文件的位置");
            return Single.just(Result.failure());
        }

        //解压文件并写入数据
        sendProgressNotification(context);  //发送通知
        Uri uri = Uri.parse(inputUriStr);
        List<ImportResult> resultFlagList = Arrays.stream(BackupDataType.values())
                .map(type -> choices == null || choices[type.ordinal()] ?
                        ImportResult.missed() : ImportResult.success()
                )
                .collect(Collectors.toList());  //存有数据导入结果的列表
        AtomicReference<ImportDataVersionType> versionType = new AtomicReference<>();
        return Single.fromCallable(() -> ZipHelper.getDataInfoFromBackupFile(context, uri))
                .flatMap(infoMap -> {
                    if (infoMap == null) {
                        throw new RuntimeException("无法读取备份文件信息，请选择正确的备份文件");
                    }

                    //识别数据版本
                    versionType.set(ImportDataVersionType.getDataVersionByVersionCode(infoMap.getAppVersionCode()));

                    //获取需要解压的文件名
                    Set<String> allowedFileNameSet = new HashSet<>();
                    List<String> fileNameList = infoMap.getFileNames();
                    for (int i = 0; i < fileNameList.size(); i++) {
                        String fileName = fileNameList.get(i);
                        if (choices == null || choices[i] && fileName != null) {
                            allowedFileNameSet.add(fileName);
                        }
                    }

                    boolean includeMedia = choices == null || choices[BackupDataType.RUNNING_ACCOUNT.ordinal()] &&
                            fileNameList.get(BackupDataType.RUNNING_ACCOUNT.ordinal()) != null;
                    return ZipHelper.unpackBackupFileWithFilter(context, uri, allowedFileNameSet, includeMedia);
                })
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(file -> {
                    //根据文件名判断数据类型
                    BackupDataType type = BackupDataType.fromFileName(file.getName(), versionType.get());

                    //使用对应的备份Helper导入数据
                    if (type != null) {
                        BackupHelperBase<?, ?> helper = type.createBackupHelper(context);
                        return helper.importDataFromTempFile(context, file, versionType.get())
                                .flatMapCompletable(resultFlag -> {
                                    resultFlagList.set(type.ordinal(), resultFlag); //更新结果
                                    return Completable.complete();
                                })
                                .onErrorComplete(e -> {
                                    resultFlagList.set(type.ordinal(), ImportResult.failure(e));
                                    Log.e(LogTags.RESTORE_WORKER.n(), "导入失败：\n" + e.getMessage());
                                    return true;
                                });
                    } else {
                        return Completable.complete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .toSingleDefault(Result.success())
                .doFinally(() -> {
                    String message;
                    if (resultFlagList.stream().allMatch(result -> result.getClass() == ImportResult.Success.class)) {
                        message = "所有数据均导入成功。";
                    } else {
                        StringBuilder strBuilder = getStrBuilder(resultFlagList, choices);

                        message = strBuilder.toString();
                    }

                    //发送通知
                    String channelID = ChannelInfo.BACKUP_AND_RESTORE.getId();
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(context.getString(R.string.import_data))
                            .setContentText(message)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_CALL)
                            .setAutoCancel(true);
                    NotificationHelper.sendNotification(NotificationID.BACKUP_AND_RESTORE.ordinal(), notificationBuilder, context);
                });
    }

    /**
     * 根据数据导入情况生成通知内容
     *
     * @param resultList 数据导入标识符列表
     * @param choices    用户导入数据的选择情况
     * @return 数据导入反馈通知的内容
     */
    @NonNull
    private static StringBuilder getStrBuilder(@NonNull List<ImportResult> resultList, boolean[] choices) {
        StringBuilder strBuilder = new StringBuilder("数据导入发生异常");

        //根据结果生成反馈信息
        for (BackupDataType dataType : BackupDataType.values()) {
            ImportResult result = resultList.get(dataType.ordinal());
            if (result instanceof ImportResult.Success || !choices[dataType.ordinal()]) continue;

            strBuilder.append("，“").append(dataType.getTitle()).append("”").append(result.getTitle());
        }
        return strBuilder.append("。");
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
                .setContentTitle(context.getString(R.string.import_data))
                .setContentText("正在导入数据……")
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
