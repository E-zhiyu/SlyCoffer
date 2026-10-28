package com.sly.coffer.data.save.db.entities;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "tags",
        foreignKeys = @ForeignKey(
                entity = TagGroupEntity.class,
                parentColumns = "groupId",
                childColumns = "groupId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index(value = "tagId"),
                @Index(value = "scope"),
                @Index(value = "groupId"),
                @Index(value = "name", unique = true)
        }
)
public class TagEntity {
    @PrimaryKey(autoGenerate = true)
    private long tagId;     //主键
    private String name;    //名称
    @ColumnInfo(defaultValue = "0")
    private int scope;      //作用域
    @Nullable
    private Long groupId;   //所属分组的编号

    public TagEntity(String name, int scope) {
        this.name = name;
        this.scope = scope;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    @Nullable
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(@Nullable Long groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TagEntity tagEntity = (TagEntity) o;
        return tagId == tagEntity.tagId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tagId);
    }
}
