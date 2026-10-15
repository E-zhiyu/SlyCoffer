package com.sly.coffer.data.backup.pojo.old.room_v62;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中多余字段
public class RoomAccountTransferPojo {
    private String export_account;
    private String import_account;
    private long rno;

    public RoomAccountTransferPojo() {
    }

    public String getExport_account() {
        return export_account;
    }

    public void setExport_account(String export_account) {
        this.export_account = export_account;
    }

    public String getImport_account() {
        return import_account;
    }

    public void setImport_account(String import_account) {
        this.import_account = import_account;
    }

    public long getRno() {
        return rno;
    }

    public void setRno(long rno) {
        this.rno = rno;
    }
}
