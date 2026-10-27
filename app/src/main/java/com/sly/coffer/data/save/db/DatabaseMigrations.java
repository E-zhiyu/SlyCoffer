package com.sly.coffer.data.save.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseMigrations {
    //添加捕获通知相关的数据
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `capturedNotifications` (`notificationId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `content` TEXT, `packageName` TEXT, `appName` TEXT, `time` INTEGER)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_capturedNotifications_title` ON `capturedNotifications` (`title`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_capturedNotifications_content` ON `capturedNotifications` (`content`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_capturedNotifications_appName` ON `capturedNotifications` (`appName`)");
        }
    };

    //删除通知规则中无用的索引
    //通知规则添加是否启用的索引
    //添加无障碍记账规则
    //添加拾取的视图数据
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            //修改旧数据库
            db.execSQL("DROP INDEX IF EXISTS index_notificationRules_name");
            db.execSQL("DROP INDEX IF EXISTS index_notificationRules_packageName");
            db.execSQL("DROP INDEX IF EXISTS index_notificationRules_targetTitle");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRules_enabled` ON `notificationRules` (`enabled`)");

            //新建表格和索引
            db.execSQL("CREATE TABLE IF NOT EXISTS `accessibilityRules` (`ruleId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `type` INTEGER NOT NULL, `enabled` INTEGER NOT NULL DEFAULT true, `packageName` TEXT, `activityName` TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRules_enabled` ON `accessibilityRules` (`enabled`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `accessibilityRuleTagRef` (`ruleId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`ruleId`, `tagId`), FOREIGN KEY(`ruleId`) REFERENCES `accessibilityRules`(`ruleId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`tagId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRuleTagRef_ruleId` ON `accessibilityRuleTagRef` (`ruleId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRuleTagRef_tagId` ON `accessibilityRuleTagRef` (`tagId`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `accessibilityRuleTransfers` (`ruleId` INTEGER NOT NULL, `exportAccount` TEXT, `importAccount` TEXT, PRIMARY KEY(`ruleId`), FOREIGN KEY(`ruleId`) REFERENCES `accessibilityRules`(`ruleId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `accessibilityRuleKeywordGroups` (`keywordId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ruleId` INTEGER NOT NULL, `content` TEXT, FOREIGN KEY(`ruleId`) REFERENCES `accessibilityRules`(`ruleId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRuleKeywordGroups_ruleId` ON `accessibilityRuleKeywordGroups` (`ruleId`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pickedPages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remark` TEXT, `packageName` TEXT DEFAULT '', `activityName` TEXT DEFAULT '', `dateTime` INTEGER)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pickedPages_packageName_activityName` ON `pickedPages` (`packageName`, `activityName`)");
        }
    };

    //更新通知规则的正则表达式，使其能够捕获带千位分隔符的数字
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("UPDATE notificationRules SET contentRegex = REPLACE(contentRegex, '(\\d+\\.?\\d{0,2})', '(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)')");
        }
    };

    //流水记录添加自动记账标签字段
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN autoTag INTEGER NOT NULL DEFAULT 0");
        }
    };

    //添加通知规则分组相关表格
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `notificationRuleGroups` (`groupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `notificationRuleGroupRef` (`ruleId` INTEGER NOT NULL, `groupId` INTEGER NOT NULL, `order` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`ruleId`, `groupId`), FOREIGN KEY(`ruleId`) REFERENCES `notificationRules`(`ruleId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`groupId`) REFERENCES `notificationRuleGroups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRuleGroupRef_order` ON `notificationRuleGroupRef` (`order`)");
        }
    };

    //添加规则分组排序序号
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notificationRuleGroups ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE notificationRuleGroups SET `order` = groupId");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRuleGroups_order` ON `notificationRuleGroups` (`order`)");
        }
    };

    //添加额外数据表
    //重建流水记录和两个自动记账规则表，用于添加外键约束
    //删除旧版的转账账户表
    //迁移旧版数据
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            //关闭外键
            db.execSQL("PRAGMA foreign_keys = OFF");

            //添加额外数据表
            db.execSQL("CREATE TABLE IF NOT EXISTS `extraTransfer` (`transferId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exportAccount` TEXT, `importAccount` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `extraLoan` (`loanId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `loanTargetName` TEXT, `interestPeriodType` INTEGER NOT NULL, `yearBasepoint` INTEGER NOT NULL, `allowCompoundInterest` INTEGER NOT NULL, `repayDate` INTEGER)");

            //创建带有外键约束的表
            db.execSQL("CREATE TABLE IF NOT EXISTS `accountsNew` (`accountId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `remark` TEXT, `type` INTEGER NOT NULL, `dateTime` INTEGER, `autoTag` INTEGER NOT NULL DEFAULT 0, `transferId` INTEGER DEFAULT NULL, `loanId` INTEGER DEFAULT NULL, `repayAccountId` INTEGER DEFAULT NULL, FOREIGN KEY(`transferId`) REFERENCES `extraTransfer`(`transferId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`loanId`) REFERENCES `extraLoan`(`loanId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`repayAccountId`) REFERENCES `accounts`(`accountId`) ON UPDATE CASCADE ON DELETE SET DEFAULT )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `notificationRulesNew` (`ruleId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `type` INTEGER NOT NULL, `packageName` TEXT, `targetTitle` TEXT, `contentRegex` TEXT, `captureGroupPos` INTEGER NOT NULL DEFAULT 1, `enabled` INTEGER NOT NULL DEFAULT true, `transferId` INTEGER DEFAULT NULL, `loanId` INTEGER DEFAULT NULL, `repayAccountId` INTEGER DEFAULT NULL, FOREIGN KEY(`transferId`) REFERENCES `extraTransfer`(`transferId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`loanId`) REFERENCES `extraLoan`(`loanId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`repayAccountId`) REFERENCES `accounts`(`accountId`) ON UPDATE CASCADE ON DELETE SET DEFAULT )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `accessibilityRulesNew` (`ruleId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `type` INTEGER NOT NULL, `enabled` INTEGER NOT NULL DEFAULT true, `packageName` TEXT, `activityName` TEXT, `transferId` INTEGER DEFAULT NULL, `loanId` INTEGER DEFAULT NULL, `repayAccountId` INTEGER DEFAULT NULL, FOREIGN KEY(`transferId`) REFERENCES `extraTransfer`(`transferId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`loanId`) REFERENCES `extraLoan`(`loanId`) ON UPDATE CASCADE ON DELETE SET DEFAULT , FOREIGN KEY(`repayAccountId`) REFERENCES `accounts`(`accountId`) ON UPDATE CASCADE ON DELETE SET DEFAULT )");

            //将需要重建的旧表数据迁移到新表（不包含转账账户等数据）
            db.execSQL("INSERT INTO accountsNew (accountId, amount, remark, type, dateTime, autoTag) SELECT accountId, amount, remark, type, dateTime, autoTag FROM accounts");
            db.execSQL("INSERT INTO notificationRulesNew (ruleId, name, type, packageName, targetTitle, contentRegex, captureGroupPos, enabled) SELECT ruleId, name, type, packageName, targetTitle, contentRegex, captureGroupPos, enabled FROM notificationRules");
            db.execSQL("INSERT INTO accessibilityRulesNew (ruleId, name, type, enabled, packageName, activityName) SELECT ruleId, name, type, enabled, packageName, activityName FROM accessibilityRules");

            //迁移流水记录的转账账户数据
            Cursor accountTransferCursor = db.query("SELECT accountId, exportAccount, importAccount FROM accountTransfers");
            while (accountTransferCursor.moveToNext()) {
                long oldId = accountTransferCursor.getLong(0);
                String exportAccount = accountTransferCursor.getString(1);
                String importAccount = accountTransferCursor.getString(2);

                // 插入 extraTransfer
                db.execSQL("INSERT INTO extraTransfer (exportAccount, importAccount) VALUES (?, ?)",
                        new Object[]{exportAccount, importAccount});

                // 取回新主键并更新
                long newTransferId;
                try (Cursor idCursor = db.query("SELECT last_insert_rowid()")) {
                    idCursor.moveToFirst();
                    newTransferId = idCursor.getLong(0);
                    db.execSQL("UPDATE accountsNew SET transferId = ? WHERE accountId = ?",
                            new Object[]{newTransferId, oldId});
                }
            }
            accountTransferCursor.close();

            //迁移通知规则的转账账户数据
            Cursor notificationRuleTransferCursor = db.query("SELECT ruleId, exportAccount, importAccount FROM notificationRuleTransfers");
            while (notificationRuleTransferCursor.moveToNext()) {
                long oldId = notificationRuleTransferCursor.getLong(0);
                String exportAccount = notificationRuleTransferCursor.getString(1);
                String importAccount = notificationRuleTransferCursor.getString(2);

                // 插入 extraTransfer
                db.execSQL("INSERT INTO extraTransfer (exportAccount, importAccount) VALUES (?, ?)",
                        new Object[]{exportAccount, importAccount});

                // 取回新主键并更新
                long newTransferId;
                try (Cursor idCursor = db.query("SELECT last_insert_rowid()")) {
                    idCursor.moveToFirst();
                    newTransferId = idCursor.getLong(0);
                    db.execSQL("UPDATE notificationRulesNew SET transferId = ? WHERE ruleId = ?",
                            new Object[]{newTransferId, oldId});
                }
            }
            notificationRuleTransferCursor.close();

            //迁移无障碍规则的转账账户数据
            Cursor accessibilityRuleTransferCursor = db.query("SELECT ruleId, exportAccount, importAccount FROM accessibilityRuleTransfers");
            while (accessibilityRuleTransferCursor.moveToNext()) {
                long oldId = accessibilityRuleTransferCursor.getLong(0);
                String exportAccount = accessibilityRuleTransferCursor.getString(1);
                String importAccount = accessibilityRuleTransferCursor.getString(2);

                // 插入 extraTransfer
                db.execSQL("INSERT INTO extraTransfer (exportAccount, importAccount) VALUES (?, ?)",
                        new Object[]{exportAccount, importAccount});

                // 取回新主键并更新
                long newTransferId;
                try (Cursor idCursor = db.query("SELECT last_insert_rowid()")) {
                    idCursor.moveToFirst();
                    newTransferId = idCursor.getLong(0);
                    db.execSQL("UPDATE accessibilityRulesNew SET transferId = ? WHERE ruleId = ?",
                            new Object[]{newTransferId, oldId});
                }
            }
            accessibilityRuleTransferCursor.close();

            //删除旧表
            db.execSQL("DROP TABLE accountTransfers");
            db.execSQL("DROP TABLE notificationRuleTransfers");
            db.execSQL("DROP TABLE accessibilityRuleTransfers");
            db.execSQL("DROP TABLE accounts");
            db.execSQL("DROP TABLE notificationRules");
            db.execSQL("DROP TABLE accessibilityRules");

            //将新建的临时表改为旧表的名称，并重新创建索引
            db.execSQL("ALTER TABLE accountsNew RENAME TO accounts");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_accountId` ON `accounts` (`accountId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_remark` ON `accounts` (`remark`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_type` ON `accounts` (`type`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_dateTime` ON `accounts` (`dateTime`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_transferId` ON `accounts` (`transferId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_loanId` ON `accounts` (`loanId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_repayAccountId` ON `accounts` (`repayAccountId`)");
            db.execSQL("ALTER TABLE notificationRulesNew RENAME TO notificationRules");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRules_enabled` ON `notificationRules` (`enabled`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRules_transferId` ON `notificationRules` (`transferId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRules_loanId` ON `notificationRules` (`loanId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificationRules_repayAccountId` ON `notificationRules` (`repayAccountId`)");
            db.execSQL("ALTER TABLE accessibilityRulesNew RENAME TO accessibilityRules");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRules_enabled` ON `accessibilityRules` (`enabled`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRules_transferId` ON `accessibilityRules` (`transferId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRules_loanId` ON `accessibilityRules` (`loanId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accessibilityRules_repayAccountId` ON `accessibilityRules` (`repayAccountId`)");

            //重启外键
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    };

    //将 TagEntity 的 groupId 由 long 改为 Long
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            //关闭外键
            db.execSQL("PRAGMA foreign_keys = OFF");

            //创建新表
            db.execSQL("CREATE TABLE IF NOT EXISTS `tagsNew` (`tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `scope` INTEGER NOT NULL DEFAULT 0, `groupId` INTEGER, FOREIGN KEY(`groupId`) REFERENCES `tagGroups`(`groupId`) ON UPDATE NO ACTION ON DELETE SET NULL )");

            //迁移数据
            db.execSQL("INSERT INTO tagsNew (name, scope, groupId) SELECT name, scope, groupId FROM tags");
            db.execSQL("UPDATE tagsNew SET groupId = NULL WHERE groupId < 0");

            //删除旧表和旧默认分组
            db.execSQL("DROP TABLE tags");
            db.execSQL("DELETE FROM tagGroups WHERE groupId < 0");

            //重命名新表并重建索引
            db.execSQL("ALTER TABLE tagsNew RENAME TO tags");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_tagId` ON `tags` (`tagId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_scope` ON `tags` (`scope`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_groupId` ON `tags` (`groupId`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)");

            //重启外键
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    };
}
