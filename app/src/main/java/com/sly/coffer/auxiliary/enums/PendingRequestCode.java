package com.sly.coffer.auxiliary.enums;

public enum PendingRequestCode {
    BUDGET_LOW_RESET,                       //低预算通知的预算重置 Action
    BUDGET_RESET_ALARM,                     //设置预算重置闹钟
    BUDGET_LOW_BALANCE,                     //预算余额低提醒通知
    ACCOUNT_KEEP,                           //自动记账通知中保留按钮的请求代码
    ACCOUNT_DELETE,                         //自动记账通知中删除按钮的请求代码
    ACCOUNT_INPUT_REMARK,                   //自动记账通知中输入备注的请求代码
    SKIP_TO_ACCOUNT_INPUT,                  //自动记账保留记录后，点击通知跳转至流水输入界面
    AUTO_BOOKKEEPING_NOTIFICATION_CLICK,    //点击自动记账确认通知
    AUTO_BOOKKEEPING_NOTIFICATION_DELETE,   //删除自动记账确认通知
}
