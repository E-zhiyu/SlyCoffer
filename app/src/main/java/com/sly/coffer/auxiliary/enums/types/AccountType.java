package com.sly.coffer.auxiliary.enums.types;

public enum AccountType {
    EXPENSE("支出", -1, "EXPENSE"),
    INCOME("收入", 1, "INCOME"),
    TRANSFER("转账", 0, "TRANSFER");
    private final String title; //名称
    private final int flag;     //是否为收入/支出种类的标识符
    private final String oldValue;  //旧枚举值

    AccountType(String title, int flag, String oldValue) {
        this.title = title;
        this.flag = flag;
        this.oldValue = oldValue;
    }

    public String getTitle() {
        return title;
    }

    public boolean isExpenseType() {
        return flag == -1;
    }

    public boolean isIncomeType() {
        return flag == 1;
    }

    /**
     * 根据旧枚举值获取流水种类
     *
     * @param oldValue 旧枚举值
     * @return 流水种类
     */
    public static AccountType fromOldValue(String oldValue) {
        for (AccountType type : values()) {
            if (type.oldValue.equals(oldValue)) {
                return type;
            }
        }
        return EXPENSE;
    }
}
