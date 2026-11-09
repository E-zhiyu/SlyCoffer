package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.LocationEntity;

public class AccountListUnionModel {
    @Embedded
    private final AccountEntity account;                //流水记录实体
    @Nullable
    @Relation(
            parentColumn = "accountId",
            entityColumn = "accountId",
            entity = LocationEntity.class
    )
    private final LocationEntity location;              //流水记录位置信息

    public AccountListUnionModel(AccountEntity account, @Nullable LocationEntity location) {
        this.account = account;
        this.location = location;
    }

    public AccountEntity getAccount() {
        return account;
    }

    @Nullable
    public LocationEntity getLocation() {
        return location;
    }
}
