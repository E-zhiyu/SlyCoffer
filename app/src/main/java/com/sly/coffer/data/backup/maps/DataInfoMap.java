package com.sly.coffer.data.backup.maps;

import java.util.List;

public class DataInfoMap {
    private int appVersionCode;     //备份时的应用版本号
    private List<Boolean> choices;  //备份时的选择情况
    private List<String> fileNames; //备份时真正导出的数据的 JSON 文件名

    public DataInfoMap() {
    }

    public int getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(int appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public List<Boolean> getChoices() {
        return choices;
    }

    public void setChoices(List<Boolean> choices) {
        this.choices = choices;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
