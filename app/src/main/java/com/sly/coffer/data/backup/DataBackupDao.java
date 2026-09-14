package com.sly.coffer.data.backup;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.sly.coffer.data.backup.maps.AccessibilityRuleDataMap;
import com.sly.coffer.data.backup.maps.BudgetDataMap;
import com.sly.coffer.data.backup.maps.NotificationRuleDataMap;
import com.sly.coffer.data.backup.maps.RunningAccountDataMap;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTransferPojo;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccountTransferPojo;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTransferPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Dao
public interface DataBackupDao {
    /**
     * 导出流水记录数据
     *
     * @return 流水记录数据集合
     */
    @Transaction
    default RunningAccountDataMap exportRunningAccountData() {
        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //流水记录数据
        List<AccountEntity> accountEntityList = readRunningAccount();
        List<AccountPojo> accountPojoList = mapper.toAccountPojoList(accountEntityList);

        //流水标签数据
        List<AccountTagRefEntity> accountTagRefEntityList = readAccountTagRef();
        List<AccountTagRefPojo> accountTagRefPojoList = mapper.toAccountTagRefPojoList(accountTagRefEntityList);

        //流水转账账户数据
        List<AccountTransferEntity> accountTransferEntityList = readAccountTransfer();
        List<AccountTransferPojo> accountTransferPojoList = mapper.toAccountTransferPojoList(accountTransferEntityList);

        //媒体数据
        List<MediaEntity> mediaEntityList = readMedia();
        List<MediaPojo> mediaPojoList = mapper.toMediaPojoList(mediaEntityList);

        //标签分组数据
        List<TagGroupEntity> tagGroupEntityList = readTagGroup();
        List<TagGroupPojo> tagGroupPojoList = mapper.toTagGroupPojoList(tagGroupEntityList);

        //标签数据
        List<TagEntity> tagEntityList = readTag();
        List<TagPojo> tagPojoList = mapper.toTagPojoList(tagEntityList);

        //实例化 Map 类
        RunningAccountDataMap map = new RunningAccountDataMap();
        map.setAccountList(accountPojoList);
        map.setAccountTagRefList(accountTagRefPojoList);
        map.setAccountTransferList(accountTransferPojoList);
        map.setMediaList(mediaPojoList);
        map.setTagGroupList(tagGroupPojoList);
        map.setTagList(tagPojoList);

        return map;
    }

    /**
     * 导入流水记录数据
     *
     * @param data 流水记录数据集合
     */
    @Transaction
    default void importRunningAccountData(Context context, RunningAccountDataMap data) {
        if (data == null) return;

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //清空旧数据
        clearRunningAccount();
        clearAccountTagRef();
        clearAccountTransfer();
        clearMedia();
        clearTagGroup();
        clearTag();

        //标签分组数据
        List<TagGroupPojo> tagGroupPojoList = data.getTagGroupList();
        if (tagGroupPojoList != null && !tagGroupPojoList.isEmpty()) {
            writeTagGroup(mapper.toTagGroupEntityList(tagGroupPojoList));
        }

        //标签数据
        List<TagPojo> tagPojoList = data.getTagList();
        if (tagPojoList != null && !tagPojoList.isEmpty()) {
            writeTag(mapper.toTagEntityList(tagPojoList));
        }

        //流水记录数据
        List<AccountPojo> accountPojoList = data.getAccountList();
        if (accountPojoList != null && !accountPojoList.isEmpty()) {
            writeRunningAccount(mapper.toAccountEntityList(accountPojoList));
        }

        //流水标签数据
        List<AccountTagRefPojo> accountTagRefPojoList = data.getAccountTagRefList();
        if (accountTagRefPojoList != null && !accountTagRefPojoList.isEmpty()) {
            writeAccountTagRef(mapper.toAccountTagRefEntityList(accountTagRefPojoList));
        }

        //流水转账账户数据
        List<AccountTransferPojo> accountTransferPojoList = data.getAccountTransferList();
        if (accountTransferPojoList != null && !accountTransferPojoList.isEmpty()) {
            writeAccountTransfer(mapper.toAccountTransferEntityList(accountTransferPojoList));
        }

        //媒体数据
        List<MediaPojo> mediaPojoList = data.getMediaList();
        if (mediaPojoList != null && !mediaPojoList.isEmpty()) {
            //替换 Uri 中的包名
            String replacement = String.format(
                    Locale.getDefault(),
                    "Android/data/%s/files/",
                    context.getPackageName()
            );
            List<MediaPojo> uriConvertedMediaPojoList = mediaPojoList.stream()
                    .peek(media -> {
                        String uriStr = media.getFileUri();
                        String currentPackageNameUri = uriStr.replaceAll(
                                "Android/data/([^/]+)/files/",
                                replacement
                        );
                        media.setFileUri(currentPackageNameUri);
                    })
                    .collect(Collectors.toList());

            writeMedia(mapper.toMediaEntityList(uriConvertedMediaPojoList));
        }
    }

