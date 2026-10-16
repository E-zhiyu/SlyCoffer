package com.sly.coffer.data.save.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "accessibilityRules",
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
                        parentColumns = "repayAccountId",
                        childColumns = "accountId",
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
public class AccessibilityRuleEntity {
    @PrimaryKey(autoGenerate = true)
    private long ruleId;            //主键
    private String name;            //规则名称
    private int type;               //流水种类
    @ColumnInfo(defaultValue = "true")
    private boolean enabled;        //是否启用
    private String packageName;     //应用包名
    private String activityName;    //目标 Activity 的名称
    @ColumnInfo(defaultValue = "-1")
    private long transferId = -1;   //转账账户额外数据编号
    @ColumnInfo(defaultValue = "-1")
    private long loanId = -1;       //借款额外数据编号
    @ColumnInfo(defaultValue = "-1")
    private long repayAccountId = -1;   //作为还款记录时，把钱还给的那个借款记录的编号

    public AccessibilityRuleEntity(String name, int type, String packageName, String activityName) {
        this.name = name;
        this.type = type;
        this.enabled = true;
        this.packageName = packageName;
        this.activityName = activityName;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public long getTransferId() {
        return transferId;
    }

    public void setTransferId(long transferId) {
        this.transferId = transferId;
    }

    public long getLoanId() {
        return loanId;
    }

    public void setLoanId(long loanId) {
        this.loanId = loanId;
    }

    public long getRepayAccountId() {
        return repayAccountId;
    }

    public void setRepayAccountId(long repayAccountId) {
        this.repayAccountId = repayAccountId;
    }
}
