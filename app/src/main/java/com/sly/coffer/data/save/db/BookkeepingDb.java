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
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Database(
        entities = {
                BudgetEntity.class,
                BudgetTagRefEntity.class,
                NotificationRuleEntity.class,
                NotificationRuleTransferEntity.class,
                NotificationRuleTagRefEntity.class,
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
        version = 5
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
                                    DatabaseMigrations.MIGRATION_4_5
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

            //默认通知规则
            NotificationRuleEntity weChatPay = new NotificationRuleEntity(  //微信支付
                    "微信支付",
                    AccountType.EXPENSE.ordinal(),
                    "com.tencent.mm",
                    "微信支付",
                    "已支付.(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)",
                    1
            );
            notificationRuleDao().insertNotificationRule(weChatPay);
            NotificationRuleEntity aliPay = new NotificationRuleEntity(     //支付宝支付
                    "支付宝支付",
                    AccountType.EXPENSE.ordinal(),
                    "com.eg.android.AlipayGphone",
                    "交易提醒",
                    "你有一笔(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)元的支出",
                    1
            );
            notificationRuleDao().insertNotificationRule(aliPay);

            return Completable.complete();
        });
    }
}
