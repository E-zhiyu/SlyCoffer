package com.sly.coffer.auxiliary.enums.unique;

public enum TransitionName {
    FULLSCREEN_MEDIA("fullscreen_media");
    private final String s;

    TransitionName(String s) {
        this.s = s;
    }

    public String getS() {
        return s;
    }
}
