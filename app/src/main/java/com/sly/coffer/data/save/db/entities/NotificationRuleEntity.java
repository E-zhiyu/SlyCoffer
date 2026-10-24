package com.sly.coffer.data.save.db.entities;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notificationRules",
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
                @Index(value = "enabled"),
                @Index(value = "transferId"),
                @Index(value = "loanId"),
                @Index(value = "repayAccountId")
        }
)
public class NotificationRuleEntity {
    @PrimaryKey(autoGenerate = true)
    private long ruleId;            //主键
    private String name;            //名称
    private int type;               //生成的流水种类
    private String packageName;     //包名
    private String targetTitle;     //通知标题
    private String contentRegex;    //通知内容正则
    @ColumnInfo(defaultValue = "1")
    private int captureGroupPos;    //金额捕获组位置
    @ColumnInfo(defaultValue = "true")
    private boolean enabled;        //是否启用
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long transferId;        //转账账户额外数据编号
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long loanId;            //借款额外数据编号
    @Nullable
    @ColumnInfo(defaultValue = "NULL")
    private Long repayAccountId;    //作为还款记录时，把钱还给的那个借款记录的编号

    public NotificationRuleEntity(String name, int type, String packageName, String targetTitle, String contentRegex, int captureGroupPos) {
        this.name = name;
        this.type = type;
        this.packageName = packageName;
        this.targetTitle = targetTitle;
        this.contentRegex = contentRegex;
        this.captureGroupPos = captureGroupPos;
        this.enabled = true;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getContentRegex() {
        return contentRegex;
    }

    public void setContentRegex(String contentRegex) {
        this.contentRegex = contentRegex;
    }

    public int getCaptureGroupPos() {
        return captureGroupPos;
    }

    public void setCaptureGroupPos(int captureGroupPos) {
        this.captureGroupPos = captureGroupPos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
