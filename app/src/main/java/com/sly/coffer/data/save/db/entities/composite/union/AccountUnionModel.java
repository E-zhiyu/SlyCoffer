package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.LoanEntity;
import com.sly.coffer.data.save.db.entities.LoanRepayRefEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class AccountUnionModel {
    @Embedded
    private final AccountEntity account;            //流水记录实体
    @Nullable
    @Relation(
            entity = AccountTransferEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final AccountTransferEntity transfer;   //转账额外数据
    @Nullable
    @Relation(
            entity = LoanEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final LoanEntity loan;                  //借款额外数据
    @Relation(
            parentColumn = "accountId",
            entityColumn = "accountId",
            associateBy = @Junction(
                    value = LoanRepayRefEntity.class,
                    parentColumn = "repayAccountId",
                    entityColumn = "loanAccountId"
            )
    )
    @Nullable
    private final AccountEntity repayRelatedAccount;         //还款额外数据，包含其关联的借款记录
    @Relation(
            parentColumn = "accountId",
            entityColumn = "tagId",
            associateBy = @Junction(AccountTagRefEntity.class)
    )
    private final List<TagEntity> tagList;          //该流水记录的标签列表
    @Relation(
            entity = MediaEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final List<MediaEntity> mediaList;      //该流水记录的媒体列表

    public AccountUnionModel(AccountEntity account, @Nullable AccountTransferEntity transfer, @Nullable LoanEntity loan, @Nullable AccountEntity repayRelatedAccount, List<TagEntity> tagList, List<MediaEntity> mediaList) {
        this.account = account;
        this.transfer = transfer;
        this.loan = loan;
        this.repayRelatedAccount = repayRelatedAccount;
        this.tagList = tagList;
        this.mediaList = mediaList;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<MediaEntity> getMediaList() {
        return mediaList;
    }

    @Nullable
    public AccountTransferEntity getTransfer() {
        return transfer;
    }

    @Nullable
    public LoanEntity getLoan() {
        return loan;
    }

    @Nullable
    public AccountEntity getRepayRelatedAccount() {
        return repayRelatedAccount;
    }
}
