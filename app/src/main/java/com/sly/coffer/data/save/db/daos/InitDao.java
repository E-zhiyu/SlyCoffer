package com.sly.coffer.data.save.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;

@Dao
public interface InitDao {
    /**
     * 插入转账账户额外数据
     *
     * @param transfer 转账账户额外数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertExtraTransfer(ExtraTransferEntity transfer);

    /**
     * 插入借款额外数据
     *
     * @param loan 借款额外数据
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertExtraLoan(ExtraLoanEntity loan);

    /**
     * 插入标签分组
     *
     * @param entity 标签分组实例
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTagGroup(TagGroupEntity entity);
}
