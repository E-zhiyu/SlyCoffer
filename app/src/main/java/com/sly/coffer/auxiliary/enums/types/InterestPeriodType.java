package com.sly.coffer.auxiliary.enums.types;

public enum InterestPeriodType {
    EVERY_DAY("每天/随时还款"),
    EVERY_MONTH("每个月"),
    EVERY_QUARTER("每季度"),
    EVERY_YEAR("每年");
    private final String title;

    InterestPeriodType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