    /**
     * 导出通知规则数据
     *
     * @return 通知规则数据集合
     */
    @Transaction
    default NotificationRuleDataMap exportNotificationRuleData() {
        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //通知规则数据
        List<NotificationRuleEntity> notificationRuleEntityList = readNotificationRule();
        List<NotificationRulePojo> notificationRulePojoList = mapper.toNotificationRulePojoList(notificationRuleEntityList);

        //绑定的标签数据
        List<NotificationRuleTagRefEntity> notificationRuleTagRefEntityList = readNotificationRuleTagRef();
        List<NotificationRuleTagRefPojo> notificationRuleTagRefPojoList = mapper.toNotificationRuleTagRefPojoList(notificationRuleTagRefEntityList);

        //转账账户数据
        List<NotificationRuleTransferEntity> notificationRuleTransferEntityList = readNotificationRuleTransfer();
        List<NotificationRuleTransferPojo> notificationRuleTransferPojoList = mapper.toNotificationRuleTransferPojoList(notificationRuleTransferEntityList);

        //实例化 Map
        NotificationRuleDataMap map = new NotificationRuleDataMap();
        map.setNotificationRuleList(notificationRulePojoList);
        map.setNotificationRuleTagRefList(notificationRuleTagRefPojoList);
        map.setNotificationRuleTransferList(notificationRuleTransferPojoList);

        return map;
    }

    /**
     * 导入通知规则数据
     *
     * @param data 通知规则数据集合
     */
    @Transaction
    default void importNotificationRuleData(NotificationRuleDataMap data) {
        if (data == null) return;

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //清空旧数据
        clearNotificationRule();
        clearNotificationRuleTagRef();
        clearNotificationRuleTransfer();

        //通知规则数据
        List<NotificationRulePojo> notificationRulePojoList = data.getNotificationRuleList();
        if (notificationRulePojoList != null && !notificationRulePojoList.isEmpty()) {
            writeNotificationRule(mapper.toNotificationRuleEntityList(notificationRulePojoList));
        }

        //绑定的标签数据
        List<Long> existedTagIdList = getExistedTagId();
        List<NotificationRuleTagRefPojo> notificationRuleTagRefPojoList = data.getNotificationRuleTagRefList();
        if (notificationRuleTagRefPojoList != null && !notificationRuleTagRefPojoList.isEmpty()) {
            List<NotificationRuleTagRefPojo> filteredTagRefPojoList = notificationRuleTagRefPojoList.stream()
                    .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                    .collect(Collectors.toList());  //仅写入标签存在的映射
            writeNotificationRuleTagRef(mapper.toNotificationRuleTagRefEntityList(filteredTagRefPojoList));
        }

        //转账账户数据
        List<NotificationRuleTransferPojo> notificationRuleTransferPojoList = data.getNotificationRuleTransferList();
        if (notificationRuleTransferPojoList != null && !notificationRuleTransferPojoList.isEmpty()) {
            writeNotificationRuleTransfer(mapper.toNotificationRuleTransferEntityList(notificationRuleTransferPojoList));
        }
    }

    /**
     * 导出预算数据
     *
     * @return 预算数据集合
     */
    @Transaction
    default BudgetDataMap exportBudgetData() {
        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //预算数据
        List<BudgetEntity> budgetEntityList = readBudget();
        List<BudgetPojo> budgetPojoList = mapper.toBudgetPojoList(budgetEntityList);

        //预算标签数据
        List<BudgetTagRefEntity> budgetTagRefEntityList = readBudgetTagRef();
        List<BudgetTagRefPojo> budgetTagRefPojoList = mapper.toBudgetTagRefPojoList(budgetTagRefEntityList);

        //实例化 Map
        BudgetDataMap map = new BudgetDataMap();
        map.setBudgetList(budgetPojoList);
        map.setBudgetTagRefList(budgetTagRefPojoList);

        return map;
    }

