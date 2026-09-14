package com.sly.coffer.auxiliary.enums.unique;

public enum KeyStrings {
    AUTO_BOOKKEEPING_TYPE("auto_bookkeeping_type"),         //自动记账的种类
    RESTORE_IS_OLD_DATA("restore_is_old_data"),             //导入数据时是否为旧数据
    BACKUP_CHOICES("backup_choices"),                       //备份时的选择情况
    BACKUP_TARGET("backup_target"),                         //备份时的目标文件(夹)
    RUNNING_EXPORT_ACCOUNT("running_export_account"),       //流水转出账户
    RUNNING_IMPORT_ACCOUNT("running_import_account"),       //流水转入账户
    RUNNING_TYPE("running_type"),                           //流水种类
    RUNNING_REMARK("running_remark"),                       //流水备注
    RUNNING_DATETIME("running_date_time"),                  //流水日期和时间
    RUNNING_AMOUNT("running_amount"),                       //流水金额
    RUNNING_ID("running_id"),                               //流水编号
    VIEW_HOLDER_POSITION("view_position"),                  //视图在列表中的索引值
    TAG_ID("tag_id"),                                       //标签编号
    TAG_MULTI_CHOICE("tag_multi_choice"),                   //标签是否为多选模式
    TAG_SCOPE("tag_scope"),                                 //标签作用域
    NOTIFICATION_RULE_NAME("notification_rule_name"),       //通知解析规则名称
    NOTIFICATION_RULE_ID("notification_rule_id"),           //通知解析规则编号
    ACCESSIBILITY_RULE_ID("accessibility_rule_id"),         //无障碍规则编号
    CAPTURED_NOTIFICATION_ID("captured_notification_id"),   //捕获通知的编号
    PACKAGE_NAME("package_name"),                           //包名
    FILE_URIS("file_uris"),                                 //文件Uri
    BUDGET_ID("budget_id"),                                 //预算编号
    PICKED_PAGE_ID("picked_page_id"),                       //拾取的视图的 ID
    NOTIFICATION_ID("notification_id");                     //应用通知ID，用于区分不同的通知

    final String value;

    KeyStrings(String value) {
        this.value = value;
    }

    public String v() {
        return value;
    }
}
