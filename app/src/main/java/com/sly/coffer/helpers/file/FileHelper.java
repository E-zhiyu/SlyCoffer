package com.sly.coffer.helpers.file;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.DirectoryPaths;
import com.sly.coffer.auxiliary.enums.unique.LogTags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.rxjava3.core.Completable;

public class FileHelper {
    /**
     * 获取文件扩展名
     */
    @Nullable
    public static String getFileExtension(@NonNull String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return null;
    }

    /**
     * 生成备份文件名称
     *
     * @return 备份文件名称
     */
    @NonNull
    public static String generateBackupFileName() {
        return String.format(
                Locale.getDefault(),
                "Sly's Coffer Backup_%s.zip",
                LocalDateTime.now().format(CustomDateTimeFormatter.BACKUP)
        );
    }

    /**
     * 分享单个图片
     *
     * @param context    上下文
     * @param targetFile 需要分享的图片
     * @param intentType 指明文件类型的字符串
     */
    public static Completable shareFileCompletable(Context context, File targetFile, String intentType) {
        return Completable.defer(() -> {
            if (targetFile == null || !targetFile.exists()) {
                Log.e(LogTags.FILE_HELPER.n(), "分享的文件不存在");
                return Completable.error(new RuntimeException("分享的文件不存在"));
            }

            //获取 content 类型的 Uri
            Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    targetFile
            );

            //创建分享 Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(intentType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, targetFile.getName());

            //授予临时权限
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(shareIntent, targetFile.getName());
            if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooserIntent);
                return Completable.complete();
            } else {
                return Completable.error(new RuntimeException("未找到可分享的应用"));
            }
        });
    }

    /**
     * 获取可用的文件名，当文件名被占用时自动添加编号以防文件名冲突
     *
     * @param targetDir  可能存在文件名占用情况的目录
     * @param originName 原始文件名
     * @return 没有被占用的文件名
     */
    @NonNull
    private static String getAvailableFilename(File targetDir, @NonNull String originName) {
        String[] namePart = originName.split("\\.", 2);
        String suffix = namePart[1];            //后缀名（不包含.）
        String originFileName = namePart[0];    //不包含后缀名的文件名

        String availableName = originName;
        File targetFile = new File(targetDir, availableName);

        //循环生成可用文件名
        int i = 0;
        while (targetFile.exists()) {
            i++;
            availableName = String.format(Locale.getDefault(), "%s (%d).%s", originFileName, i, suffix);
            targetFile = new File(targetDir, availableName);
        }

        return availableName;
    }

    /**
     * 删除文件
     *
     * @param uri     待删除文件的 Uri
     * @param context 上下文
     */
    public static void deleteFile(@NonNull Uri uri, Context context) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            // 如果是绝对路径的 file:// 协议临时文件
            File file = new File(Objects.requireNonNull(uri.getPath()));
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(LogTags.FILE_HELPER.n(), "File delete: " + deleted + " -> " + file.getAbsolutePath());
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // 如果是通过 FileProvider 生成的 content:// 协议临时文件
            context.getContentResolver().delete(uri, null, null);
            Log.d(LogTags.FILE_HELPER.n(), "ContentUri delete success: " + uri);
        }
    }

    /**
     * 移动文件
     *
     * @param originFile 待移动的文件的 Uri
     * @param targetDir  存放移动后目标文件的目录
     * @return 移动完成后的文件对象，移动失败则返回 null
     */
    @Nullable
    public static File moveFile(File originFile, File targetDir) {
        if (targetDir == null || !targetDir.isDirectory()) {
            throw new IllegalArgumentException("参数错误，targetDir不是目录对象");
        }

        //判断文件是否存在
        if (originFile == null || !originFile.exists()) {
            Log.w(LogTags.FILE_HELPER.n(), "待移动的文件不存在");
            return null;
        }

        //获取 Path 对象
        Path originFilePath = originFile.toPath();
        File targetFile = new File(targetDir, getAvailableFilename(targetDir, originFile.getName()));
        Path targetPath = targetFile.toPath();

        //移动文件
        try {
            Path movedPath = Files.move(
                    originFilePath,
                    targetPath
            );

            Log.i(LogTags.FILE_HELPER.n(), "文件移动成功");
            return movedPath.toFile();
        } catch (IOException e) {
            Log.e(LogTags.FILE_HELPER.n(), "文件移动失败");
            return null;
        }
    }

    /**
     * 复制文件
     *
     * @param context   上下文
     * @param uri       待复制的文件的Uri
     * @param targetDir 存放复制的文件的目录
     * @param buffer    缓冲区，用于复制多个文件时共享，减小内存压力
     * @return 复制成功则返回复制成功的文件，失败则返回 null
     * @throws IOException 文件复制失败引发的异常
     */
    @Nullable
    public static File copyFile(Context context, Uri uri, File targetDir, @Nullable byte[] buffer) throws IOException {
        //判断目标目录是否存在
        if (targetDir == null || !targetDir.exists()) {
            Log.e(LogTags.FILE_HELPER.n(), "目标文件夹不存在");
            return null;
        }

        //获取或生成文件名
        String fileName = null;
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
        if (documentFile.exists()) {
            fileName = documentFile.getName();
        }
        if (fileName == null) {
            fileName = "copied_media_" + System.currentTimeMillis() + ".jpg";
        }

        //创建文件对象
        File targetFile = new File(targetDir, fileName);

        //复制文件
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(targetFile)) {

            if (inputStream == null) return null;

            if (buffer == null) {
                buffer = new byte[1024 * 32];
            }
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }

        return targetFile;
    }

    /**
     * 通过 Uri 获取文件大小
     *
     * @param context 上下文
     * @param uri     需要获取大小的文件的 Uri
     * @return 文件大小，单位为 B
     */
    public static long getFileSizeByUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return -1;
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.SIZE},
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        return cursor.getLong(sizeIndex);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    return file.length();
                }
            }
        }

        return -1;
    }

    /**
     * 通过 Uri 获取文件名
     *
     * @param context 上下文
     * @param uri     目标文件的 Uri
     * @return 文件显示名称
     */
    public static String getFileNameByUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        return cursor.getString(sizeIndex);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlashIndex = path.lastIndexOf('/');
                if (lastSlashIndex != -1) {
                    return path.substring(lastSlashIndex + 1);
                }
                return path;
            }
        }

        return null;
    }

    /**
     * 根据 Uri 获取文件最后修改时间
     *
     * @param context 上下文
     * @param uri     需要获取最后修改时间的文件的 Uri
     * @return 最后修改时间的时间戳
     */
    public static long getLastModifyTimeByUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return -1;
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{DocumentsContract.Document.COLUMN_LAST_MODIFIED},
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int dateIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                    if (dateIndex != -1 && !cursor.isNull(dateIndex)) {
                        return cursor.getLong(dateIndex);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    return file.lastModified();
                }
            }
        }

        return -1;
    }

    /**
     * 读取文本文件内容
     *
     * @param file 待读取的文本文件
     * @return 文件内容字符串
     * @throws IOException 文件内容读取失败引发的异常
     */
    @NonNull
    public static String readContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }

        return content.toString();
    }

    /**
     * 将字符串写入到临时数据文件中
     *
     * @param context  上下文
     * @param fileName 临时数据文件名称
     * @param content  文件内容
     * @throws IOException 写入失败引发的异常
     */
    public static void writeContentToTempDataFile(Context context, String fileName, String content) throws IOException {
        File tempDataDir = DirectoryPaths.DATA_TEMP.getDir(context);
        createFileAndWrite(tempDataDir, fileName, content);
    }

    /**
     * 创建文件并写入指定的字符串
     *
     * @param dir      文件所在目录
     * @param fileName 文件名称
     * @param content  文件内容
     * @throws IOException 无法将字符串写入文件时引发的异常
     */
    public static void createFileAndWrite(File dir, String fileName, String content) throws IOException {
        File targetFile = new File(dir, fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write(content);
        }
    }

    /**
     * 清空临时数据文件目录
     *
     * @param context 上下文
     */
    public static void clearTempDataDir(Context context) {
        File tempDataDir = DirectoryPaths.DATA_TEMP.getDir(context);
        clearDir(tempDataDir);
    }

    /**
     * 清空媒体文件夹
     *
     * @param context 上下文
     */
    public static void clearMediaDir(Context context) {
        File mediaDir = DirectoryPaths.MEDIA.getDir(context);
        clearDir(mediaDir);
    }

    /**
     * 清空临时媒体目录
     *
     * @param context 上下文
     */
    public static void clearMediaTempDir(Context context) {
        File mediaTempDir = DirectoryPaths.MEDIA_TEMP.getDir(context);
        clearDir(mediaTempDir);
    }

    /**
     * 清理某个目录
     *
     * @param dir 需要被清理的目录
     */
    public static void clearDir(@Nullable File dir) {
        //判断目录是否可用
        Log.i(LogTags.DATA_IO_HELPER.n(), "开始清除文件……");
        if (dir == null || !dir.isDirectory() || !dir.exists()) {
            Log.e(LogTags.DATA_IO_HELPER.n(), "非目录对象或目录不存在");
            return;
        }

        //获取文件列表
        File[] files = dir.listFiles();
        if (files == null) {
            Log.e(LogTags.DATA_IO_HELPER.n(), "无法获取文件列表");
            return;
        }

        //遍历删除列表中的文件
        for (File file : files) {
            if (!file.delete()) {
                Log.w(LogTags.DATA_IO_HELPER.n(), "无法删除文件：" + file.getName());
            }
        }

        Log.i(LogTags.DATA_IO_HELPER.n(), "文件清理完毕");
    }
}