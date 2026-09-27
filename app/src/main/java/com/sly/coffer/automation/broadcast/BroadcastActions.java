package com.sly.coffer.automation.broadcast;

/**
 * 广播标识
 */
public enum BroadcastActions {
    ACTION_INPUT_REMARK,            //自动记账通知中输入备注
    ACTION_KEEP,                    //保留自动记账产生的流水记录
    ACTION_DELETE,                  //删除自动记账产生的流水记录
    ACTION_NOTIFICATION_CANCELED,   //自动记账确认通知被删除
    ACTION_NOTIFICATION_CLICKED,    //自动记账确认通知被点击
    ACTION_SHUT_DOWN_ACCESSIBILITY_BOOKKEEPING, //关闭无障碍自动记账
    ACTION_SHUT_DOWN_PAGE_PICK,     //关闭界面拾取服务
}
