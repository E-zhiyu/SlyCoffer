package com.sly.coffer.helpers.file;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sly.coffer.SlyCoffer;
import com.sly.coffer.auxiliary.enums.unique.LogTags;

public class SAFHelper {
    /**
     * 通过SAF打开文件
     *
     * @param fileTypes 文件种类白名单，只能选择指定类型的文件
     * @param launcher  启动SAF的意图启动器
     */
    public static void openDocumentViaSAF(
            String[] fileTypes,
            @NonNull ActivityResultLauncher<Intent> launcher
    ) {
        SlyCoffer.lockLifecycleObserver();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, fileTypes);
        launcher.launch(intent);
        Log.d(LogTags.SAF_HELPER.n(), "SAF启动成功");
    }

    /**
     * 通过SAF创建文件
     *
     * @param intentType {@link Intent#setType(String)}中传递的参数
     * @param fileName   新建文件的文件名
     * @param launcher   启动SAF的启动器
     */
    public static void createDocumentViaSAF(
            String intentType,
            String fileName,
            @NonNull ActivityResultLauncher<Intent> launcher
    ) {
        SlyCoffer.lockLifecycleObserver();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(intentType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        launcher.launch(intent);
    }

    /**
     * 通过 SAF 选择文件目录
     *
     * @param initialUri 初始加载的 Uri
     * @param launcher   意图启动器
     */
    public static void openDocumentTreeViaSAF(
            @Nullable String initialUri,
            ActivityResultLauncher<Intent> launcher
    ) {
        SlyCoffer.lockLifecycleObserver();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        launcher.launch(intent);
    }

    /**
     * 从 SAF 返回的 Tree Uri 中提取出可读的路径
     *
     * @param context 上下文
     * @param treeUri 用户选择目录后返回的 Uri
     * @return 可读的路径字符串
     */
    @NonNull
    public static String getReadablePathFromSafUri(Context context, @NonNull Uri treeUri) {
        String pathInfo;
        // 获取 DocumentId (例如: "primary:Download/MyFolder" 或 "1A2B-3C4D:Music")
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        if (documentId == null) {
            return getDisplayName(context, treeUri);
        }

        // 拆分存储盘符和相对路径
        String[] parts = documentId.split(":");
        if (parts.length > 0) {
            String storageId = parts[0]; // "primary" 代表内部主存储，其他字符串通常代表外置 SD 卡
            String relativePath = parts.length > 1 ? parts[1] : "";

            if ("primary".equalsIgnoreCase(storageId)) {
                // 如果是内部主存储，可以拼接出绝对路径
                pathInfo = "/storage/emulated/0/" + relativePath;
            } else {
                return getDisplayName(context, treeUri);
            }
        } else {
            pathInfo = documentId;
        }

        // 去掉路径末尾多余的斜杠
        if (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
        }

        return pathInfo;
    }

    /**
     * 通过 ContentResolver 查询 Uri 的系统显示名称 (Display Name)
     */
    @NonNull
    private static String getDisplayName(@NonNull Context context, @NonNull Uri uri) {
        // 构建用于查询的 Document Uri（如果是 Tree Uri，需要先转换成 Document Uri 才能查询属性）
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
        );

        // 查询系统数据库中的 DISPLAY_NAME 字段
        String[] projection = {OpenableColumns.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) {
                    String displayName = cursor.getString(index);
                    if (displayName != null && !displayName.isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LogTags.SAF_HELPER.n(), "无法获取 Uri 的显示名称");
        }

        // 终极保底：如果实在查不到，返回 Uri 的最后一部分或原始 Uri
        return uri.getLastPathSegment() != null ? uri.getLastPathSegment() : uri.toString();
    }
}
