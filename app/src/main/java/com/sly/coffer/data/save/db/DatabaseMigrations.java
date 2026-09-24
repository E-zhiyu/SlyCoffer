package com.sly.coffer.data.save.db;

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
            db.execSQL( "CREATE INDEX IF NOT EXISTS `index_notificationRuleGroups_order` ON `notificationRuleGroups` (`order`)");
        }
    };
}
