package com.sly.coffer.helpers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.automation.workers.backup.BackupWorker;
import com.sly.coffer.data.save.preference.VersionPreference;
import com.sly.coffer.automation.workers.WorkerScheduler;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UpdateHelper {
    private static final String RELEASE_FILE_NAME = "Sly.s-Coffer";
    private static final String REPOSITORY_ADDRESS = "https://gitee.com/e-zhiyu/sly-coffer";
    private static final String VERSION_INFO_PART = "/raw/main/VERSION.json";
    private static final String CHANGE_LOG_PART = "/raw/main/CHANGELOG.md";

    @JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
    static class VersionInfo {
        private long versionCode;   //版本代码
        private String versionName; //版本名称
        private String changeLog;   //更新日志

        public VersionInfo() {
        }

        public long getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(long versionCode) {
            this.versionCode = versionCode;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public String getChangeLog() {
            return changeLog;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }
    }

    /**
     * 检查更新
     *
     * @param context    上下文
     * @param disposable 多线程任务订阅列表
     * @param isManual   是否为手动检测更新
     */
    public static void checkUpdate(
            Context context,
            @NonNull CompositeDisposable disposable,
            boolean isManual
    ) {
        Scheduler scheduler = Schedulers.newThread();
        disposable.add(readVersionInfoFromRemote()
                .flatMapMaybe(info -> {
                    String versionName = info.getVersionName();
                    if (!versionName.startsWith("v")) {
                        info.setVersionName("v" + versionName);
                    }
                    long versionCode = info.getVersionCode();

                    //与跳过的版本比较
                    long skippedVersionCode = VersionPreference.getSkipVersionCode(context);
                    if (!isManual && versionCode <= skippedVersionCode) {
                        return Maybe.empty();
                    }

                    //获取更新日志
                    String changeLog = readChangeLogFromRemote(versionName);
                    if (!changeLog.isEmpty()) {
                        info.setChangeLog(changeLog);
                    } else {
                        info.setChangeLog("修复了一些已知问题🥰");
                    }

                    //返回带有更新日志的版本信息
                    return Maybe.just(info);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .subscribe(
                        info -> {
                            String versionName = info.getVersionName();
                            long versionCode = info.getVersionCode();
                            String changeLog = info.getChangeLog();

                            long currentVersionCode = AboutHelper.getVersionCode(context);
                            if (currentVersionCode < versionCode) {
                                new MarkdownDialogBuilder(context, "发现新版本", changeLog)
                                        .setNegativeButton("跳过", (dialogInterface, i) ->
                                                VersionPreference.setSkipVersionCode(context, versionCode)
                                        )
                                        .setPositiveButton("更新", (dialogInterface, i) -> {
                                            //下载安装包时就自动备份一次，防止数据丢失
                                            WorkerScheduler.executeWorkOnceNow(context, BackupWorker.class);

                                            Toast.makeText(context, "正在下载安装包，请在通知栏查看进度", Toast.LENGTH_SHORT).show();
                                            downloadLatestFile(context, versionName);
                                        })
                                        .show();
                            } else if (isManual) {
                                Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show();
                            }
                        },
                        e -> {
                            if (!isManual) return;

                            if (e instanceof ProtocolException) {
                                Toast.makeText(context, "未知的远程主机名", Toast.LENGTH_SHORT).show();
                            } else if (e instanceof FileNotFoundException) {
                                Toast.makeText(context, "无法获取最新版本", Toast.LENGTH_SHORT).show();
                            } else if (e instanceof SocketTimeoutException) {
                                Toast.makeText(context, "与服务器的连接超时", Toast.LENGTH_SHORT).show();
                            } else if (e instanceof ConnectException || e instanceof UnknownHostException) {
                                Toast.makeText(context, "请检查网络连接", Toast.LENGTH_SHORT).show();
                            } else {
                                ExceptionHelper.showExceptionDialog(context, e);
                            }
                        }
                )
        );
    }

    /**
     * 从远程地址中读取版本信息文本
     *
     * @return 读取到的字符串
     */
    @NonNull
    private static Single<VersionInfo> readVersionInfoFromRemote() {
        return Single.defer(() -> {
            //获取连接
            URL url = new URL(REPOSITORY_ADDRESS + VERSION_INFO_PART);
            HttpsURLConnection versionConnection = (HttpsURLConnection) url.openConnection();
            versionConnection.setRequestMethod("GET");
            versionConnection.setConnectTimeout(5_000);    //设置连接超时
            versionConnection.setReadTimeout(5_000);       //设置读取超时

            //获取 JSON 字符串
            BufferedReader reader = new BufferedReader(new InputStreamReader(versionConnection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String versionLine;
            while ((versionLine = reader.readLine()) != null) {
                content.append(versionLine);
                content.append("\n");
            }
            reader.close();

            //解析为 Java 对象
            ObjectMapper mapper = new ObjectMapper();
            VersionInfo info = mapper.readValue(content.toString(), VersionInfo.class);

            return Single.just(info);
        });
    }

    /**
     * 从远程地址中读取更新日志
     *
     * @param targetVersion 目标版本
     * @return 读取到的更新日志内容
     * @throws ConnectException       无法创建连接时抛出的异常
     * @throws UnknownHostException   无法解析主机名时抛出的异常（例如没有网络时）
     * @throws SocketTimeoutException 连接超时异常
     */
    @NonNull
    private static String readChangeLogFromRemote(@NonNull String targetVersion) throws IOException {
        //获取连接
        URL url = new URL(REPOSITORY_ADDRESS + CHANGE_LOG_PART);
        HttpsURLConnection versionConnection = (HttpsURLConnection) url.openConnection();
        versionConnection.setRequestMethod("GET");
        versionConnection.setConnectTimeout(5_000);    //设置连接超时
        versionConnection.setReadTimeout(5_000);       //设置读取超时

        //获取输入流
        BufferedReader reader = new BufferedReader(new InputStreamReader(versionConnection.getInputStream()));
        StringBuilder content = new StringBuilder();

        //逐行解析
        boolean isCollecting = false;   //是否正在将读取到的行保存到 StringBuilder 中
        final String VERSION_REGEX = "^#\\s+v.*";
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();

            // 检测是否匹配版本号标题（例如：# v1.9.0）
            if (trimmedLine.matches(VERSION_REGEX)) {
                if (isCollecting) {
                    // 如果已经在读取目标版本的日志，遇到下一个版本号说明目标日志已读取完毕，直接跳出循环
                    break;
                }

                // 判断是否是我们要找的目标版本
                if (trimmedLine.contains(targetVersion)) {
                    isCollecting = true;
                    content.append(line).append("\n");
                }
            } else if (isCollecting) {
                // 在目标版本日志范围内，直接拼接行
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    /**
     * 下载新版安装包
     *
     * @param context     上下文
     * @param versionName 版本名称
     * @throws IllegalArgumentException 更新链接无效时引发的异常
     */
    private static void downloadLatestFile(
            @NonNull Context context,
            String versionName
    ) throws IllegalArgumentException {
        //生成文件名
        String fileName = String.format("%s_%s.apk", RELEASE_FILE_NAME, versionName);

        //生成下载链接
        String downloadUrl = String.format(
                Locale.getDefault(),
                "%s/releases/download/%s/%s",
                REPOSITORY_ADDRESS,
                versionName,
                fileName
        );

        //请求下载
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setTitle("软件更新");
        request.setDescription(String.format(
                Locale.getDefault(),
                "正在更新“%s”……",
                context.getString(R.string.app_name)
        ));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        //设置下载路径
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        //获取下载服务
        DownloadManager downloadManager = context.getSystemService(DownloadManager.class);
        long downloadId = downloadManager.enqueue(request);

        //实例化下载完成的广播接收器
        BroadcastReceiver downloadFinishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    //检测下载完成的状态
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(query);

                    if (cursor.moveToFirst()) {
                        try {
                            //获取下载状态
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (columnIndex == -1)
                                throw new RuntimeException("无法获取安装包状态，请手动安装安装包");
                            int status = cursor.getInt(columnIndex);

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                //获取文件URI
                                columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                if (columnIndex == -1)
                                    throw new RuntimeException("无法获取安装包URI");
                                String fileUriStr = cursor.getString(columnIndex);
                                VersionPreference.setApkUri(context, fileUriStr);

                                //显示弹窗提醒用户安装
                                new MaterialAlertDialogBuilder(context)
                                        .setTitle("安装更新")
                                        .setMessage("安装包已下载完毕，是否立刻更新？")
                                        .setPositiveButton("立刻更新", (dialog, which) -> {
                                            Uri contentUri = downloadManager.getUriForDownloadedFile(downloadId);
                                            installLatestApk(context, contentUri);
                                        })
                                        .setNegativeButton("取消", null)
                                        .show();
                            } else {
                                throw new IllegalArgumentException("安装包下载失败");
                            }
                        } catch (Exception e) {
                            ExceptionHelper.showExceptionDialog(context, e);
                        }
                    }
                    cursor.close();
                }

                //注销自身
                context.unregisterReceiver(this);
            }
        };

        //注册下载完毕监听器
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadFinishReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            ContextCompat.registerReceiver(context, downloadFinishReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    /**
     * 安装下载好的APK安装包
     *
     * @param context 上下文
     * @param fileUri 安装包的 content 类型 Uri
     */
    private static void installLatestApk(@NonNull Context context, Uri fileUri) {
        //启动安装逻辑
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}