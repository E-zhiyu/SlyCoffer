package com.sly.coffer.data.backup;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.sly.coffer.auxiliary.classes.ImportResult;
import com.sly.coffer.data.backup.maps.AccessibilityRuleDataMap;
import com.sly.coffer.data.backup.maps.BudgetDataMap;
import com.sly.coffer.data.backup.maps.NotificationRuleDataMap;
import com.sly.coffer.data.backup.maps.RunningAccountDataMap;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.ExtraLoanPojo;
import com.sly.coffer.data.backup.pojo.ExtraTransferPojo;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

        //转账账户数据
        Set<Long> transferIdSet = accountEntityList.stream()
                .map(AccountEntity::getTransferId)
                .collect(Collectors.toSet());
        List<ExtraTransferEntity> extraTransferEntityList = readExtraTransferById(transferIdSet);
        List<ExtraTransferPojo> extraTransferPojoList = mapper.toExtraTransferPojoList(extraTransferEntityList);

        //借款记录数据
        Set<Long> loanIdSet = accountEntityList.stream()
                .map(AccountEntity::getLoanId)
                .collect(Collectors.toSet());
        List<ExtraLoanEntity> extraLoanEntityList = readExtraLoanById(loanIdSet);
        List<ExtraLoanPojo> extraLoanPojoList = mapper.toExtraLoanPojoList(extraLoanEntityList);

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
        map.setExtraTransferList(extraTransferPojoList);
        map.setExtraLoanList(extraLoanPojoList);
        map.setMediaList(mediaPojoList);
        map.setTagGroupList(tagGroupPojoList);
        map.setTagList(tagPojoList);

        return map;
    }

    /**
     * 导入流水记录数据
     *
     * @param context 上下文
     * @param data    流水记录数据集合
     * @return 数据导入的结果
     */
    @Transaction
    default ImportResult importRunningAccountData(Context context, RunningAccountDataMap data) {
        if (data == null) return ImportResult.missed();

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;
        List<Throwable> exceptionList = new ArrayList<>();

        //清理旧的额外数据
        List<Long> existedTransferId = getExistedAccountTransferId();
        deleteExtraTransferById(existedTransferId);
        List<Long> existedLoanId = getExistedAccountLoanId();
        deleteExtraLoanById(existedLoanId);

        //清空旧数据
        clearRunningAccount();
        clearAccountTagRef();
        clearMedia();
        clearTagGroup();
        clearTag();

        //标签分组数据
        try {
            List<TagGroupPojo> tagGroupPojoList = data.getTagGroupList();
            if (tagGroupPojoList != null && !tagGroupPojoList.isEmpty()) {
                writeTagGroup(mapper.toTagGroupEntityList(tagGroupPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //标签数据
        try {
            List<TagPojo> tagPojoList = data.getTagList();
            if (tagPojoList != null && !tagPojoList.isEmpty()) {
                writeTag(mapper.toTagEntityList(tagPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //流水记录数据
        try {
            List<AccountPojo> accountPojoList = data.getAccountList();
            if (accountPojoList != null && !accountPojoList.isEmpty()) {
                //转账账户数据
                List<AccountEntity> accountEntityList = mapper.toAccountEntityList(accountPojoList);
                Map<Long, List<AccountEntity>> transferIdMap = accountEntityList.stream()
                        .filter(account -> account.getTransferId() != null)
                        .collect(Collectors.groupingBy(
                                AccountEntity::getTransferId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraTransferPojo> extraTransferPojoList = data.getExtraTransferList();
                if (extraTransferPojoList != null && !extraTransferPojoList.isEmpty()) {
                    List<ExtraTransferEntity> extraTransferEntityList = mapper.toExtraTransferEntityList(extraTransferPojoList);
                    for (ExtraTransferEntity transfer : extraTransferEntityList) {
                        long originId = transfer.getTransferId();
                        if (hasTransferIdConflict(originId)) {
                            transfer.setTransferId(0);  //设置为0以触发自增
                            long newId = writeExtraTransfer(transfer);
                            List<AccountEntity> accountList = transferIdMap.get(originId);
                            if (accountList != null) {
                                for (AccountEntity account : accountList) {
                                    account.setTransferId(newId);
                                }
                            }
                            transferIdMap.remove(originId);
                        } else {
                            writeExtraTransfer(transfer);
                        }
                    }
                }

                //借款记录数据
                Map<Long, List<AccountEntity>> loanIdMap = accountEntityList.stream()
                        .filter(account -> account.getLoanId() != null)
                        .collect(Collectors.groupingBy(
                                AccountEntity::getLoanId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraLoanPojo> extraLoanPojoList = data.getExtraLoanList();
                if (extraLoanPojoList != null && !extraLoanPojoList.isEmpty()) {
                    List<ExtraLoanEntity> extraLoanEntityList = mapper.toExtraLoanEntityList(extraLoanPojoList);
                    for (ExtraLoanEntity loan : extraLoanEntityList) {
                        long originId = loan.getLoanId();
                        if (hasLoanIdConflict(originId)) {
                            loan.setLoanId(0);  //设置为0以触发自增
                            long newId = writeExtraLoan(loan);
                            List<AccountEntity> accountList = loanIdMap.get(originId);
                            if (accountList != null) {
                                for (AccountEntity account : accountList) {
                                    account.setLoanId(newId);
                                }
                            }
                            loanIdMap.remove(originId);
                        } else {
                            writeExtraLoan(loan);
                        }
                    }
                }

                //写入流水记录数据
                accountEntityList.sort(Comparator.comparing(AccountEntity::getRepayAccountId,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
                writeRunningAccount(accountEntityList);
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //流水标签数据
        try {
            List<AccountTagRefPojo> accountTagRefPojoList = data.getAccountTagRefList();
            if (accountTagRefPojoList != null && !accountTagRefPojoList.isEmpty()) {
                writeAccountTagRef(mapper.toAccountTagRefEntityList(accountTagRefPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //媒体数据
        try {
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
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //返回导入结果
        if (exceptionList.isEmpty()) {
            return ImportResult.success();
        } else {
            return ImportResult.partlySuccess(exceptionList);
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
        Set<Long> transferIdSet = notificationRuleEntityList.stream()
                .map(NotificationRuleEntity::getTransferId)
                .collect(Collectors.toSet());
        List<ExtraTransferEntity> extraTransferEntityList = readExtraTransferById(transferIdSet);
        List<ExtraTransferPojo> extraTransferPojoList = mapper.toExtraTransferPojoList(extraTransferEntityList);

        //借款记录数据
        Set<Long> loanIdSet = notificationRuleEntityList.stream()
                .map(NotificationRuleEntity::getLoanId)
                .collect(Collectors.toSet());
        List<ExtraLoanEntity> extraLoanEntityList = readExtraLoanById(loanIdSet);
        List<ExtraLoanPojo> extraLoanPojoList = mapper.toExtraLoanPojoList(extraLoanEntityList);

        //分组数据
        List<NotificationRuleGroupEntity> notificationRuleGroupEntityList = readNotificationRuleGroup();
        List<NotificationRuleGroupPojo> notificationRuleGroupPojoList = mapper.toNotificationRuleGroupPojoList(notificationRuleGroupEntityList);

        //规则与分组的映射
        List<NotificationRuleGroupRefEntity> notificationRuleGroupRefEntityList = readNotificationRuleGroupRef();
        List<NotificationRuleGroupRefPojo> notificationRuleGroupRefPojoList = mapper.toNotificationRuleGroupRefPojoList(notificationRuleGroupRefEntityList);

        //实例化 Map
        NotificationRuleDataMap map = new NotificationRuleDataMap();
        map.setNotificationRuleList(notificationRulePojoList);
        map.setNotificationRuleTagRefList(notificationRuleTagRefPojoList);
        map.setExtraTransferList(extraTransferPojoList);
        map.setExtraLoanList(extraLoanPojoList);
        map.setNotificationRuleGroupList(notificationRuleGroupPojoList);
        map.setNotificationRuleGroupRefList(notificationRuleGroupRefPojoList);

        return map;
    }

    /**
     * 导入通知规则数据
     *
     * @param data 通知规则数据集合
     * @return 数据导入的结果
     */
    @Transaction
    default ImportResult importNotificationRuleData(NotificationRuleDataMap data) {
        if (data == null) return ImportResult.missed();

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;
        List<Throwable> exceptionList = new ArrayList<>();

        //清空额外数据
        List<Long> existedTransferIdList = getExistedNotificationRuleTransferId();
        deleteExtraTransferById(existedTransferIdList);
        List<Long> existedLoanIdList = getExistedNotificationRuleLoanId();
        deleteExtraLoanById(existedLoanIdList);

        //清空旧数据
        clearNotificationRule();
        clearNotificationRuleTagRef();
        clearNotificationRuleGroup();
        clearNotificationRuleGroupRef();

        //通知规则数据
        try {
            List<NotificationRulePojo> notificationRulePojoList = data.getNotificationRuleList();
            if (notificationRulePojoList != null && !notificationRulePojoList.isEmpty()) {
                //转账账户数据
                List<NotificationRuleEntity> notificationRuleEntityList = mapper.toNotificationRuleEntityList(notificationRulePojoList);
                Map<Long, List<NotificationRuleEntity>> transferIdMap = notificationRuleEntityList.stream()
                        .filter(rule -> rule.getTransferId() != null)
                        .collect(Collectors.groupingBy(
                                NotificationRuleEntity::getTransferId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraTransferPojo> extraTransferPojoList = data.getExtraTransferList();
                if (extraTransferPojoList != null && !extraTransferPojoList.isEmpty()) {
                    List<ExtraTransferEntity> extraTransferEntityList = mapper.toExtraTransferEntityList(extraTransferPojoList);
                    for (ExtraTransferEntity transfer : extraTransferEntityList) {
                        long originTransferId = transfer.getTransferId();
                        if (hasTransferIdConflict(originTransferId)) {
                            transfer.setTransferId(0);  //设置为0以触发自增
                            long newTransferId = writeExtraTransfer(transfer);
                            List<NotificationRuleEntity> accountList = transferIdMap.get(originTransferId);
                            if (accountList != null) {
                                for (NotificationRuleEntity rule : accountList) {
                                    rule.setTransferId(newTransferId);
                                }
                            }
                            transferIdMap.remove(originTransferId);
                        } else {
                            writeExtraTransfer(transfer);
                        }
                    }
                }

                //借款记录数据
                Map<Long, List<NotificationRuleEntity>> loanIdMap = notificationRuleEntityList.stream()
                        .filter(rule -> rule.getLoanId() != null)
                        .collect(Collectors.groupingBy(
                                NotificationRuleEntity::getLoanId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraLoanPojo> extraLoanPojoList = data.getExtraLoanList();
                if (extraLoanPojoList != null && !extraLoanPojoList.isEmpty()) {
                    List<ExtraLoanEntity> extraLoanEntityList = mapper.toExtraLoanEntityList(extraLoanPojoList);
                    for (ExtraLoanEntity loan : extraLoanEntityList) {
                        long originId = loan.getLoanId();
                        if (hasLoanIdConflict(originId)) {
                            loan.setLoanId(0);  //设置为0以触发自增
                            long newId = writeExtraLoan(loan);
                            List<NotificationRuleEntity> accountList = loanIdMap.get(originId);
                            if (accountList != null) {
                                for (NotificationRuleEntity rule : accountList) {
                                    rule.setLoanId(newId);
                                }
                            }
                            loanIdMap.remove(originId);
                        } else {
                            writeExtraLoan(loan);
                        }
                    }
                }

                //写入通知规则数据
                writeNotificationRule(notificationRuleEntityList);
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //绑定的标签数据
        try {
            List<Long> existedTagIdList = getExistedTagId();
            List<NotificationRuleTagRefPojo> notificationRuleTagRefPojoList = data.getNotificationRuleTagRefList();
            if (notificationRuleTagRefPojoList != null && !notificationRuleTagRefPojoList.isEmpty()) {
                List<NotificationRuleTagRefPojo> filteredTagRefPojoList = notificationRuleTagRefPojoList.stream()
                        .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                        .collect(Collectors.toList());  //仅写入标签存在的映射
                writeNotificationRuleTagRef(mapper.toNotificationRuleTagRefEntityList(filteredTagRefPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //分组数据
        try {
            List<NotificationRuleGroupPojo> notificationRuleGroupPojoList = data.getNotificationRuleGroupList();
            if (notificationRuleGroupPojoList != null && !notificationRuleGroupPojoList.isEmpty()) {
                writeNotificationRuleGroup(mapper.toNotificationRuleGroupEntityList(notificationRuleGroupPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //规则与分组的映射
        try {
            List<NotificationRuleGroupRefPojo> notificationRuleGroupRefPojoList = data.getNotificationRuleGroupRefList();
            if (notificationRuleGroupRefPojoList != null && !notificationRuleGroupRefPojoList.isEmpty()) {
                writeNotificationRuleGroupRef(mapper.toNotificationRuleGroupRefEntityList(notificationRuleGroupRefPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //返回导入结果
        if (exceptionList.isEmpty()) {
            return ImportResult.success();
        } else {
            return ImportResult.partlySuccess(exceptionList);
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
     * @return 数据导入的结果
     */
    @Transaction
    default ImportResult importBudgetData(BudgetDataMap data) {
        if (data == null) return ImportResult.missed();

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;
        List<Throwable> exceptionList = new ArrayList<>();

        //清空旧数据
        clearBudget();
        clearBudgetTagRef();

        //预算数据
        try {
            List<BudgetPojo> budgetPojoList = data.getBudgetList();
            if (budgetPojoList != null && !budgetPojoList.isEmpty()) {
                writeBudget(mapper.toBudgetEntityList(budgetPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //预算标签数据
        try {
            List<Long> existedTagIdList = getExistedTagId();
            List<BudgetTagRefPojo> budgetTagRefPojoList = data.getBudgetTagRefList();
            if (budgetTagRefPojoList != null && !budgetTagRefPojoList.isEmpty()) {
                List<BudgetTagRefPojo> filteredBudgetTagRefPojoList = budgetTagRefPojoList.stream()
                        .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                        .collect(Collectors.toList());  //仅写入存在的标签的映射
                writeBudgetTagRef(mapper.toBudgetTagRefEntityList(filteredBudgetTagRefPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //返回导入结果
        if (exceptionList.isEmpty()) {
            return ImportResult.success();
        } else {
            return ImportResult.partlySuccess(exceptionList);
        }
    }

    /**
     * 导出无障碍规则数据
     *
     * @return 包含无障碍规则数据的集合
     */
    @Transaction
    default AccessibilityRuleDataMap exportAccessibilityRuleData() {
        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;

        //规则
        List<AccessibilityRuleEntity> ruleEntityList = readAccessibilityRule();
        List<AccessibilityRulePojo> rulePojoList = mapper.toAccessibilityRulePojoList(ruleEntityList);

        //标签映射
        List<AccessibilityRuleTagRefEntity> tagRefEntityList = readAccessibilityRuleTagRef();
        List<AccessibilityRuleTagRefPojo> tagRefPojoList = mapper.toAccessibilityRuleTagRefPojoList(tagRefEntityList);

        //转账账户
        Set<Long> transferIdSet = ruleEntityList.stream()
                .map(AccessibilityRuleEntity::getTransferId)
                .collect(Collectors.toSet());
        List<ExtraTransferEntity> transferEntityList = readExtraTransferById(transferIdSet);
        List<ExtraTransferPojo> transferPojoList = mapper.toExtraTransferPojoList(transferEntityList);

        //借款记录
        Set<Long> loanIdSet = ruleEntityList.stream()
                .map(AccessibilityRuleEntity::getLoanId)
                .collect(Collectors.toSet());
        List<ExtraLoanEntity> extraLoanEntityList = readExtraLoanById(loanIdSet);
        List<ExtraLoanPojo> extraLoanPojoList = mapper.toExtraLoanPojoList(extraLoanEntityList);

        //关键词组合
        List<AccessibilityRuleKeywordGroupEntity> keywordGroupEntityList = readAccessibilityRuleKeywordGroup();
        List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList = mapper.toAccessibilityRuleKeywordGroupPojoList(keywordGroupEntityList);

        //实例化 Map
        AccessibilityRuleDataMap map = new AccessibilityRuleDataMap();
        map.setRuleList(rulePojoList);
        map.setTagRefList(tagRefPojoList);
        map.setExtraTransferList(transferPojoList);
        map.setExtraLoanList(extraLoanPojoList);
        map.setKeywordGroupList(keywordGroupPojoList);

        return map;
    }

    /**
     * 导入无障碍规则数据
     *
     * @param data 包含无障碍规则数据的集合
     * @return 数据导入的结果
     */
    @Transaction
    default ImportResult importAccessibilityRuleData(AccessibilityRuleDataMap data) {
        if (data == null) return ImportResult.missed();

        EntityPojoMapper mapper = EntityPojoMapper.INSTANCE;
        List<Throwable> exceptionList = new ArrayList<>();

        //清空旧额外数据
        List<Long> existedTransferIdList = getExistedAccessibilityRuleTransferId();
        deleteExtraTransferById(existedTransferIdList);
        List<Long> existedLoanIdList = getExistedAccessibilityRuleLoanId();
        deleteExtraLoanById(existedLoanIdList);

        //清空旧数据
        clearAccessibilityRule();
        clearAccessibilityRuleTagRef();
        clearAccessibilityRuleKeywordGroup();

        //规则
        try {
            List<AccessibilityRulePojo> rulePojoList = data.getRuleList();
            if (rulePojoList != null && !rulePojoList.isEmpty()) {
                //转账账户数据
                List<AccessibilityRuleEntity> ruleEntityList = mapper.toAccessibilityRuleEntityList(data.getRuleList());
                Map<Long, List<AccessibilityRuleEntity>> transferIdMap = ruleEntityList.stream()
                        .filter(rule -> rule.getTransferId() != null)
                        .collect(Collectors.groupingBy(
                                AccessibilityRuleEntity::getTransferId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraTransferPojo> extraTransferPojoList = data.getExtraTransferList();
                if (extraTransferPojoList != null && !extraTransferPojoList.isEmpty()) {
                    List<ExtraTransferEntity> extraTransferEntityList = mapper.toExtraTransferEntityList(extraTransferPojoList);
                    for (ExtraTransferEntity transfer : extraTransferEntityList) {
                        long originTransferId = transfer.getTransferId();
                        if (hasTransferIdConflict(originTransferId)) {
                            transfer.setTransferId(0);  //设置为0以触发自增
                            long newTransferId = writeExtraTransfer(transfer);
                            List<AccessibilityRuleEntity> accountList = transferIdMap.get(originTransferId);
                            if (accountList != null) {
                                for (AccessibilityRuleEntity rule : accountList) {
                                    rule.setTransferId(newTransferId);
                                }
                            }
                            transferIdMap.remove(originTransferId);
                        } else {
                            writeExtraTransfer(transfer);
                        }
                    }
                }

                //借款记录数据
                Map<Long, List<AccessibilityRuleEntity>> loanIdMap = ruleEntityList.stream()
                        .filter(rule -> rule.getLoanId() != null)
                        .collect(Collectors.groupingBy(
                                AccessibilityRuleEntity::getLoanId,
                                HashMap::new,
                                Collectors.toList()
                        ));
                List<ExtraLoanPojo> extraLoanPojoList = data.getExtraLoanList();
                if (extraLoanPojoList != null && !extraLoanPojoList.isEmpty()) {
                    List<ExtraLoanEntity> extraLoanEntityList = mapper.toExtraLoanEntityList(extraLoanPojoList);
                    for (ExtraLoanEntity loan : extraLoanEntityList) {
                        long originId = loan.getLoanId();
                        if (hasLoanIdConflict(originId)) {
                            loan.setLoanId(0);  //设置为0以触发自增
                            long newId = writeExtraLoan(loan);
                            List<AccessibilityRuleEntity> accountList = loanIdMap.get(originId);
                            if (accountList != null) {
                                for (AccessibilityRuleEntity rule : accountList) {
                                    rule.setLoanId(newId);
                                }
                            }
                            loanIdMap.remove(originId);
                        } else {
                            writeExtraLoan(loan);
                        }
                    }
                }

                //写入无障碍规则数据并修改标识符
                writeAccessibilityRule(ruleEntityList);
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //标签映射
        try {
            List<Long> existedTagIdList = getExistedTagId();
            List<AccessibilityRuleTagRefPojo> tagRefPojoList = data.getTagRefList();
            if (tagRefPojoList != null && !tagRefPojoList.isEmpty()) {
                List<AccessibilityRuleTagRefPojo> filteredTagRefPojoList = tagRefPojoList.stream()
                        .filter(pojo -> existedTagIdList.contains(pojo.getTagId()))
                        .collect(Collectors.toList());  //仅写入存在的标签映射
                writeAccessibilityRuleTagRef(mapper.toAccessibilityRuleTagRefEntityList(filteredTagRefPojoList));
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //关键词组合
        try {
            List<AccessibilityRuleKeywordGroupPojo> keywordGroupPojoList = data.getKeywordGroupList();
            if (keywordGroupPojoList != null && !keywordGroupPojoList.isEmpty()) {
                List<AccessibilityRuleKeywordGroupEntity> keywordGroupEntityList = mapper.toAccessibilityRuleKeywordGroupEntityList(keywordGroupPojoList);
                writeAccessibilityRuleKeywordGroup(keywordGroupEntityList);
            }
        } catch (Exception e) {
            exceptionList.add(e);
        }

        //返回导入结果
        if (exceptionList.isEmpty()) {
            return ImportResult.success();
        } else {
            return ImportResult.partlySuccess(exceptionList);
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
     * 根据编号读取转账账户数据
     *
     * @param idSet 需要读取的转账账户数据的编号
     * @return 转账账户数据列表
     */
    @Query("SELECT * FROM extraTransfer WHERE transferId IN (:idSet)")
    List<ExtraTransferEntity> readExtraTransferById(Set<Long> idSet);

    /**
     * 写入转账账户数据
     *
     * @param entity 转账账户数据
     * @return 插入后的主键
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long writeExtraTransfer(ExtraTransferEntity entity);

    /**
     * 判断现有的表中是否有和指定主键值冲突的主键
     *
     * @param transferId 主键值
     * @return 是否有主键冲突
     */
    @Query("SELECT EXISTS(SELECT 1 FROM extraTransfer WHERE transferId = :transferId)")
    boolean hasTransferIdConflict(long transferId);

    /**
     * 通过转账账户编号删除转账账户数据
     *
     * @param idList 转账账户编号
     */
    @Query("DELETE FROM extratransfer WHERE transferId IN (:idList)")
    void deleteExtraTransferById(List<Long> idList);

    /**
     * 通过编号读取借款记录数据
     *
     * @param idSet 借款记录编号集合
     * @return 借款记录数据
     */
    @Query("SELECT * FROM extraLoan WHERE loanId IN (:idSet)")
    List<ExtraLoanEntity> readExtraLoanById(Set<Long> idSet);

    /**
     * 通过借款记录编号删除借款记录
     *
     * @param idList 借款记录编号
     */
    @Query("DELETE FROM extraLoan WHERE loanId IN (:idList)")
    void deleteExtraLoanById(List<Long> idList);

    /**
     * 判断是否有与指定主键冲突的主键值
     *
     * @param loanId 主键值
     * @return 是否有主键冲突
     */
    @Query("SELECT EXISTS(SELECT 1 FROM extraLoan WHERE loanId = :loanId)")
    boolean hasLoanIdConflict(long loanId);

    /**
     * 写入借款记录数据
     *
     * @param entity 待写入的借款记录数据
     * @return 插入后的主键值
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long writeExtraLoan(ExtraLoanEntity entity);

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
     * 读取通知规则分组
     *
     * @return 通知规则分组数据
     */
    @Query("SELECT * FROM notificationRuleGroups")
    List<NotificationRuleGroupEntity> readNotificationRuleGroup();

    /**
     * 清空通知规则分组
     */
    @Query("DELETE FROM notificationRuleGroups")
    void clearNotificationRuleGroup();

    /**
     * 写入通知规则分组
     *
     * @param entityList 待写入的数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeNotificationRuleGroup(List<NotificationRuleGroupEntity> entityList);

    /**
     * 读取通知规则分组何规则映射的数据
     *
     * @return 通知规则分组
     */
    @Query("SELECT * FROM notificationRuleGroupRef")
    List<NotificationRuleGroupRefEntity> readNotificationRuleGroupRef();

    /**
     * 清空通知规则与其分组的映射关系
     */
    @Query("DELETE FROM notificationRuleGroupRef")
    void clearNotificationRuleGroupRef();

    /**
     * 写入通知规则与其分组的映射关系
     *
     * @param entityList 待写入的映射关系
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void writeNotificationRuleGroupRef(List<NotificationRuleGroupRefEntity> entityList);

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

    /**
     * 获取现存的流水记录的转账账户编号
     *
     * @return 现存的流水记录的转账账户编号
     */
    @Query("SELECT transferId FROM accounts")
    List<Long> getExistedAccountTransferId();

    /**
     * 获取现存的流水记录的借款记录编号
     *
     * @return 现存的流水记录的借款记录编号
     */
    @Query("SELECT loanId FROM accounts")
    List<Long> getExistedAccountLoanId();

    /**
     * 获取现存的通知规则的转账账户编号
     *
     * @return 现存的通知规则的转账账户编号
     */
    @Query("SELECT transferId FROM notificationRules")
    List<Long> getExistedNotificationRuleTransferId();

    /**
     * 获取现存的通知规则的借款记录编号
     *
     * @return 获取现存的通知规则的借款记录编号
     */
    @Query("SELECT loanId FROM notificationRules")
    List<Long> getExistedNotificationRuleLoanId();

    /**
     * 获取现存的无障碍规则的转账账户编号
     *
     * @return 现存的无障碍规则的转账账户编号
     */
    @Query("SELECT transferId FROM accessibilityRules")
    List<Long> getExistedAccessibilityRuleTransferId();

    /**
     * 获取现存的无障碍规则的借款记录编号
     *
     * @return 现存的无障碍规则的借款记录编号
     */
    @Query("SELECT loanId FROM accessibilityRules")
    List<Long> getExistedAccessibilityRuleLoanId();
}
