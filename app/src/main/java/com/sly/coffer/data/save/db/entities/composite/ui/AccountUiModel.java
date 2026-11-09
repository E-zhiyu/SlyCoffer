package com.sly.coffer.data.save.db.entities.composite.ui;

import com.sly.coffer.data.save.db.entities.composite.union.AccountListUnionModel;

public class AccountUiModel {
    public static class Item extends AccountUiModel {
        public final AccountListUnionModel model;

        public Item(AccountListUnionModel model) {
            this.model = model;
        }
    }

    public static class Separator extends AccountUiModel {
        public final String text;

        public Separator(String text) {
            this.text = text;
        }
    }
}
