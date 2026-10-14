package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.LoanRepayRefEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class AccountUnionModel {
    @Embedded
    private final AccountEntity account;                //流水记录实体
    @Nullable
    @Relation(
            entity = ExtraTransferEntity.class,
            parentColumn = "transferId",
            entityColumn = "transferId"
    )
    private final ExtraTransferEntity transfer;         //转账额外数据
    @Nullable
    @Relation(
            entity = ExtraLoanEntity.class,
            parentColumn = "loanId",
            entityColumn = "loanId"
    )
    private final ExtraLoanEntity loan;                 //借款额外数据
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
    private final AccountEntity repayRelatedAccount;    //还款额外数据，包含其关联的借款记录
    @Relation(
            parentColumn = "accountId",
            entityColumn = "tagId",
            associateBy = @Junction(AccountTagRefEntity.class)
    )
    private final List<TagEntity> tagList;              //该流水记录的标签列表
    @Relation(
            entity = MediaEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final List<MediaEntity> mediaList;          //该流水记录的媒体列表

    public AccountUnionModel(AccountEntity account, @Nullable ExtraTransferEntity transfer, @Nullable ExtraLoanEntity loan, @Nullable AccountEntity repayRelatedAccount, List<TagEntity> tagList, List<MediaEntity> mediaList) {
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
    public AccountEntity getRepayRelatedAccount() {
        return repayRelatedAccount;
    }

    @Nullable
    public ExtraTransferEntity getTransfer() {
        return transfer;
    }

    @Nullable
    public ExtraLoanEntity getLoan() {
        return loan;
    }
}
