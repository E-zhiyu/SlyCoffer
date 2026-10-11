package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "accountTransfers",
        foreignKeys = @ForeignKey(
                entity = AccountEntity.class,
                parentColumns = "accountId",
                childColumns = "accountId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "accountId")
        }
)
public class AccountTransferEntity {
    @PrimaryKey
    private long accountId;         //流水记录 ID
    private String exportAccount;   //转出账户
    private String importAccount;   //转入账户

    public AccountTransferEntity(long accountId,String exportAccount, String importAccount) {
        this.accountId = accountId;
        this.exportAccount = exportAccount;
        this.importAccount = importAccount;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
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