    /**
     * 导入预算数据
     *
     * @param data 预算数据集合
     */
    @Transaction
    default void importBudgetData(BudgetDataMap data) {
        if (data == null) return;

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //清空旧数据
        clearBudget();
        clearBudgetTagRef();

        //预算数据
        List<BudgetPojo> budgetPojoList = data.getBudgetList();
        if (budgetPojoList != null && !budgetPojoList.isEmpty()) {
            writeBudget(mapper.toBudgetEntityList(budgetPojoList));
        }

        //预算标签数据
        List<Long> existedTagIdList = getExistedTagId();
        List<BudgetTagRefPojo> budgetTagRefPojoList = data.getBudgetTagRefList();
        if (budgetTagRefPojoList != null && !budgetTagRefPojoList.isEmpty()) {
            List<BudgetTagRefPojo> filteredBudgetTagRefPojoList = budgetTagRefPojoList.stream()
                    .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                    .collect(Collectors.toList());  //仅写入存在的标签的映射
            writeBudgetTagRef(mapper.toBudgetTagRefEntityList(filteredBudgetTagRefPojoList));
        }
    }

    /**
     * 导出无障碍规则数据
     *
     * @return 包含无障碍规则数据的集合
     */
    default AccessibilityRuleDataMap exportAccessibilityRuleData() {
        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //规则
        List<AccessibilityRuleEntity> ruleEntityList = readAccessibilityRule();
        List<AccessibilityRulePojo> rulePojoList = mapper.toAccessibilityRulePojoList(ruleEntityList);

        //标签映射
        List<AccessibilityRuleTagRefEntity> tagRefEntityList = readAccessibilityRuleTagRef();
        List<AccessibilityRuleTagRefPojo> tagRefPojoList = mapper.toAccessibilityRuleTagRefPojoList(tagRefEntityList);

        //转账账户
        List<AccessibilityRuleTransferEntity> transferEntityList = readAccessibilityRuleTransfer();
        List<AccessibilityRuleTransferPojo> transferPojoList = mapper.toAccessibilityRuleTransferPojoList(transferEntityList);

        //关键词组合
        List<AccessibilityRuleKeywordGroupEntity> keywordGroupEntityList = readAccessibilityRuleKeywordGroup();
        List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList = mapper.toAccessibilityRuleKeywordGroupPojoList(keywordGroupEntityList);

        //实例化 Map
        AccessibilityRuleDataMap map = new AccessibilityRuleDataMap();
        map.setRulePojoList(rulePojoList);
        map.setTagRefPojoList(tagRefPojoList);
        map.setTransferPojoList(transferPojoList);
        map.setKeywordGroupPojoList(keywordGroupPojoList);

        return map;
    }

    /**
     * 导入无障碍规则数据
     *
     * @param data 包含无障碍规则数据的集合
     */
    default void importAccessibilityRuleData(AccessibilityRuleDataMap data) {
        if (data == null) return;

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //清空旧数据
        clearAccessibilityRule();
        clearAccessibilityRuleTagRef();
        clearAccessibilityRuleTransfer();
        clearAccessibilityRuleKeywordGroup();

        //规则
        List<AccessibilityRulePojo> rulePojoList = data.getRulePojoList();
        if (rulePojoList != null && !rulePojoList.isEmpty()) {
            List<AccessibilityRuleEntity> ruleEntityList = mapper.toAccessibilityRuleEntityList(data.getRulePojoList());
            writeAccessibilityRule(ruleEntityList);
        }

        //标签映射
        List<Long> existedTagIdList = getExistedTagId();
        List<AccessibilityRuleTagRefPojo> tagRefPojoList = data.getTagRefPojoList();
        if (tagRefPojoList != null && !tagRefPojoList.isEmpty()) {
            List<AccessibilityRuleTagRefPojo> filteredTagRefPojoList = tagRefPojoList.stream()
                    .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                    .collect(Collectors.toList());  //仅写入存在的标签映射
            writeAccessibilityRuleTagRef(mapper.toAccessibilityRuleTagRefEntityList(filteredTagRefPojoList));
        }

        //转账账户
        List<AccessibilityRuleTransferPojo> transferPojoList = data.getTransferPojoList();
        if (transferPojoList != null && !transferPojoList.isEmpty()) {
            List<AccessibilityRuleTransferEntity> transferEntityList = mapper.toAccessibilityRuleTransferEntityList(transferPojoList);
            writeAccessibilityRuleTransfer(transferEntityList);
        }

        //关键词组合
        List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList = data.getKeywordGroupPojoList();
        if (keywordGroupPojoList != null && !keywordGroupPojoList.isEmpty()) {
            List<AccessibilityRuleKeywordGroupEntity> keywordGroupEntityList = mapper.toAccessibilityRuleKeywordGroupEntityList(keywordGroupPojoList);
            writeAccessibilityRuleKeywordGroup(keywordGroupEntityList);
        }
    }

