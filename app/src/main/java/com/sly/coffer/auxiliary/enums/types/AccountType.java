package com.sly.coffer.auxiliary.enums.types;

import org.jetbrains.annotations.Contract;

public enum AccountType {
    EXPENSE("支出", -1),
    INCOME("收入", 1),
    TRANSFER("转账", 0),
    LEND("借出", -1),
    BORROW("借入", 1);
    private final String title;     //名称
    private final int flag;         //是否为收入/支出种类的标识符

    AccountType(String title, int flag) {
        this.title = title;
        this.flag = flag;
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
    @Contract(pure = true)
    public static AccountType fromOldValue(String oldValue) {
        if (oldValue == null) return EXPENSE;
        switch (oldValue) {
            case "INCOME":
                return INCOME;
            case "TRANSFER":
                return TRANSFER;
            case "EXPENSE":
            default:
                return EXPENSE;
        }
    }
}
