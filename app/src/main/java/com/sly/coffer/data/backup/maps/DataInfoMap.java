package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataInfoMap {
    private long appVersionCode;    //备份时的应用版本号
    private List<String> fileNames; //备份数据的文件名，null 表示那个位置的数据类型没有导出

    public DataInfoMap(List<String> fileNames, long appVersionCode) {
        this.fileNames = fileNames;
        this.appVersionCode = appVersionCode;
    }

    public DataInfoMap() {
    }

    public long getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(long appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
