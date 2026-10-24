package com.sly.coffer.data.backup.maps.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomAccountPojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomAccountTransferPojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomMediaPojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomTagGroupPojo;
import com.sly.coffer.data.backup.pojo.old.room_v62.RoomTagPojo;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomRunningAccountDataMap {
    private List<RoomAccountPojo> basic_data;
    private List<RoomMediaPojo> picture_data;
    private List<RoomTagPojo> tag_data;
    private List<RoomTagGroupPojo> tag_group_data;
    private List<RoomAccountTransferPojo> transfer_data;

    public RoomRunningAccountDataMap() {
    }

    public List<RoomAccountPojo> getBasic_data() {
        return basic_data;
    }

    public void setBasic_data(List<RoomAccountPojo> basic_data) {
        this.basic_data = basic_data;
    }

    public List<RoomMediaPojo> getPicture_data() {
        return picture_data;
    }

    public void setPicture_data(List<RoomMediaPojo> picture_data) {
        this.picture_data = picture_data;
    }

    public List<RoomTagPojo> getTag_data() {
        return tag_data;
    }

    public void setTag_data(List<RoomTagPojo> tag_data) {
        this.tag_data = tag_data;
    }

    public List<RoomTagGroupPojo> getTag_group_data() {
        return tag_group_data;
    }

    public void setTag_group_data(List<RoomTagGroupPojo> tag_group_data) {
        this.tag_group_data = tag_group_data;
    }

    public List<RoomAccountTransferPojo> getTransfer_data() {
        return transfer_data;
    }

    public void setTransfer_data(List<RoomAccountTransferPojo> transfer_data) {
        this.transfer_data = transfer_data;
    }
}
