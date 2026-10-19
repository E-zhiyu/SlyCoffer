package com.sly.coffer.auxiliary.enums.types;

public enum ImportDataVersionType {
    BEFORE_ROOM("v1.9.0之前"),
    BEFORE_EXTRA("v1.9.0 ~ v1.12.8"),
    LATEST("v1.13.0之后");
    private final String title;

    ImportDataVersionType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
