package com.sly.coffer.auxiliary.enums.unique;

public enum ViewTags {
    RECYCLER_SCROLL_LISTENER(0x7f000001);       // RecyclerView 的滚动监听器
    private final int t;

    ViewTags(int t) {
        this.t = t;
    }

    public int getT() {
        return t;
    }
}
