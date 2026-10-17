package com.sly.coffer.data.save.db.entities;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(
        tableName = "accounts",
        foreignKeys = {
                @ForeignKey(
                        entity = ExtraTransferEntity.class,
                        parentColumns = "transferId",
                        childColumns = "transferId",
                        onDelete = ForeignKey.SET_DEFAULT,
                        onUpdate = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ExtraLoanEntity.class,
                        parentColumns = "loanId",
                        childColumns = "loanId",
                        onDelete = ForeignKey.SET_DEFAULT,
                        onUpdate = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = AccountEntity.class,
                        parentColumns = "accountId",
                        childColumns = "repayAccountId",
                        onDelete = ForeignKey.SET_DEFAULT,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "accountId"),
                @Index(value = "remark"),
                @Index(value = "type"),
                @Index(value = "dateTime"),
                @Index(value = "transferId"),
                @Index(value = "loanId"),
                @Index(value = "repayAccountId")
        }
)
public class AccountEntity {
    @PrimaryKey(autoGenerate = true)
    private long accountId;         //主键
    private double amount;          //金额
    private String remark;          //备注
    private int type;               //种类
    private LocalDateTime dateTime; //日期和时间
    @ColumnInfo(defaultValue = "0")
    private int autoTag;            //自动记账标识
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long transferId;        //转账账户额外数据编号
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long loanId;            //借款额外数据编号
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long repayAccountId;    //作为还款记录时，把钱还给的那个借款记录的编号

    public AccountEntity(double amount, String remark, int type, LocalDateTime dateTime, int autoTag) {
        this.amount = amount;
        this.remark = remark;
        this.type = type;
        this.dateTime = dateTime;
        this.autoTag = autoTag;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setAutoTag(int autoTag) {
        this.autoTag = autoTag;
    }

    public int getAutoTag() {
        return autoTag;
    }

    @Nullable
    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(@Nullable Long transferId) {
        this.transferId = transferId;
    }

    @Nullable
    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(@Nullable Long loanId) {
        this.loanId = loanId;
    }

    @Nullable
    public Long getRepayAccountId() {
        return repayAccountId;
    }

    public void setRepayAccountId(@Nullable Long repayAccountId) {
        this.repayAccountId = repayAccountId;
    }
}
