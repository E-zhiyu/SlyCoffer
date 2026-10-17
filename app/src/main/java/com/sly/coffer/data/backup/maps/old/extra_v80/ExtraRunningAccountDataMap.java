package com.sly.coffer.data.backup.maps.old.extra_v80;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccountPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraAccountTransferPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraMediaPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraTagGroupPojo;
import com.sly.coffer.data.backup.pojo.old.extra_v80.ExtraTagPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class ExtraRunningAccountDataMap {
    private List<ExtraAccountPojo> accountList;
    private List<ExtraAccountTagRefPojo> accountTagRefList;
    private List<ExtraAccountTransferPojo> accountTransferList;
    private List<ExtraMediaPojo> mediaList;
    private List<ExtraTagGroupPojo> tagGroupList;
    private List<ExtraTagPojo> tagList;

    public ExtraRunningAccountDataMap() {
    }

    public List<ExtraAccountPojo> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<ExtraAccountPojo> accountList) {
        this.accountList = accountList;
    }

    public List<ExtraAccountTagRefPojo> getAccountTagRefList() {
        return accountTagRefList;
    }

    public void setAccountTagRefList(List<ExtraAccountTagRefPojo> accountTagRefList) {
        this.accountTagRefList = accountTagRefList;
    }

    public List<ExtraAccountTransferPojo> getAccountTransferList() {
        return accountTransferList;
    }

    public void setAccountTransferList(List<ExtraAccountTransferPojo> accountTransferList) {
        this.accountTransferList = accountTransferList;
    }

    public List<ExtraMediaPojo> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<ExtraMediaPojo> mediaList) {
        this.mediaList = mediaList;
    }

    public List<ExtraTagGroupPojo> getTagGroupList() {
        return tagGroupList;
    }

    public void setTagGroupList(List<ExtraTagGroupPojo> tagGroupList) {
        this.tagGroupList = tagGroupList;
    }

    public List<ExtraTagPojo> getTagList() {
        return tagList;
    }

    public void setTagList(List<ExtraTagPojo> tagList) {
        this.tagList = tagList;
    }
}
