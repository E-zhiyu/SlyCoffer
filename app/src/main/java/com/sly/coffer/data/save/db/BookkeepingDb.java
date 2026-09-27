package com.sly.coffer.data.save.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.data.backup.DataBackupDao;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.converters.UriConverter;
import com.sly.coffer.data.save.db.daos.AccessibilityRuleDao;
import com.sly.coffer.data.save.db.daos.AccountDao;
import com.sly.coffer.data.save.db.daos.BudgetDao;
import com.sly.coffer.data.save.db.daos.NotificationRuleDao;
import com.sly.coffer.data.save.db.daos.TagDao;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Database(
        entities = {
                BudgetEntity.class,
                BudgetTagRefEntity.class,
                NotificationRuleEntity.class,
                NotificationRuleTransferEntity.class,
                NotificationRuleTagRefEntity.class,
                NotificationRuleGroupEntity.class,
                NotificationRuleGroupRefEntity.class,
                AccountEntity.class,
                AccountTransferEntity.class,
                AccountTagRefEntity.class,
                MediaEntity.class,
                TagEntity.class,
                TagGroupEntity.class,
                CapturedNotificationEntity.class,
                AccessibilityRuleEntity.class,
                AccessibilityRuleTagRefEntity.class,
                AccessibilityRuleTransferEntity.class,
                AccessibilityRuleKeywordGroupEntity.class,
                PickedPageEntity.class
        },
        version = 7
)
@TypeConverters({
        DateTimeConverter.class,
        UriConverter.class
})
public abstract class BookkeepingDb extends RoomDatabase {
    private static volatile BookkeepingDb INSTANCE; //单例实例

