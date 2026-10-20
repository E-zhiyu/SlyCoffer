package com.sly.coffer.data.save.db.daos;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface ExtraDataDao {
    /**
     * 插入转账账户额外数据
     *
     * @param transfer 转账账户额外数据
     * @return 新插入的数据的主键值
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Single<Long> insertExtraTransferSingle(ExtraTransferEntity transfer);

    /**
     * 插入借款额外数据
     *
     * @param loan 借款额外数据
     * @return 插入的数据的主键值
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Single<Long> insertExtraLoanSingle(ExtraLoanEntity loan);

    /**
     * 通过转账账户编号删除转账账户数据
     *
     * @param transferId 转账账户编号
     */
    @Query("DELETE FROM extraTransfer WHERE transferId = :transferId")
    void deleteExtraTransferById(long transferId);
//TODO:将找不到这几个方法的位置全部修改
    /**
     * 通过借款记录编号删除借款记录
     *
     * @param loanId 借款记录编号
     */
    @Query("DELETE FROM extraLoan WHERE loanId = :loanId")
    void deleteExtraLoanById(long loanId);

    /**
     * 获取借款对象名称
     *
     * @return 借款对象名称列表
     */
    @Query("SELECT DISTINCT loanTargetName FROM extraLoan WHERE loanTargetName IS NOT NULL")
    Single<List<String>> getLoanTargetNameSingle();
}
