package com.sly.coffer.auxiliary.enums.types;

public enum DateRangeType {
    THAT_DAY("一天"),
    MONTH("一个月"),
    YEAR("一年"),
    CUSTOM("自定义");
    private final String title;

    DateRangeType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