    /**
     * 获取数据库实例
     *
     * @param context 上下文
     * @return 数据库实例
     */
    public static BookkeepingDb getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BookkeepingDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BookkeepingDb.class,
                                    "bookkeeping_database"
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);

                                    //初始化默认分组逻辑
                                    getInstance(context).insertDefaultData()
                                            .subscribeOn(Schedulers.io())
                                            .subscribe();
                                }
                            })
                            .addMigrations(
                                    DatabaseMigrations.MIGRATION_1_2,
                                    DatabaseMigrations.MIGRATION_2_3,
                                    DatabaseMigrations.MIGRATION_3_4,
                                    DatabaseMigrations.MIGRATION_4_5,
                                    DatabaseMigrations.MIGRATION_5_6,
                                    DatabaseMigrations.MIGRATION_6_7
                            )
                            .build();
                }
            }
        }

        return INSTANCE;
    }

    public abstract AccountDao accountDao();

    public abstract TagDao tagDao();

    public abstract NotificationRuleDao notificationRuleDao();

    public abstract BudgetDao budgetDao();

    public abstract AccessibilityRuleDao accessibilityRuleDao();

    public abstract DataBackupDao dataBackupDao();

    /**
     * 填充默认数据
     *
     * @return 是否完成
     */
    private Completable insertDefaultData() {
        return Completable.defer(() -> {
            //默认标签分组
            TagGroupEntity defaultGroup = new TagGroupEntity("默认分组");
            defaultGroup.setGroupId(-1);
            tagDao().insertTagGroup(defaultGroup);

            //插入默认规则
            insertDefaultNotificationRule();    //默认通知规则
            insertDefaultAccessibilityRule();   //默认无障碍规则

            return Completable.complete();
        });
    }

    /**
     * 插入默认的通知规则
     */
    private void insertDefaultNotificationRule() {
        //微信支付
        NotificationRuleEntity weChatPay = new NotificationRuleEntity(
                "微信支付",
                AccountType.EXPENSE.ordinal(),
                "com.tencent.mm",
                "微信支付",
                "已支付.(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)",
                1
        );
        notificationRuleDao().insertNotificationRule(weChatPay);

        //支付宝支付
        NotificationRuleEntity aliPayExpense = new NotificationRuleEntity(
                "支付宝支付",
                AccountType.EXPENSE.ordinal(),
                "com.eg.android.AlipayGphone",
                "交易提醒",
                "你有一笔(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)元的支出",
                1
        );
        notificationRuleDao().insertNotificationRule(aliPayExpense);

        //支付宝退款
        NotificationRuleEntity aliPayRefund = new NotificationRuleEntity(
                "支付宝退款",
                AccountType.INCOME.ordinal(),
                "com.eg.android.AlipayGphone",
                "退款提醒",
                "你收到一笔(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)元退款",
                1
        );
        notificationRuleDao().insertNotificationRule(aliPayRefund);

        //微信收款
        NotificationRuleEntity weChatProceeds = new NotificationRuleEntity(
                "微信收款",
                AccountType.INCOME.ordinal(),
                "com.tencent.mm",
                "微信支付",
                "个人收款码到账.(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)",
                1
        );
        notificationRuleDao().insertNotificationRule(weChatProceeds);
    }

    /**
     * 插入默认无障碍规则
     */
    private void insertDefaultAccessibilityRule() {
        //微信转账支出
        AccessibilityRuleEntity weChatTransferExpense = new AccessibilityRuleEntity(
                "微信转账支出",
                AccountType.EXPENSE.ordinal(),
                "com.tencent.mm",
                "com.tencent.mm.framework.app.UIPageFragmentActivity"
        );
        long weChatTransferExpenseId = accessibilityRuleDao().insertAccessibilityRule(weChatTransferExpense);
        List<AccessibilityRuleKeywordGroupEntity> weChatTransferExpenseKgs = List.of(
                new AccessibilityRuleKeywordGroupEntity(
                        weChatTransferExpenseId, "支付成功 确认收款"
                )
        );
        accessibilityRuleDao().insertKeywordGroup(weChatTransferExpenseKgs);

        //微信转账收入
        AccessibilityRuleEntity weChatTransferIncome = new AccessibilityRuleEntity(
                "微信转账收入",
                AccountType.INCOME.ordinal(),
                "com.tencent.mm",
                "com.tencent.mm.plugin.remittance.ui.RemittanceDetailUI"
        );
        long weChatTransferIncomeId = accessibilityRuleDao().insertAccessibilityRule(weChatTransferIncome);
        List<AccessibilityRuleKeywordGroupEntity> weChatTransferIncomeKgs = List.of(
                new AccessibilityRuleKeywordGroupEntity(
                        weChatTransferIncomeId, "收款 存入"
                )
        );
        accessibilityRuleDao().insertKeywordGroup(weChatTransferIncomeKgs);

        //微信充值
        AccessibilityRuleEntity weChatRecharge = new AccessibilityRuleEntity(
                "微信充值",
                AccountType.TRANSFER.ordinal(),
                "com.tencent.mm",
                "com.tencent.mm.framework.app.UIPageFragmentActivity"
        );
        long weChatRechargeId = accessibilityRuleDao().insertAccessibilityRule(weChatRecharge);
        List<AccessibilityRuleKeywordGroupEntity> weChatRechargeKgs = List.of(
                new AccessibilityRuleKeywordGroupEntity(
                        weChatRechargeId, "充值成功"
                )
        );
        accessibilityRuleDao().insertKeywordGroup(weChatRechargeKgs);

        //微信收红包
        AccessibilityRuleEntity weChatReceiveRedPacket = new AccessibilityRuleEntity(
                "微信收红包",
                AccountType.INCOME.ordinal(),
                "com.tencent.mm",
                "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNewDetailUI"
        );
        long weChatReceiveRedPacketId = accessibilityRuleDao().insertAccessibilityRule(weChatReceiveRedPacket);
        List<AccessibilityRuleKeywordGroupEntity> weChatReceiveRedPacketKgs = List.of(
                new AccessibilityRuleKeywordGroupEntity(
                        weChatReceiveRedPacketId, "红包 存入"
                )
        );
        accessibilityRuleDao().insertKeywordGroup(weChatReceiveRedPacketKgs);

        //微信发红包
        AccessibilityRuleEntity weChatSendRedPacket = new AccessibilityRuleEntity(
                "微信发红包",
                AccountType.EXPENSE.ordinal(),
                "com.tencent.mm",
                "com.tencent.mm.framework.app.UIPageFragmentActivity"
        );
        long weChatSendRedPacketId = accessibilityRuleDao().insertAccessibilityRule(weChatSendRedPacket);
        List<AccessibilityRuleKeywordGroupEntity> weChatSendRedPacketKgs = List.of(
                new AccessibilityRuleKeywordGroupEntity(
                        weChatSendRedPacketId, "微信红包"
                )
        );
        accessibilityRuleDao().insertKeywordGroup(weChatSendRedPacketKgs);
    }
}
