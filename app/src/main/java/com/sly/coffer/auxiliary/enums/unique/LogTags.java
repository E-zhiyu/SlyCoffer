package com.sly.coffer.auxiliary.enums.unique;

public enum LogTags {
    RESTORE_WORKER("RestoreWorker"),
    BACKUP_WORKER("BackupWorker"),
    MEDIA_LIST_ACTIVITY("MediaListActivity"),
    MAIN_ACTIVITY("MainActivity"),
    REPORT_ACTIVITY("ReportActivity"),
    REPORT_VIEW_MODEL("ReportViewModel"),
    AUTH_ACTIVITY("AuthActivity"),
    FULL_SCREEN_MEDIA_ACTIVITY("FullScreenMediaActivity"),
    FILE_HELPER("FileHelper"),
    ACCOUNT_INPUT("RunningAccountInputActivity"),
    AB_NOTIFICATION_LISTENER_SERVICE("AbNotificationListenerService"),
    PICK_ACCESSIBILITY_SERVICE("PickAccessibilityService"),
    AB_ACCESSIBILITY_SERVICE("AbAccessibilityService"),
    DATA_IO_HELPER("DataIOHelper"),
    WORK_STATS("WorkStats"),
    SAF_HELPER("SAFHelper"),
    SCROLL_HELPER("ScrollHelper"),
    ZIP_HELPER("ZipHelper"),
    ALARM_HELPER("AlarmHelper"),
    APPLICATION("Application"),
    BUDGET_RESET_RECEIVER("BudgetResetReceiver"),
    BOOT_RECEIVER("BootReceiver"),
    PERMISSION_HELPER("PermissionHelper"),
    BIOMETRIC_HELPER("BiometricHelper");
    private final String v;

    LogTags(String v) {
        this.v = v;
    }

    public String n() {
        return v;
    }
}
