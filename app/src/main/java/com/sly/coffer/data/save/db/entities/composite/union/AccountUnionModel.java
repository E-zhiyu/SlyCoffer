package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class AccountUnionModel {
    @Embedded
    private final AccountEntity account;
    @Relation(
            entity = AccountTransferEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final AccountTransferEntity transfer;
    @Relation(
            parentColumn = "accountId",
            entityColumn = "tagId",
            associateBy = @Junction(AccountTagRefEntity.class)
    )
    private final List<TagEntity> tagList;
    @Relation(
            entity = MediaEntity.class,
            parentColumn = "accountId",
            entityColumn = "accountId"
    )
    private final List<MediaEntity> mediaList;

    public AccountUnionModel(AccountEntity account, AccountTransferEntity transfer, List<TagEntity> tagList, List<MediaEntity> mediaList) {
        this.account = account;
        this.transfer = transfer;
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

    public AccountTransferEntity getTransfer() {
        return transfer;
    }
}
