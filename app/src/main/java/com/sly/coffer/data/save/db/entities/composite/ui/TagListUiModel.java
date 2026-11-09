package com.sly.coffer.data.save.db.entities.composite.ui;

import com.sly.coffer.data.save.db.entities.TagGroupEntity;
import com.sly.coffer.data.save.db.entities.composite.union.TagUseCountUnionModel;

public class TagListUiModel {
    public static class Item extends TagListUiModel {
        public final TagUseCountUnionModel model;

        public Item(TagUseCountUnionModel model) {
            this.model = model;
        }
    }

    public static class Group extends TagListUiModel {
        public final TagGroupEntity group;

        public Group(TagGroupEntity group) {
            this.group = group;
        }
    }
}
