package com.sly.coffer.helpers.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.enums.DirectoryPaths;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.data.backup.maps.DataInfoMap;

import java.io.ByteArrayOutputStream;
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
                // 将 ZipOutputStream 包装在外部传入的流上
                try (ZipOutputStream zos = new ZipOutputStream(os)) {

                    // 1. 打包临时 JSON 目录下的所有文件（不包含目录本身）
                    File[] tempFiles = tempDir.listFiles();
                    if (tempFiles != null) {
                        for (File file : tempFiles) {
                            addToZip(file, file.getName(), zos);
                        }
                    }

                    // 2. 将图片目录整体添加进去
                    if (includeMedia && mediaDir != null && mediaDir.exists() && mediaDir.isDirectory()) {
                        addToZip(mediaDir, mediaDir.getName(), zos);
                    }
                }
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
     * 从备份文件中获取备份数据信息
     *
     * @param context 上下文
     * @param zipUri  指向备份文件的 Uri
     * @return 备份数据信息
     */
    public static Single<DataInfoMap> getDataInfoFromBackupFile(Context context, Uri zipUri) {
        return Single.fromCallable(() -> {
            byte[] jsonBuffer = new byte[1024 * 8];
            try (InputStream is = context.getContentResolver().openInputStream(zipUri);
                 ZipInputStream zis = new ZipInputStream(is)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    if (name.equals(BackupHelperBase.DATA_INFO_JSON) && !entry.isDirectory()) {
                        //读取文件内容
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int len;
                        // 读到当前条目末尾时 read 返回 -1
                        while ((len = zis.read(jsonBuffer)) != -1) {
                            buffer.write(jsonBuffer, 0, len);
                        }
                        String content = buffer.toString();

                        //反序列化
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(content, DataInfoMap.class);
                    } else {
                        zis.closeEntry();
                    }
                }
            }

            return null;
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
            Set<String> allowedFiles,
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

//                    if (name.equals("pictures.zip")) {          //处理旧版媒体文件
//                        File outFile = new File(tempJsonDir, name);
//                        extractFile(zis, outFile, mediaBuffer);
//                        zis.closeEntry();
//
//                        //第二次解压
//                        unpackOldMediaZip(outFile, mediaDir, mediaBuffer);
//                    } else
                    if (name.startsWith(mediaDirName) && !name.equals(mediaDirName) && includeMedia) {  //处理新版媒体文件
                        File outFile = new File(mediaDir, name.substring(mediaDirName.length()));   //去掉开头的文件夹路径
                        extractFile(zis, outFile, mediaBuffer);
                    } else if (allowedFiles.contains(name)) {   //处理根目录下的 JSON 文件
                        File outFile = new File(tempJsonDir, name);
                        extractFile(zis, outFile, jsonBuffer);
                        extractedJsonFiles.add(outFile);
                    }
                    zis.closeEntry();
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
}
