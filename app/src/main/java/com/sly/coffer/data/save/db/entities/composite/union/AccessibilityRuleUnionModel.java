package com.sly.coffer.data.save.db.entities.composite.union;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;

import java.util.List;

public class AccessibilityRuleUnionModel {
    @Embedded
    private final AccessibilityRuleEntity rule;
    @Nullable
    @Relation(
            entity = AccessibilityRuleTransferEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final AccessibilityRuleTransferEntity transfer;
    @Relation(
            parentColumn = "ruleId",
            entityColumn = "tagId",
            associateBy = @Junction(AccessibilityRuleTagRefEntity.class)
    )
    private final List<TagEntity> tagList;
    @Relation(
            entity = AccessibilityRuleKeywordGroupEntity.class,
            parentColumn = "ruleId",
            entityColumn = "ruleId"
    )
    private final List<AccessibilityRuleKeywordGroupEntity> keywordGroupList;

    public AccessibilityRuleUnionModel(AccessibilityRuleEntity rule, @Nullable AccessibilityRuleTransferEntity transfer, List<TagEntity> tagList, List<AccessibilityRuleKeywordGroupEntity> keywordGroupList) {
        this.rule = rule;
        this.transfer = transfer;
        this.tagList = tagList;
        this.keywordGroupList = keywordGroupList;
    }

    public AccessibilityRuleEntity getRule() {
        return rule;
    }

    @Nullable
    public AccessibilityRuleTransferEntity getTransfer() {
        return transfer;
    }

    public List<TagEntity> getTagList() {
        return tagList;
    }

    public List<AccessibilityRuleKeywordGroupEntity> getKeywordGroupList() {
        return keywordGroupList;
    }
}
