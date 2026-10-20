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
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.types.ImportDataVersionType;
import com.sly.coffer.auxiliary.enums.types.ImportResultType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.file.ZipHelper;

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
        int importDataVersionTypeOrdinal = inputData.getInt(
                KeyStrings.IMPORT_DATA_VERSION_TYPE.v(),
                ImportDataVersionType.values().length - 1
        );

        //判断输入参数是否为空
        if (inputUriStr == null) {
            Log.e(LogTags.RESTORE_WORKER.n(), "无法获取恢复文件的位置");
            return Single.just(Result.failure());
        }

        //判断选择结果
        if (choices == null) {
            Log.e(LogTags.RESTORE_WORKER.n(), "无法获取需要导入的数据类型");
            return Single.just(Result.failure());
        }

        //获取需要解压的文件名列表
        List<String> allowedFileNameList = Arrays.stream(BackupDataType.values())
                .filter(type -> choices[type.ordinal()])
                .map(BackupDataType::getLatestVersionFileName)
                .collect(Collectors.toList());
        boolean includeMedia = choices[BackupDataType.RUNNING_ACCOUNT.ordinal()];

        //解压文件并写入数据
        sendProgressNotification(context);  //发送通知
        Uri uri = Uri.parse(inputUriStr);
        List<Integer> resultFlagList = Arrays.stream(BackupDataType.values())
                .map(type -> ImportResultType.MISSING.ordinal())
                .collect(Collectors.toList());  //存有数据导入结果的列表
        ImportDataVersionType[] versionTypes = ImportDataVersionType.values();
        return ZipHelper.unpackBackupFileWithFilter(context, uri, allowedFileNameList, includeMedia)
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(file -> {
                    //根据文件名判断数据类型
                    ImportDataVersionType importDataVersionType = versionTypes[importDataVersionTypeOrdinal];
                    BackupDataType type = BackupDataType.fromFileName(file.getName(), importDataVersionType);

                    //使用对应的备份Helper导入数据
                    if (type != null) {
                        BackupHelperBase<?, ?> helper = type.createBackupHelper(context);
                        return helper.importDataFromTempFile(context, file, importDataVersionType)
                                .flatMapCompletable(resultFlag -> {
                                    resultFlagList.set(type.ordinal(), resultFlag); //更新结果
                                    return Completable.complete();
                                })
                                .onErrorComplete(e -> {
                                    resultFlagList.set(type.ordinal(), ImportResultType.JSON_ERROR.ordinal());
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
                    if (resultFlagList.stream().allMatch(flag -> flag == ImportResultType.SUCCEED.ordinal())) {
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
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_CALL)
                            .setAutoCancel(true);
                    NotificationHelper.sendNotification(NotificationID.BACKUP_AND_RESTORE.ordinal(), notificationBuilder, context);
                });
    }

    /**
     * 根据数据导入情况生成通知内容
     *
     * @param resultFlagList 数据导入标识符列表
     * @param choices        用户导入数据的选择情况
     * @return 数据导入反馈通知的内容
     */
    @NonNull
    private static StringBuilder getStrBuilder(@NonNull List<Integer> resultFlagList, boolean[] choices) {
        StringBuilder strBuilder;
        if (resultFlagList.contains(ImportResultType.SUCCEED.ordinal())) {
            strBuilder = new StringBuilder("部分数据导入成功");
        } else {
            strBuilder = new StringBuilder("所有数据均导入失败");
        }

        //根据结果生成反馈信息
        ImportResultType[] resultTypes = ImportResultType.values();
        for (BackupDataType dataType : BackupDataType.values()) {
            ImportResultType resultType = resultTypes[resultFlagList.get(dataType.ordinal())];
            if (resultType != ImportResultType.SUCCEED &&
                    choices[dataType.ordinal()]) {
                strBuilder.append("，“").append(dataType.getTitle()).append("”").append(resultType.getTitle());
            }
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
