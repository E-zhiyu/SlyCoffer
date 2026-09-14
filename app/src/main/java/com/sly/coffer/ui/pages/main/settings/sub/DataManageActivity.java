package com.sly.coffer.ui.pages.main.settings.sub;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.automation.workers.WorkerScheduler;
import com.sly.coffer.automation.workers.backup.BackupWorker;
import com.sly.coffer.automation.workers.backup.RestoreWorker;
import com.sly.coffer.auxiliary.enums.types.BackupDataType;
import com.sly.coffer.auxiliary.enums.ChannelInfo;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.NotificationID;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.data.save.preference.AutoBackupPreference;
import com.sly.coffer.databinding.ActivityDataManageBinding;
import com.sly.coffer.auxiliary.enums.settings.BackupFrequency;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.NotificationHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.helpers.file.SAFHelper;
import com.sly.coffer.helpers.file.ZipHelper;
import com.sly.coffer.ui.others.dialogs.MultiChoiceDialogBuilder;
import com.sly.coffer.ui.others.dialogs.ProgressDialogBuilder;
import com.sly.coffer.ui.pages.main.settings.components.SettingClickableTextView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSpinnerView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSwitchView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DataManageActivity extends AppCompatActivity {
    private ActivityDataManageBinding binding;  //绑定的XML布局
    private final CompositeDisposable disposables = new CompositeDisposable();      //多线程任务列表
    private List<Boolean> exportChoiceStatList = null;                              //导出数据时的选项选择情况
    private ActivityResultLauncher<Intent> importDataLauncher, exportDataLauncher;  //活动启动器
    private ActivityResultLauncher<Intent> importOldDataLauncher;                   //导入旧版数据的启动器
    private ActivityResultLauncher<Intent> backupDirSelectLauncher;                 //自动备份目录选择启动器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataManageBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollView.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom + AppearanceHelper.dpToPx(this, 15)
            );
            return insets;
        });

        initLaunchers();
        initViews();
        initPermissionRequests();
    }

    /**
     * 初始化活动启动器
     */
    private void initLaunchers() {
        exportDataLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
                        FileHelper.clearTempDataDir(this);
                        return;
                    }

                    exportData(data.getData(), exportChoiceStatList);
                }
        );

        importDataLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
                        FileHelper.clearTempDataDir(this);
                        return;
                    }

                    showImportChoiceDialog(data.getData(), false);
                }
        );

        importOldDataLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
                        FileHelper.clearTempDataDir(this);
                        return;
                    }

                    showImportChoiceDialog(data.getData(), true);
                }
        );

        backupDirSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
                        return;
                    }

                    //请求持久化权限
                    Uri backupDirUri = data.getData();
                    getContentResolver().takePersistableUriPermission(
                            backupDirUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

                    //保存备份目录 Uri
                    AutoBackupPreference.setBackupDirectoryUri(this, backupDirUri.toString());

                    //显示备份目录
                    String display = SAFHelper.getReadablePathFromSafUri(this, backupDirUri);
                    binding.backupDirectoryOption.descriptionText.setText(display);
                }
        );
    }

    /**
     * 初始化数据管理条目
     */
    private void initViews() {
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        //导出数据
        SettingClickableTextView exportDataOption = new SettingClickableTextView(
                this,
                binding.exportDataOption,
                R.string.export_data,
                "将应用数据以文件形式保存",
                R.drawable.outline_file_export_24,
                RadiusStyle.TOP
        );
        exportDataOption.setFunctionListener(v -> showExportChoiceDialog());

        //导入数据
        SettingClickableTextView importDataOption = new SettingClickableTextView(
                this,
                binding.importDataOption,
                R.string.import_data,
                "长按可导入v1.9.0之前的数据",
                R.drawable.outline_download_24,
                RadiusStyle.BOTTOM
        );
        importDataOption.setFunctionListener(v -> SAFHelper.openDocumentViaSAF(
                new String[]{"application/zip"},
                importDataLauncher
        ));
        importDataOption.setOnLongClickListener(view -> {
            SAFHelper.openDocumentViaSAF(
                    new String[]{"application/zip"},
                    importOldDataLauncher
            );
            return true;
        });

        //自动备份开关
        SettingSwitchView autoBackupSwitchOption = new SettingSwitchView(
                this,
                binding.autoBackupOption,
                R.string.auto_backup,
                "自动生成备份文件至指定位置",
                R.drawable.outline_settings_backup_restore_24,
                RadiusStyle.TOP
        );
        String backupDir = AutoBackupPreference.getBackupDirectoryUri(this);
        boolean switchStat = AutoBackupPreference.getSwitchStat(this);
        autoBackupSwitchOption.setChecked(!backupDir.isEmpty() && switchStat);
        autoBackupSwitchOption.setFunctionListener((buttonView, isChecked) -> {
            String oldDir = AutoBackupPreference.getBackupDirectoryUri(this);
            if (oldDir.isEmpty() && isChecked) {    //备份目录无效则先提示设置
                buttonView.setChecked(false);
                new MaterialAlertDialogBuilder(this)
                        .setTitle("未设置备份目录")
                        .setMessage("该功能需要先设置备份文件存储目录，请点击“确定”按钮设置存储目录，然后再开启该功能。")
                        .setNegativeButton("取消", (dialog, which) -> dialog.cancel())
                        .setPositiveButton("确定", (dialog, which) -> {
                            String backupDirUri = AutoBackupPreference.getBackupDirectoryUri(this);
                            SAFHelper.openDocumentTreeViaSAF(backupDirUri, backupDirSelectLauncher);
                        })
                        .show();
                return;
            } else if (isChecked) {
                int frequencyIndex = AutoBackupPreference.getBackupFrequency(this);
                long intervalMillis = BackupFrequency.values()[frequencyIndex].getIntervalMillis();
                WorkerScheduler.schedulePeriodicBackup(this, intervalMillis, TagStrings.BACKUP_WORKER.t(), BackupWorker.class);
            } else {
                WorkerScheduler.cancelUniqueWork(this, TagStrings.BACKUP_WORKER.t());
            }

            AutoBackupPreference.setSwitchStat(this, isChecked);
        });

        //备份频率
        SettingSpinnerView backupFrequencyOption = new SettingSpinnerView(
                this,
                binding.backupFrequencyOption,
                R.string.backup_frequency,
                "自动备份的时间间隔",
                R.drawable.outline_timer_24,
                RadiusStyle.MIDDLE
        );
        int frequencyIndex = AutoBackupPreference.getBackupFrequency(this);
        backupFrequencyOption.setSpinnerText(
                BackupFrequency.values()[frequencyIndex].getTitle()
        );
        backupFrequencyOption.setFunctionListener(v -> {
            PopupMenu frequencyMenu = new PopupMenu(this, backupFrequencyOption.getFunctionComponent());

            //填充选项
            for (BackupFrequency frequency : BackupFrequency.values()) {
                int groupId = frequency.getGroupId();
                int itemId = frequency.getItemId();
                int order = frequency.getOrder();
                String title = frequency.getTitle();
                frequencyMenu.getMenu().add(groupId, itemId, order, title);
            }

            //设置监听
            frequencyMenu.setOnMenuItemClickListener(item -> {
                boolean isItemClicked = false;

                //获取选项编号列表
                List<Integer> itemIdList = Arrays.stream(BackupFrequency.values())
                        .map(BackupFrequency::getItemId)
                        .collect(Collectors.toList());

                //判断是否选中
                if (itemIdList.contains(item.getItemId())) {
                    int index = itemIdList.indexOf(item.getItemId());
                    backupFrequencyOption.setSpinnerText(item.getTitle());
                    isItemClicked = true;

                    int oldIndex = AutoBackupPreference.getBackupFrequency(this);  //获取之前的频率代码防止重复更新工作
                    if (oldIndex != index) {
                        BackupFrequency frequency = BackupFrequency.values()[index];
                        String title = frequency.getTitle();
                        backupFrequencyOption.setSpinnerText(title);
                        AutoBackupPreference.setBackupFrequency(this, index);

                        //只有开关打开时才更新工作内容并安排一次备份
                        if (AutoBackupPreference.getSwitchStat(this)) {
                            //更新工作内容
                            long intervalMillis = frequency.getIntervalMillis();
                            WorkerScheduler.schedulePeriodicBackup(this, intervalMillis, TagStrings.BACKUP_WORKER.t(), BackupWorker.class);

                            //立即备份一次
                            WorkerScheduler.executeWorkOnceNow(this, BackupWorker.class);
                        }
                    }
                }

                return isItemClicked;
            });

            frequencyMenu.show();
        });

        //备份目录
        SettingClickableTextView backupDirectoryOption = new SettingClickableTextView(
                this,
                binding.backupDirectoryOption,
                R.string.backup_directory,
                "备份文件存储的位置",
                R.drawable.outline_folder_data_24,
                RadiusStyle.BOTTOM
        );
        backupDirectoryOption.setFunctionListener(v -> {
            String backupDirUri = AutoBackupPreference.getBackupDirectoryUri(this);
            SAFHelper.openDocumentTreeViaSAF(backupDirUri, backupDirSelectLauncher);
        });
        String uriStr = AutoBackupPreference.getBackupDirectoryUri(this);
        if (!uriStr.isEmpty()) {
            String path = SAFHelper.getReadablePathFromSafUri(this, Uri.parse(uriStr));
            binding.backupDirectoryOption.descriptionText.setText(path);
        }
    }

    /**
     * 初始化权限请求
     */
    private void initPermissionRequests() {
        PermissionHelper helper = new PermissionHelper(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            helper.addPermission(Manifest.permission.POST_NOTIFICATIONS, "请授予通知权限以发送操作进度通知");
        }
    }

    /**
     * 显示导出数据时的多选对话框
     */
    private void showExportChoiceDialog() {
        //实例化选项列表
        List<MultiChoiceDialogBuilder.ChoiceItem> itemList = Arrays.stream(BackupDataType.values())
                .map(backupDataType ->
                        new MultiChoiceDialogBuilder.ChoiceItem(true, backupDataType.getTitle(), true)
                )
                .collect(Collectors.toList());

        //显示多选对话框
        new MultiChoiceDialogBuilder(this, "导出数据", itemList)
                .setPositiveButton("确定", checkedStatList -> {
                    //判断是否没有选择任何一个选项
                    if (checkedStatList.stream().noneMatch(Boolean::booleanValue)) {
                        Toast.makeText(this, "请选择至少一个选项", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //保存选择结果引用
                    exportChoiceStatList = checkedStatList;

                    //打开 SAF 用于创建压缩包文件
                    SAFHelper.createDocumentViaSAF(
                            "application/zip",
                            FileHelper.generateBackupFileName(),
                            exportDataLauncher
                    );
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 导出数据到文件
     *
     * @param uri          用户通过 SAF 创建的 zip 文件的 Uri
     * @param checkedStats 备份数据选项选择情况
     */
    private void exportData(Uri uri, @NonNull List<Boolean> checkedStats) {
        //构建传递给 Worker 的 Data
        boolean[] choices = new boolean[checkedStats.size()];
        for (int i = 0; i < checkedStats.size(); i++) {
            choices[i] = checkedStats.get(i);
        }
        Data data = new Data.Builder()
                .putString(KeyStrings.BACKUP_TARGET.v(), uri.toString())
                .putBooleanArray(KeyStrings.BACKUP_CHOICES.v(), choices)
                .build();

        //执行一次任务
        UUID uuid = WorkerScheduler.executeWorkOnceNow(this, BackupWorker.class, data);

        //显示进度条对话框
        AlertDialog progressDialog = new ProgressDialogBuilder(this, "导出数据", "正在导出数据……")
                .setPositiveButton("后台执行", (dialogInterface, i) ->
                        sendBackgroundProgress("导出数据", "正在导出数据……")
                )
                .setNegativeButton("取消执行", (dialogInterface, i) ->
                        WorkerScheduler.cancelWorkById(this, uuid)
                )
                .show();

        //监听运行状态
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(uuid)
                .observe(this, workInfo -> {
                    if (workInfo == null) return;

                    //判断 Worker 是否运行结束
                    if (workInfo.getState().isFinished()) {
                        String text;
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                text = "数据导出成功";
                                break;
                            case CANCELLED:
                                text = "数据导出已取消";
                                break;
                            case FAILED:
                            default:
                                text = "数据导出失败";
                        }

                        if (progressDialog.isShowing()) {
                            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                        } else {
                            //构建通知
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannelInfo.BACKUP_AND_RESTORE.getId())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("导出数据")
                                    .setContentText(text)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setCategory(NotificationCompat.CATEGORY_PROGRESS);

                            //发送通知
                            NotificationHelper.sendNotification(
                                    NotificationID.BACKUP_AND_RESTORE.ordinal(),
                                    builder,
                                    this
                            );
                        }

                        progressDialog.dismiss();
                    }
                });
    }

    /**
     * 扫描备份文件并显示多选对话框
     *
     * @param uri       SAF 返回的 Uri 实例
     * @param isOldData 是否是旧版数据
     */
    private void showImportChoiceDialog(Uri uri, boolean isOldData) {
        //显示扫描文件的进度条对话框
        AlertDialog progressDialog = new ProgressDialogBuilder(this, "扫描文件", "正在扫描文件……")
                .setNegativeButton("取消", (dialogInterface, i) -> {
                    disposables.clear();
                    Toast.makeText(this, "已取消数据导入", Toast.LENGTH_SHORT).show();
                })
                .show();

        //扫描压缩包并显示多选对话框
        disposables.add(ZipHelper.scanBackupFile(uri, this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(fileNameSet -> {
                    progressDialog.dismiss();

                    //实例化选项列表
                    List<MultiChoiceDialogBuilder.ChoiceItem> itemList = Arrays.stream(BackupDataType.values())
                            .map(backupDataType -> {
                                if (!isOldData && fileNameSet.contains(backupDataType.getFileName()) ||
                                        isOldData && fileNameSet.contains(backupDataType.getOldFileName())) {
                                    return new MultiChoiceDialogBuilder.ChoiceItem(
                                            true,
                                            backupDataType.getTitle(),
                                            true
                                    );
                                } else {
                                    return new MultiChoiceDialogBuilder.ChoiceItem(
                                            false,
                                            backupDataType.getTitle(),
                                            false
                                    );
                                }
                            })
                            .collect(Collectors.toList());

                    //显示多选对话框
                    new MultiChoiceDialogBuilder(this, "导入数据", itemList)
                            .setPositiveButton("确认", checkedStatList ->
                                    importData(uri, checkedStatList)
                            )
                            .setNegativeButton("取消", null)
                            .show();
                }, e -> {
                    progressDialog.dismiss();
                    ExceptionHelper.showExceptionDialog(this, e);
                })
        );
    }

    /**
     * 将用户选择的数据导入到数据库中
     *
     * @param uri          备份文件的 Uri
     * @param checkedStats 用户选择的选项状态，选项的下标与{@link BackupDataType}的枚举序数一一对应
     */
    private void importData(Uri uri, @NonNull List<Boolean> checkedStats) {
        //构建传递给 Worker 的 Data
        boolean[] choices = new boolean[checkedStats.size()];
        for (int i = 0; i < checkedStats.size(); i++) {
            choices[i] = checkedStats.get(i);
        }
        Data data = new Data.Builder()
                .putString(KeyStrings.BACKUP_TARGET.v(), uri.toString())
                .putBooleanArray(KeyStrings.BACKUP_CHOICES.v(), choices)
                .build();

        //执行一次任务
        UUID uuid = WorkerScheduler.executeWorkOnceNow(this, RestoreWorker.class, data);

        //显示扫描文件的进度条对话框
        AlertDialog progressDialog = new ProgressDialogBuilder(this, "导入数据", "正在导入数据……")
                .setPositiveButton("后台执行", (dialogInterface, i) ->
                        sendBackgroundProgress("导入数据", "正在导入数据……")
                )
                .setNegativeButton("取消执行", (dialogInterface, i) ->
                        WorkerScheduler.cancelWorkById(this, uuid)
                )
                .show();

        //监听运行状态
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(uuid)
                .observe(this, workInfo -> {
                    if (workInfo == null) return;

                    //判断 Worker 是否运行结束
                    if (workInfo.getState().isFinished()) {
                        String text;
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                text = "数据导入成功";
                                break;
                            case CANCELLED:
                                text = "数据导入已取消";
                                break;
                            case FAILED:
                            default:
                                text = "数据导入失败";
                        }

                        if (progressDialog.isShowing()) {
                            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                        } else {
                            //构建通知
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannelInfo.BACKUP_AND_RESTORE.getId())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("导入数据")
                                    .setContentText(text)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setCategory(NotificationCompat.CATEGORY_PROGRESS);

                            //发送通知
                            NotificationHelper.sendNotification(
                                    NotificationID.BACKUP_AND_RESTORE.ordinal(),
                                    builder,
                                    this
                            );
                        }

                        progressDialog.dismiss();
                    }
                });
    }

    /**
     * 发送处于后台运行状态的进度条通知
     *
     * @param title   标题
     * @param content 文本内容
     */
    private void sendBackgroundProgress(String title, String content) {
        //构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannelInfo.BACKUP_AND_RESTORE.getId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        //发送通知
        NotificationHelper.sendNotification(
                NotificationID.BACKUP_AND_RESTORE.ordinal(),
                builder,
                this
        );
    }
}