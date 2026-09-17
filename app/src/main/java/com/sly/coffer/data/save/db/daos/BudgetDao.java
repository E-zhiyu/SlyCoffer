package com.sly.coffer.data.save.db.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.composite.union.BudgetUnionModel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface BudgetDao {
    /**
     * 获取预算数量
     *
     * @return 预算数量，支持响应式更新
     */
    @Query("SELECT COUNT(*) FROM budgets")
    Flowable<Integer> getBudgetCountFlowable();

    /**
     * 获取所有预算，并按照起算日期倒序排序
     *
     * @return 由所有预算组成的列表，支持响应式更新
     */
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    Flowable<List<BudgetEntity>> getAllBudgetFlowable();

    /**
     * 获取所有预算数据
     *
     * @return 所有预算数据组成的列表
     */
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    Single<List<BudgetEntity>> getAllBudgetSingle();

    /**
     * 删除预算
     *
     * @param budget 需要删除的预算
     */
    @Delete
    void deleteBudget(BudgetEntity budget);

    /**
     * 通过预算 ID 重置预算
     *
     * @param budgetId    需要重置的预算的 ID
     * @param currentDate 当前日期
     * @return 是否完成
     */
    @Query("UPDATE budgets SET balance = initAmount, startDate = :currentDate WHERE budgetId = :budgetId")
    Completable resetBudgetById(long budgetId, LocalDate currentDate);

    /**
     * 通过预算 ID 重置预算
     *
     * @param budgetIdList 需要重置的预算的 ID
     * @param currentDate  当前日期
     * @return 是否完成
     */
    @Query("UPDATE budgets SET balance = initAmount, startDate = :currentDate WHERE budgetId IN (:budgetIdList)")
    Completable resetBudgetById(List<Long> budgetIdList, LocalDate currentDate);

    /**
     * 通过预算 ID 获取预算数据
     *
     * @param budgetId 需要获取数据的预算 ID
     * @return 获取到的预算详情数据
     */
    @Transaction
    @Query("SELECT * FROM budgets WHERE budgetId = :budgetId")
    Single<Optional<BudgetUnionModel>> getBudgetWithDetailById(long budgetId);

    /**
     * 插入预算和标签的映射关系数据
     *
     * @param refList 映射关系数据列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBudgetTagRef(List<BudgetTagRefEntity> refList);

    /**
     * 插入预算
     *
     * @param budget 需要插入的预算
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insertBudget(BudgetEntity budget);

    /**
     * 添加预算的事务
     *
     * @param budget    需要添加的预算
     * @param tagIdList 与该预算绑定的标签的 ID 列表
     */
    @Transaction
    default void addBudget(BudgetEntity budget, List<Long> tagIdList) {
        if (budget == null) return;
        budget.setBalance(budget.getInitAmount());   //将余额重置为初始值
        long budgetId = insertBudget(budget);

        List<BudgetTagRefEntity> refList = tagIdList.stream()
                .map(id -> new BudgetTagRefEntity(budgetId, id))
                .collect(Collectors.toList());
        insertBudgetTagRef(refList);
    }

    /**
     * 通过预算 ID 删除预算和标签的映射关系
     *
     * @param budgetId 需要删除映射关系的预算的 ID
     */
    @Query("DELETE FROM budgettagref WHERE budgetId = :budgetId")
    void deleteBudgetTagRefByBudgetId(long budgetId);

    /**
     * 更新预算
     *
     * @param budget 更新后的预算数据
     */
    @Update
    void updateBudget(BudgetEntity budget);

    /**
     * 修改预算的事务
     *
     * @param budget    修改后的预算
     * @param tagIdList 与该预算绑定的标签 ID
     */
    @Transaction
    default void modifyBudget(BudgetEntity budget, List<Long> tagIdList) {
        if (budget == null) return;
        long budgetId = budget.getBudgetId();
        updateBudget(budget);

        deleteBudgetTagRefByBudgetId(budgetId);
        List<BudgetTagRefEntity> refList = tagIdList.stream()
                .map(id -> new BudgetTagRefEntity(budgetId, id))
                .collect(Collectors.toList());
        insertBudgetTagRef(refList);
    }

    /**
     * 获取余额低的预算
     *
     * @return 低余额预算列表
     */
    @Query("SELECT * FROM budgets WHERE balance <= initAmount * lowBalanceRatio / 100")
    List<BudgetEntity> getLowBalanceBudget();
}
