package com.sly.coffer.helpers.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sly.coffer.auxiliary.enums.DirectoryPaths;
import com.sly.coffer.auxiliary.enums.unique.LogTags;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class ZipHelper {
    /**
     * 将源文件或目录打包到 ZipOutputStream
     *
     * @param sourceFile    需要添加到压缩包的文件或目录
     * @param fileNameInZip 在压缩包中的路径名称
     * @param zos           ZipOutputStream 实例，用于将文件写入压缩包
     */
    private static void addToZip(@NonNull File sourceFile, String fileNameInZip, ZipOutputStream zos) throws IOException {
        if (sourceFile.isHidden()) return;

        if (sourceFile.isDirectory()) {
            // 如果是目录，确保以 "/" 结尾
            String folderName = fileNameInZip.endsWith("/") ? fileNameInZip : fileNameInZip + "/";
            zos.putNextEntry(new ZipEntry(folderName));
            zos.closeEntry();

            File[] children = sourceFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    // 递归添加子文件，路径累加
                    addToZip(child, folderName + child.getName(), zos);
                }
            }
        } else {
            // 如果是文件，直接写入
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                ZipEntry zipEntry = new ZipEntry(fileNameInZip);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 创建备份文件压缩包
     *
     * @param tempDir      临时数据目录
     * @param imageDir     媒体目录，若为 null 则不添加媒体目录
     * @param outputStream 由 SAF Uri 转换而来的输出流
     * @param includeMedia 是否打包媒体文件
     * @throws IOException 文件写入失败引发的异常
     */
    public static void createBackupZip(
            @NonNull File tempDir,
            @Nullable File imageDir,
            OutputStream outputStream,
            boolean includeMedia
    ) throws IOException {
        // 将 ZipOutputStream 包装在外部传入的流上
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            // 1. 打包临时 JSON 目录下的所有文件（不包含目录本身）
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File file : tempFiles) {
                    addToZip(file, file.getName(), zos);
                }
            }

            // 2. 将图片目录整体添加进去
            if (includeMedia && imageDir != null && imageDir.exists() && imageDir.isDirectory()) {
                addToZip(imageDir, imageDir.getName(), zos);
            }
        }
    }

    /**
     * 生成备份文件
     *
     * @param targetUri    目标压缩包的 Uri
     * @param context      上下文
     * @param includeMedia 是否打包临时文件目录
     * @return 是否成功
     */
    public static Completable createBackupFile(Uri targetUri, Context context, boolean includeMedia) {
        File tempDir = DirectoryPaths.DATA_TEMP.getDir(context);
        File mediaDir = DirectoryPaths.MEDIA.getDir(context);

        return Completable.fromAction(() -> {
            if (tempDir == null) {
                throw new IOException("无法获取临时数据目录");
            }

            // 1. 获取 SAF 的输出流
            OutputStream os = context.getContentResolver().openOutputStream(targetUri);
            if (os == null) {
                throw new IOException("无法打开输出流，请检查权限或路径");
            }

            try {
                ZipHelper.createBackupZip(tempDir, mediaDir, os, includeMedia);
            } finally {
                // 3. 无论成功失败，清理临时 JSON 目录
                FileHelper.clearTempDataDir(context);
            }
        });
    }

    /**
     * 扫描压缩文件并返回所有位于根目录下的文件名
     *
     * @param uri     需要扫描的压缩文件的 Uri
     * @param context 上下文
     * @return 可通过 RxJava 观察的文件名字符串集合
     */
    public static Single<Set<String>> scanBackupFile(Uri uri, @NonNull Context context) {
        return Single.fromCallable(() -> {
            Set<String> fileNameSet = new HashSet<>();
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 ZipInputStream zis = new ZipInputStream(is)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    fileNameSet.add(entry.getName());
                    zis.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("压缩包扫描失败", e);
            }

            return fileNameSet;
        });
    }

    /**
     * 根据用户选择结果解压备份文件
     *
     * @param context      上下文
     * @param zipUri       压缩包的 Uri
     * @param allowedFiles 允许解压的文件名称列表
     * @param includeMedia 是否解压媒体文件
     * @return 已解压好的JSON文件列表，用于后续读取并导入数据
     */
    public static Single<List<File>> unpackBackupFileWithFilter(
            Context context,
            Uri zipUri,
            List<String> allowedFiles,
            boolean includeMedia
    ) {
        return Single.fromCallable(() -> {
            //获取存放解压文件的目标文件夹
            File tempJsonDir = DirectoryPaths.DATA_TEMP.getDir(context);
            File mediaDir = DirectoryPaths.MEDIA.getDir(context);
            String mediaDirName = DirectoryPaths.MEDIA.getChildDirName() + "/";

            //清空媒体文件目录
            if (includeMedia) {
                FileHelper.clearMediaDir(context);
            }

            //创建共享缓冲区，防止频繁GC
            List<File> extractedJsonFiles = new ArrayList<>();
            byte[] jsonBuffer = new byte[1024 * 8];
            byte[] mediaBuffer = new byte[1024 * 32];

            //开始执行解压逻辑
            try (InputStream is = context.getContentResolver().openInputStream(zipUri);
                 ZipInputStream zis = new ZipInputStream(is)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    if (name.equals("pictures.zip")) {          //处理旧版媒体文件
                        File outFile = new File(tempJsonDir, name);
                        extractFile(zis, outFile, mediaBuffer);
                        zis.closeEntry();

                        //第二次解压
                        unpackOldMediaZip(outFile, mediaDir, mediaBuffer);
                    } else if (name.startsWith(mediaDirName)
                            && !name.equals(mediaDirName)
                            && includeMedia
                    ) {  //处理新版媒体文件
                        File outFile = new File(mediaDir, name.substring(mediaDirName.length()));   //去掉开头的文件夹路径
                        extractFile(zis, outFile, mediaBuffer);
                        zis.closeEntry();
                    } else if (allowedFiles.contains(name)) {   //处理根目录下的 JSON 文件
                        File outFile = new File(tempJsonDir, name);
                        extractFile(zis, outFile, jsonBuffer);
                        extractedJsonFiles.add(outFile);
                        zis.closeEntry();
                    } else {    //非媒体文件且非允许的根目录 JSON 文件直接不解压
                        zis.closeEntry();
                    }
                }
            }
            return extractedJsonFiles;
        });
    }

    /**
     * 将文件真正地从压缩包中解压出来
     *
     * @param zis     {@link ZipInputStream}实例
     * @param outFile 输出文件
     * @param buffer  读写缓冲区
     * @throws IOException 文件解压失败引发的异常
     */
    private static void extractFile(ZipInputStream zis, @NonNull File outFile, byte[] buffer) throws IOException {
        // 确保父目录存在
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.e(LogTags.ZIP_HELPER.n(), "父目录不存在且无法创建");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            // 刷新缓冲区并同步到磁盘
            fos.getFD().sync();
        }
    }

    /**
     * 二次解压旧版媒体文件压缩包
     *
     * @param zipFile  已经从主备份文件解压出来的包含媒体文件的压缩包文件
     * @param mediaDir 永久存放媒体文件的目录
     * @param buffer   缓冲区
     * @throws IOException 解压失败引发的异常
     */
    private static void unpackOldMediaZip(File zipFile, File mediaDir, byte[] buffer) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                File outFile = new File(mediaDir, fileName);
                extractFile(zis, outFile, buffer);
                zis.closeEntry();
            }

            if (!zipFile.delete()) {
                Log.w(LogTags.ZIP_HELPER.n(), "解压旧媒体文件后压缩包删除失败");
            }
        }
    }
}
