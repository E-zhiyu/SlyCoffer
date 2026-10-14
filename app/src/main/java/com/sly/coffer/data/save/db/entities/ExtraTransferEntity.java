package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "extraTransfer"
)
public class ExtraTransferEntity {
    @PrimaryKey(autoGenerate = true)
    private long transferId;        //自增主键
    private String exportAccount;   //转出账户
    private String importAccount;   //转入账户

    public ExtraTransferEntity(String exportAccount, String importAccount) {
        this.exportAccount = exportAccount;
        this.importAccount = importAccount;
    }

    public long getTransferId() {
        return transferId;
    }

    public void setTransferId(long transferId) {
        this.transferId = transferId;
    }

    public String getExportAccount() {
        return exportAccount;
    }

    public void setExportAccount(String exportAccount) {
        this.exportAccount = exportAccount;
    }

    public String getImportAccount() {
        return importAccount;
    }

    public void setImportAccount(String importAccount) {
        this.importAccount = importAccount;
    }
}
