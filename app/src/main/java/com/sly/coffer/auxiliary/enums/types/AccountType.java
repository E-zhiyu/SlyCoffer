package com.sly.coffer.auxiliary.enums.types;

import org.jetbrains.annotations.Contract;

public enum AccountType {
    EXPENSE("支出", -1, 4),
    INCOME("收入", 1, 4),
    TRANSFER("转账", 0, 1),
    LEND("借出", -1, 2),
    BORROW("借入", 1, 2);
    public static final int DATA_FLAG_TRANSFER = 1;
    public static final int DATA_FLAG_LOAN_INFO = 2;
    public static final int DATA_FLAG_REPAY = 4;
    private final String title;     //名称
    private final int reportFlag;   //钱增加/减少的标识符
    private final int dataFlag;     //额外数据输入标识符（二进制位为1表示需要输入额外数据）

    AccountType(String title, int flag, int dataFlag) {
        this.title = title;
        this.reportFlag = flag;
        this.dataFlag = dataFlag;
    }

    public String getTitle() {
        return title;
    }

    public boolean isExpenseType() {
        return reportFlag == -1;
    }

    public boolean isIncomeType() {
        return reportFlag == 1;
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

    /**
     * 判断是否需要输入指定的额外数据
     *
     * @param edittextFlag 额外数据标识符，详见{@link AccountType}中的静态常量
     * @return 是否需要输入指定的额外数据
     */
    public boolean shouldShowEdittext(int edittextFlag) {
        return (this.dataFlag & edittextFlag) == edittextFlag;
    }
}
