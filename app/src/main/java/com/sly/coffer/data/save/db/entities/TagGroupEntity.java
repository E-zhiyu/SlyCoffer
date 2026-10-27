package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "tagGroups",
        indices = {
                @Index(value = "groupId"),
                @Index(value = "name", unique = true)
        }
)
public class TagGroupEntity {
    @PrimaryKey(autoGenerate = true)
    private long groupId;
    private String name;

    public TagGroupEntity(String name) {
        this.name = name;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TagGroupEntity that = (TagGroupEntity) o;
        return groupId == that.groupId && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, name);
    }
}
