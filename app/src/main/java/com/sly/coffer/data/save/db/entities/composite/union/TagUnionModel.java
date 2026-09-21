package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;

public class TagUnionModel {
    @Embedded
    private final TagEntity tag;

    @Relation(
            entity = TagGroupEntity.class,
            parentColumn = "groupId",
            entityColumn = "groupId"
    )
    private final TagGroupEntity group;

    public TagUnionModel(TagEntity tag, TagGroupEntity group) {
        this.tag = tag;
        this.group = group;
    }

    public TagEntity getTag() {
        return tag;
    }

    public TagGroupEntity getGroup() {
        return group;
    }
}