    /**
     * 读取流水记录数据
     *
     * @return 流水记录列表
     */
    @Query("SELECT * FROM accounts")
    List<AccountEntity> readRunningAccount();

    /**
     * 清空流水记录数据
     */
    @Query("DELETE FROM accounts")
    void clearRunningAccount();

    /**
     * 写入流水记录数据
     *
     * @param entityList 需要写入的流水记录数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeRunningAccount(List<AccountEntity> entityList);

    /**
     * 读取流水记录和标签映射关系数据
     *
     * @return 映射关系数据列表
     */
    @Query("SELECT * FROM accountTagRef")
    List<AccountTagRefEntity> readAccountTagRef();

    /**
     * 清空流水标签引用数据
     */
    @Query("DELETE FROM accountTagRef")
    void clearAccountTagRef();

    /**
     * 写入流水标签引用数据
     *
     * @param entityList 流水标签引用数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccountTagRef(List<AccountTagRefEntity> entityList);

    /**
     * 读取流水记录的转账账户数据
     *
     * @return 转账账户数据列表
     */
    @Query("SELECT * FROM accounttransfers")
    List<AccountTransferEntity> readAccountTransfer();

    /**
     * 清空流水记录转账账户数据
     */
    @Query("DELETE FROM accountTransfers")
    void clearAccountTransfer();

    /**
     * 写入流水记录转账账户数据
     *
     * @param entityList 转账账户数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccountTransfer(List<AccountTransferEntity> entityList);

    /**
     * 读取媒体数据
     *
     * @return 媒体数据列表
     */
    @Query("SELECT * FROM medias")
    List<MediaEntity> readMedia();

    /**
     * 清空媒体数据
     */
    @Query("DELETE FROM medias")
    void clearMedia();

    /**
     * 写入媒体数据
     *
     * @param entityList 媒体数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeMedia(List<MediaEntity> entityList);

    /**
     * 读取标签分组数据
     *
     * @return 标签分组列表
     */
    @Query("SELECT * FROM taggroups")
    List<TagGroupEntity> readTagGroup();

    /**
     * 清空标签分组数据
     */
    @Query("DELETE FROM taggroups")
    void clearTagGroup();

    /**
     * 写入标签分组数据
     *
     * @param entityList 标签分组数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeTagGroup(List<TagGroupEntity> entityList);

    /**
     * 读取标签数据
     *
     * @return 标签列表
     */
    @Query("SELECT * FROM tags")
    List<TagEntity> readTag();

    /**
     * 清空标签数据
     */
    @Query("DELETE FROM tags")
    void clearTag();

    /**
     * 写入标签数据
     *
     * @param entityList 标签数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeTag(List<TagEntity> entityList);

    /**
     * 读取通知规则数据
     *
     * @return 通知规则数据列表
     */
    @Query("SELECT * FROM notificationRules")
    List<NotificationRuleEntity> readNotificationRule();

    /**
     * 清空通知规则数据
     */
    @Query("DELETE FROM notificationRules")
    void clearNotificationRule();

    /**
     * 写入通知规则数据
     *
     * @param entityList 通知规则数据列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeNotificationRule(List<NotificationRuleEntity> entityList);

    /**
     * 读取通知规则绑定的标签数据
     *
     * @return 绑定的标签数据列表
     */
    @Query("SELECT * FROM notificationRuleTagRef")
    List<NotificationRuleTagRefEntity> readNotificationRuleTagRef();

    /**
     * 清空通知规则绑定的标签数据
     */
    @Query("DELETE FROM notificationRuleTagRef")
    void clearNotificationRuleTagRef();

