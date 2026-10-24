package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RunningAccountDataMap {
    private List<AccountPojo> accountList;
    private List<AccountTagRefPojo> accountTagRefList;
    private List<ExtraTransferPojo> extraTransferList;
    private List<ExtraLoanPojo> extraLoanList;
    private List<MediaPojo> mediaList;
    private List<TagGroupPojo> tagGroupList;
    private List<TagPojo> tagList;

    public RunningAccountDataMap() {
    }

    public List<AccountPojo> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<AccountPojo> accountList) {
        this.accountList = accountList;
    }

    public List<AccountTagRefPojo> getAccountTagRefList() {
        return accountTagRefList;
    }

    public void setAccountTagRefList(List<AccountTagRefPojo> accountTagRefList) {
        this.accountTagRefList = accountTagRefList;
    }

    public List<ExtraTransferPojo> getExtraTransferList() {
        return extraTransferList;
    }

    public void setExtraTransferList(List<ExtraTransferPojo> extraTransferList) {
        this.extraTransferList = extraTransferList;
    }

    public List<MediaPojo> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<MediaPojo> mediaList) {
        this.mediaList = mediaList;
    }

    public List<TagGroupPojo> getTagGroupList() {
        return tagGroupList;
    }

    public void setTagGroupList(List<TagGroupPojo> tagGroupList) {
        this.tagGroupList = tagGroupList;
    }

    public List<TagPojo> getTagList() {
        return tagList;
    }

    public void setTagList(List<TagPojo> tagList) {
        this.tagList = tagList;
    }

    public List<ExtraLoanPojo> getExtraLoanList() {
        return extraLoanList;
    }

    public void setExtraLoanList(List<ExtraLoanPojo> extraLoanList) {
        this.extraLoanList = extraLoanList;
    }
}
