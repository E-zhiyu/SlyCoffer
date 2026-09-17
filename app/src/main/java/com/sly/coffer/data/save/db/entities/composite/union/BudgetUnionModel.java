package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class BudgetUnionModel {
    @Embedded
    private final BudgetEntity budget;
    @Relation(
            parentColumn = "budgetId",
            entityColumn = "tagId",
            associateBy = @Junction(BudgetTagRefEntity.class)
    )
    private final List<TagEntity> tagList;

    public BudgetUnionModel(BudgetEntity budget, List<TagEntity> tagList) {
        this.budget = budget;
        this.tagList = tagList;
    }

    public BudgetEntity getBudget() {
        return budget;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }
}
