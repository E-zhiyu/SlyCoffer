package com.sly.coffer.auxiliary.classes;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.util.List;

public abstract class ImportResult {
    protected String title;

    //全部导入成功
    public static class Success extends ImportResult {
        @Override
        public String getTitle() {
            return "导入成功";
        }
    }

    //数据全部缺失
    public static class Missed extends ImportResult {
        @Override
        public String getTitle() {
            return "已缺失";
        }
    }

    //部分成功
    public static class PartlySuccess extends ImportResult {
        public List<Throwable> exceptionList;

        public PartlySuccess(List<Throwable> exceptionList) {
            this.exceptionList = exceptionList;
        }

        @Override
        public String getTitle() {
            return "不完全导入成功";
        }
    }

    //失败
    public static class Failure extends ImportResult {
        public Throwable exception;

        public Failure(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public String getTitle() {
            return "导入失败";
        }
    }

    public abstract String getTitle();

    @NonNull
    @Contract(" -> new")
    public static Success success() {
        return new Success();
    }

    @NonNull
    @Contract(" -> new")
    public static Missed missed() {
        return new Missed();
    }

    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static PartlySuccess partlySuccess(List<Throwable> exceptionList) {
        return new PartlySuccess(exceptionList);
    }

    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Failure failure(Throwable e) {
        return new Failure(e);
    }
}
