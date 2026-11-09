package com.sly.coffer.data.backup.maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.LocationPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RunningAccountDataMap {
    private List<AccountPojo> accountList;              //流水记录
    private List<AccountTagRefPojo> accountTagRefList;  //标签映射
    private List<ExtraTransferPojo> extraTransferList;  //转账账户信息
    private List<ExtraLoanPojo> extraLoanList;          //借款记录信息
    private List<LocationPojo> locationList;            //段落位置信息
    private List<MediaPojo> mediaList;                  //媒体
    private List<TagGroupPojo> tagGroupList;            //标签分组
    private List<TagPojo> tagList;                      //标签

    public RunningAccountDataMap(List<AccountPojo> accountList, List<AccountTagRefPojo> accountTagRefList, List<ExtraTransferPojo> extraTransferList, List<ExtraLoanPojo> extraLoanList, List<LocationPojo> locationList, List<MediaPojo> mediaList, List<TagGroupPojo> tagGroupList, List<TagPojo> tagList) {
        this.accountList = accountList;
        this.accountTagRefList = accountTagRefList;
        this.extraTransferList = extraTransferList;
        this.extraLoanList = extraLoanList;
        this.locationList = locationList;
        this.mediaList = mediaList;
        this.tagGroupList = tagGroupList;
        this.tagList = tagList;
    }

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

    public List<ExtraLoanPojo> getExtraLoanList() {
        return extraLoanList;
    }

    public void setExtraLoanList(List<ExtraLoanPojo> extraLoanList) {
        this.extraLoanList = extraLoanList;
    }

    public List<LocationPojo> getLocationList() {
        return locationList;
    }

    public void setLocationList(List<LocationPojo> locationList) {
        this.locationList = locationList;
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
}
