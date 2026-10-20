package com.sly.coffer.auxiliary.enums.types;

public enum ImportResultType {
    MISSING("缺失"),
    SUCCEED("导入成功"),
    PARTIALLY_SUCCEED("部分导入成功"),
    JSON_ERROR("无法解析");

    private final String title;

    ImportResultType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
