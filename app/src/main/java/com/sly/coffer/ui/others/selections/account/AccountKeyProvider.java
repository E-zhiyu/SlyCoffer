package com.sly.coffer.ui.others.selections.account;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.ui.pages.report.AccountSelectListAdapter;

import java.util.List;

public class AccountKeyProvider extends ItemKeyProvider<Long> {
    private final AccountSelectListAdapter adapter;

    public AccountKeyProvider(AccountSelectListAdapter adapter) {
        super(SCOPE_MAPPED);
        this.adapter = adapter;
    }

    @Override
    public Long getKey(int position) {
        AccountUiModel model = adapter.getCurrentList().get(position);
        if (model instanceof AccountUiModel.Item) {
            return ((AccountUiModel.Item) model).model.getAccount().getAccountId();
        } else if (model instanceof AccountUiModel.Separator) {
            return -Math.abs((long) ((AccountUiModel.Separator) model).text.hashCode());
        } else return null;
    }

    @Override
    public int getPosition(@NonNull Long key) {
        List<AccountUiModel> currentList = adapter.getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            AccountUiModel item = currentList.get(i);
            if (key >= 0 &&
                    (item instanceof AccountUiModel.Item) &&
                    ((AccountUiModel.Item) item).model.getAccount().getAccountId() == key
            ) {
                return i;
            } else if (key < 0 &&
                    (item instanceof AccountUiModel.Separator) &&
                    ((AccountUiModel.Separator) item).text.hashCode() == -key
            ) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }
}
