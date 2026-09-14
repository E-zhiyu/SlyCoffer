package com.sly.coffer.auxiliary.enums.types;

import android.content.Context;

import androidx.annotation.Nullable;

import com.sly.coffer.data.backup.helpers.AccessibilityRuleBackupHelper;
import com.sly.coffer.data.backup.helpers.BackupHelperBase;
import com.sly.coffer.data.backup.helpers.BudgetBackupHelper;
import com.sly.coffer.data.backup.helpers.NotificationRuleBackupHelper;
import com.sly.coffer.data.backup.helpers.RunningAccountBackupHelper;

import java.util.Objects;
import java.util.function.Function;

/**
 * 备份的数据类型
 */
public enum BackupDataType {
    RUNNING_ACCOUNT(
            "流水记录数据",
            "running_account.json",
            "RunningAccount.json",
            RunningAccountBackupHelper::new
    ),
    NOTIFICATION_RULE(
            "通知规则数据",
            "notification_rule.json",
            "AnalysisRule.json",
            NotificationRuleBackupHelper::new
    ),
    ACCESSIBILITY_RULE(
            "无障碍规则数据",
            "accessibility_rule.json",
            null,
            AccessibilityRuleBackupHelper::new
    ),
    BUDGET(
            "预算数据",
            "budget.json",
            "Budget.json",
            BudgetBackupHelper::new
    );
    private final String title;
    private final String fileName;

    private final String oldFileName;
    private final Function<Context, BackupHelperBase<?, ?>> helperFactory;

    BackupDataType(String title, String fileName, String oldFileName, Function<Context, BackupHelperBase<?, ?>> helperFactory) {
        this.title = title;
        this.fileName = fileName;
        this.oldFileName = oldFileName;
        this.helperFactory = helperFactory;
    }


    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOldFileName() {
        return oldFileName;
    }

    /**
     * 创建备份帮助器
     *
     * @param context 上下文
     * @return 备份帮助器实例
     */
    public BackupHelperBase<?, ?> createBackupHelper(Context context) {
        return helperFactory.apply(context);
    }

    /**
     * 根据文件名称判断数据种类
     *
     * @param fileName 文件名称
     * @return 数据类型，若无法匹配类型则返回 null
     */
    @Nullable
    public static BackupDataType fromFileName(String fileName) {
        for (BackupDataType type : values()) {
            if (type.fileName.equals(fileName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 通过旧版文件名判断数据种类
     *
     * @param oldFileName 旧版文件名
     * @return 数据类型，若无法匹配类型则返回 null
     */
    @Nullable
    public static BackupDataType fromOldFileName(@Nullable String oldFileName) {
        if (oldFileName == null) return null;

        for (BackupDataType type : values()) {
            if (Objects.equals(oldFileName, type.oldFileName)) {
                return type;
            }
        }
        return null;
    }
}