    /**
     * 写入通知规则绑定的标签数据
     *
     * @param entityList 绑定的标签数据列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeNotificationRuleTagRef(List<NotificationRuleTagRefEntity> entityList);

    /**
     * 读取通知规则转账账户数据
     *
     * @return 转账账户数据列表
     */
    @Query("SELECT * FROM notificationRuleTransfers")
    List<NotificationRuleTransferEntity> readNotificationRuleTransfer();

    /**
     * 清空通知规则转账账户数据
     */
    @Query("DELETE FROM notificationRuleTransfers")
    void clearNotificationRuleTransfer();

    /**
     * 写入通知规则转账账户数据
     *
     * @param entityList 转账账户数据列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeNotificationRuleTransfer(List<NotificationRuleTransferEntity> entityList);

    /**
     * 读取预算数据
     *
     * @return 预算列表
     */
    @Query("SELECT * FROM budgets")
    List<BudgetEntity> readBudget();

    /**
     * 清空预算数据
     */
    @Query("DELETE FROM budgets")
    void clearBudget();

    /**
     * 写入预算数据
     *
     * @param entityList 预算列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeBudget(List<BudgetEntity> entityList);

    /**
     * 读取预算标签数据
     *
     * @return 预算标签列表
     */
    @Query("SELECT * FROM budgetTagRef")
    List<BudgetTagRefEntity> readBudgetTagRef();

    /**
     * 清空预算标签数据
     */
    @Query("DELETE FROM budgetTagRef")
    void clearBudgetTagRef();

    /**
     * 写入预算标签数据
     *
     * @param entityList 预算标签数据列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeBudgetTagRef(List<BudgetTagRefEntity> entityList);

    /**
     * 读取无障碍规则
     *
     * @return 无障碍规则列表
     */
    @Query("SELECT * FROM accessibilityRules")
    List<AccessibilityRuleEntity> readAccessibilityRule();

    /**
     * 清空无障碍规则
     */
    @Query("DELETE FROM accessibilityRules")
    void clearAccessibilityRule();

    /**
     * 写入无障碍规则
     *
     * @param entityList 待写入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccessibilityRule(List<AccessibilityRuleEntity> entityList);

    /**
     * 读取无障碍规则与标签的映射
     *
     * @return 无障碍规则与标签的映射
     */
    @Query("SELECT * FROM accessibilityRuleTagRef")
    List<AccessibilityRuleTagRefEntity> readAccessibilityRuleTagRef();

    /**
     * 清空无障碍规则与标签的映射
     */
    @Query("DELETE FROM accessibilityRuleTagRef")
    void clearAccessibilityRuleTagRef();

    /**
     * 写入无障碍规则与标签的映射
     *
     * @param entityList 待写入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccessibilityRuleTagRef(List<AccessibilityRuleTagRefEntity> entityList);

    /**
     * 读取无障碍规则的转账账户
     *
     * @return 无障碍规则的转账账户
     */
    @Query("SELECT * FROM accessibilityRuleTransfers")
    List<AccessibilityRuleTransferEntity> readAccessibilityRuleTransfer();

    /**
     * 清空无障碍规则的转账账户数据
     */
    @Query("DELETE FROM accessibilityRuleTransfers")
    void clearAccessibilityRuleTransfer();

    /**
     * 写入无障碍规则的转账账户
     *
     * @param entityList 待写入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccessibilityRuleTransfer(List<AccessibilityRuleTransferEntity> entityList);

    /**
     * 读取无障碍规则的关键词组合
     *
     * @return 无障碍规则的关键词组合
     */
    @Query("SELECT * FROM accessibilityRuleKeywordGroups")
    List<AccessibilityRuleKeywordGroupEntity> readAccessibilityRuleKeywordGroup();

    /**
     * 清空无障碍规则的关键词组合
     */
    @Query("DELETE FROM accessibilityRuleKeywordGroups")
    void clearAccessibilityRuleKeywordGroup();

    /**
     * 写入无障碍规则的关键词组合
     *
     * @param entityList 待写入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeAccessibilityRuleKeywordGroup(List<AccessibilityRuleKeywordGroupEntity> entityList);

    /**
     * 获取现存的标签编号
     *
     * @return 现存的标签编号
     */
    @Query("SELECT tagId FROM tags")
    List<Long> getExistedTagId();
}
