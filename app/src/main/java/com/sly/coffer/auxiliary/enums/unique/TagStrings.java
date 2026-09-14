package com.sly.coffer.auxiliary.enums.unique;

public enum TagStrings {
    BACKUP_WORKER("backup_worker"),             //自动备份的 Worker
    MEDIA_SELECTION("media_selection"),         //媒体多选追踪器
    ACCOUNT_SELECTION("account_selection"),     //流水记录多选追踪器
    TIME_PICKER("time_picker"),                 //时间选择弹窗
    DATE_PICKER("date_picker"),                 //日期选择弹窗
    ACCOUNT_FILTER_BOTTOM("account_filter_fragment"),   //流水记录过滤对话框
    TAG_SELECT_BOTTOM("tag_select_bottom"),     //标签选择弹窗
    PICKED_VIEW_BOTTOM("picked_view_bottom"),   //拾取的视图选择对话框
    MEDIA_ADD_BOTTOM("media_add_bottom");       //图片添加选项弹窗

    private final String value;

    TagStrings(String value) {
        this.value = value;
    }

    public String t() {
        return value;
    }
}
