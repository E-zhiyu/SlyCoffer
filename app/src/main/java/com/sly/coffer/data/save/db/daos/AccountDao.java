package com.sly.coffer.data.save.db.daos;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.composite.AccountWithDetailModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AccountDao {
    /**
     * 获取在指定日期后的流水记录和日期分隔符数量
     *
     * @param date 指定的日期
     * @return 在指定日期后的流水记录和日期分隔符数量
     */
    @Query("SELECT COUNT(*) + COUNT(DISTINCT DATE(dateTime / 1000, 'unixepoch'))" +
            "FROM accounts WHERE dateTime > :date")
    Single<Integer> getAccountCountAfterDateSingle(LocalDate date);

    /**
     * 获取最早的记账日期
     *
     * @return 最早的记账日期
     */
    @Query("SELECT dateTime FROM accounts ORDER BY dateTime ASC LIMIT 1")
    Flowable<Optional<LocalDate>> getEarliestDateFlowable();

    /**
     * 获取在日期范围内的流水记录
     *
     * @param start 起始日期（包含）
     * @param end   结束日期（不包含）
     * @return 在日期范围内的流水记录
     */
    @Query("SELECT * FROM accounts WHERE dateTime >= :start AND dateTime < :end")
    Flowable<List<AccountEntity>> getAccountInDateRange(LocalDate start, LocalDate end);

    /**
     * 获取符合过滤条件的流水记录
     *
     * @param query 数据库查询实例
     * @return 符合过滤条件的流水记录列表，支持响应式更新
     */
    @RawQuery(observedEntities = {AccountEntity.class, AccountTagRefEntity.class})
    Flowable<List<AccountEntity>> getAccountWithFilter(SupportSQLiteQuery query);

    /**
     * 根据流水 ID 获取带有标签的流水记录
     *
     * @param accountId 需要获取的流水记录的 ID
     * @return 带有标签的流水记录
     */
    @Transaction
    @Query("SELECT * FROM accounts WHERE accountId = :accountId")
    Single<Optional<AccountWithDetailModel>> getAccountWithDetailSingleById(long accountId);

    /**
     * 通过日期区间获取流水记录的 ID
     *
     * @param start 起始日期（包含）
     * @param end   结束日期（不包含）
     * @return 在日期范围内的流水记录 ID
     */
    @Query("SELECT accountId FROM accounts WHERE dateTime >= :start AND dateTime < :end")
    Flowable<List<Long>> getAccountIdFlowableByDateRange(LocalDate start, LocalDate end);

    /**
     * 通过日期区间获取流水记录
     *
     * @param start 起始日期（包含）
     * @param end   结束日期（不包含）
     * @return 在日期范围内的流水记录
     */
    @Transaction
    @Query("SELECT * FROM accounts WHERE dateTime >= :start AND dateTime < :end")
    Flowable<List<AccountWithDetailModel>> getAccountWithDetailFlowableByDateRange(LocalDate start, LocalDate end);

    /**
     * 通过流水记录的 ID 获取流水记录数据
     *
     * @param ids 需要获取的流水记录的 ID 集合
     * @return ID 在集合中的流水记录
     */
    @Transaction
    @Query("SELECT * FROM accounts WHERE accountId IN (:ids)")
    Flowable<List<AccountWithDetailModel>> getAccountWithDetailFlowableById(Set<Long> ids);

    /**
     * 获取数据库中储存的转出和转入账户
     *
     * @return 包含所有转出和转入账户的列表
     */
    @Query("SELECT exportAccount FROM accountTransfers WHERE exportAccount IS NOT NULL " +
            "UNION " +
            "SELECT importAccount FROM accountTransfers WHERE importAccount IS NOT NULL " +
            "UNION " +
            "SELECT exportAccount FROM notificationRuleTransfers WHERE exportAccount IS NOT NULL " +
            "UNION " +
            "SELECT importAccount FROM notificationRuleTransfers WHERE importAccount IS NOT NULL " +
            "ORDER BY 1 ASC")
    Single<List<String>> getTransferAccountsSingle();

    /**
     * 插入新的流水记录
     *
     * @param account 流水记录实例
     * @return 新添加的流水记录分配的主键值
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insertAccount(AccountEntity account);

    /**
     * 插入流水记录的转账账户数据
     *
     * @param entity 转账账户数据实体
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAccountTransfer(AccountTransferEntity entity);

    /**
     * 插入新的流水记录与标签映射关系
     *
     * @param refList 映射关系列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAccountTagRef(List<AccountTagRefEntity> refList);

    /**
     * 插入新媒体文件
     *
     * @param mediaList 新媒体文件列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMedia(List<MediaEntity> mediaList);

    /**
     * 通过流水 ID 获取绑定的标签
     *
     * @param accountId 流水 ID
     * @return 该流水记录绑定的标签的编号列表
     */
    @Query("SELECT tagId FROM accountTagRef WHERE accountId = :accountId")
    List<Long> getTagIdListByAccountId(long accountId);

    /**
     * 通过流水编号获取流水日期和时间
     *
     * @param accountId 需要获取日期和时间的流水编号
     * @return 流水日期和时间
     */
    @Query("SELECT dateTime FROM accounts WHERE accountId = :accountId")
    LocalDateTime getAccountDateTimeById(long accountId);

    /**
     * 通过标签编号修改预算余额
     *
     * @param increase        预算余额增加的量
     * @param tagIdList       需要更新的预算对应的标签编号的列表
     * @param accountDateTime 流水记录的日期和时间
     */
    @Query("UPDATE budgets SET balance = balance + :increase " +
            "WHERE startDate <= :accountDateTime AND " +
            "budgetId NOT IN (" +
            "   SELECT budgetId FROM budgetTagRef WHERE tagId NOT IN (:tagIdList)" +
            ")")
    void updateBudgetBalanceByTagId(double increase, List<Long> tagIdList, LocalDateTime accountDateTime);

    /**
     * 将预算余额限制在初始金额 ~ 0的范围内
     *
     * @param tagIdList       需要限制余额的预算所绑定的标签编号列表
     * @param accountDateTime 流水记录的日期和时间
     */
    @Query("UPDATE budgets SET balance = MAX(0, MIN(initAmount, balance)) " +
            "WHERE startDate <= :accountDateTime AND " +
            "budgetId NOT IN (" +
            "   SELECT budgetId FROM budgetTagRef WHERE tagId NOT IN (:tagIdList)" +
            ")")
    void limitBudgetBalanceByTagId(List<Long> tagIdList, LocalDateTime accountDateTime);

    /**
     * 流水记录添加事务
     *
     * @param account         新流水记录实体
     * @param transfer        转账账户数据（仅当记录类型为转账时会写入）
     * @param mediaEntityList 附带的位于永久目录下的媒体文件实体列表
     * @param tagIdList       与该记录绑定的标签的 ID 列表
     */
    @Transaction
    default long addAccount(AccountEntity account, AccountTransferEntity transfer, @Nullable List<MediaEntity> mediaEntityList, @Nullable List<Long> tagIdList) {
        long accountId = insertAccount(account);

        //写入转账账户数据
        if (account.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setAccountId(accountId);
            insertAccountTransfer(transfer);
        }

        //生成媒体实体列表并写入数据
        if (mediaEntityList != null) {
            List<MediaEntity> availableMediaList = mediaEntityList.stream() //赋予流水记录编号
                    .peek(mediaEntity -> mediaEntity.setAccountId(accountId))
                    .collect(Collectors.toList());
            insertMedia(availableMediaList);
        }

        //生成标签映射列表并写入数据
        if (tagIdList != null) {
            List<AccountTagRefEntity> tagRefEntityList = tagIdList.stream()
                    .map(id -> new AccountTagRefEntity(accountId, id))
                    .collect(Collectors.toList());
            insertAccountTagRef(tagRefEntityList);
        }

        //更新预算余额
        updateBudgetBalanceByTagId(-account.getAmount(), tagIdList, account.getDateTime());
        limitBudgetBalanceByTagId(tagIdList, account.getDateTime());

        return accountId;
    }

    /**
     * 通过流水编号删除转账账户记录
     *
     * @param accountId 需要删除转账账户记录的流水编号
     */
    @Query("DELETE FROM accountTransfers WHERE accountId = :accountId")
    void deleteAccountTransferByAccountId(long accountId);

    /**
     * 通过流水编号删除与标签的映射关系
     *
     * @param accountId 待删除的映射关系的流水记录 ID
     */
    @Query("DELETE FROM accountTagRef WHERE accountId = :accountId")
    void deleteAccountTagRefByAccountId(long accountId);

    /**
     * 通过流水记录编号获取媒体文件 Uri
     *
     * @param accountId 流水记录 ID
     * @return 该流水记录的媒体文件的 Uri
     */
    @Query("SELECT fileUri FROM medias WHERE accountId = :accountId")
    List<Uri> getMediaUriByAccountId(long accountId);

    /**
     * 通过流水记录编号删除媒体记录
     *
     * @param accountId 流水记录编号
     */
    @Query("DELETE FROM medias WHERE accountId = :accountId")
    void deleteMediaByAccountId(long accountId);

    /**
     * 更新流水记录
     *
     * @param account 新流水记录数据
     */
    @Update
    void updateAccount(AccountEntity account);

    /**
     * 修改流水记录的事务
     *
     * @param account         修改后的流水数据
     * @param transfer        转账账户数据（仅当记录类型为转账时会写入）
     * @param mediaEntityList 位于永久目录下的媒体文件实体列表，可能包含主键值为0的媒体实体
     * @param tagIdList       修改后的标签列表
     */
    @Transaction
    default Set<Uri> modifyAccount(
            AccountEntity account,
            AccountTransferEntity transfer,
            List<MediaEntity> mediaEntityList,
            List<Long> tagIdList
    ) {
        if (account == null) return null;
        long accountId = account.getAccountId();

        //获取在数据库中的媒体文件 Uri，并计算需要删除的媒体文件的 Uri
        Set<Uri> oldMediaUriSet = new HashSet<>(getMediaUriByAccountId(accountId));
        Set<Uri> newMediaUriSet = mediaEntityList.stream()
                .map(MediaEntity::getFileUri)
                .collect(Collectors.toSet());
        oldMediaUriSet.removeAll(newMediaUriSet);

        //更新流水记录
        LocalDateTime oldDateTime = getAccountDateTimeById(accountId);  //获取原来的日期和时间
        updateAccount(account);

        //更新转账账户数据
        deleteAccountTransferByAccountId(accountId);
        if (account.getType() == AccountType.TRANSFER.ordinal()) {
            transfer.setAccountId(accountId);
            insertAccountTransfer(transfer);
        }

        //更新标签
        List<Long> oldTagIdList = getTagIdListByAccountId(accountId);   //获取原来的标签绑定关系
        deleteAccountTagRefByAccountId(accountId);
        List<AccountTagRefEntity> tagRefEntityList = tagIdList.stream()
                .map(id -> new AccountTagRefEntity(accountId, id))
                .collect(Collectors.toList());
        insertAccountTagRef(tagRefEntityList);

        //更新媒体
        deleteMediaByAccountId(accountId);
        insertMedia(mediaEntityList);

        //更新预算余额
        double amount = account.getAmount();
        LocalDateTime dateTime = account.getDateTime();
        updateBudgetBalanceByTagId(amount, oldTagIdList, oldDateTime);
        updateBudgetBalanceByTagId(-amount, tagIdList, dateTime);
        limitBudgetBalanceByTagId(oldTagIdList, oldDateTime);
        limitBudgetBalanceByTagId(tagIdList, dateTime);

        return oldMediaUriSet;
    }

    /**
     * 从数据库中删除流水记录
     *
     * @param account 需要删除的流水记录
     */
    @Delete
    void deleteAccount(AccountEntity account);

    /**
     * 删除流水记录的事务
     *
     * @param account 需要删除的流水记录
     * @return 需要删除的媒体文件的 Uri
     */
    default Set<Uri> removeAccount(AccountEntity account) {
        if (account == null) return null;

        //获取媒体数据
        Set<Uri> uriSet = new HashSet<>(getMediaUriByAccountId(account.getAccountId()));

        //更新预算
        LocalDateTime oldDateTime = getAccountDateTimeById(account.getAccountId());
        List<Long> oldTagIdList = getTagIdListByAccountId(account.getAccountId());
        updateBudgetBalanceByTagId(account.getAmount(), oldTagIdList, oldDateTime);
        limitBudgetBalanceByTagId(oldTagIdList, oldDateTime);

        //删除流水记录
        deleteAccount(account);

        return uriSet;
    }
}
