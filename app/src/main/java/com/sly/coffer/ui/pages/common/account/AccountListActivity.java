package com.sly.coffer.ui.pages.common.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.databinding.ActivityAccountListBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderItemDecoration;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountListAdapter;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AccountListActivity extends AppCompatActivity {
    private ActivityAccountListBinding binding;
    @Nullable
    private Bundle initBundle;
    private final CompositeDisposable disposable = new CompositeDisposable();  //订阅列表（便于取消订阅）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initBundle = getIntent().getExtras();
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disposable.dispose();
        binding = null;
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        //流水列表
        AccountListAdapter adapter = new AccountListAdapter(
                (entity, anchor) -> {
                    long accountId = entity.getAccountId();
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.RUNNING_ID.v(), accountId);

                    Intent skip2AccountInput = new Intent(this, RunningAccountInputActivity.class);
                    skip2AccountInput.putExtras(bundle);
                    startActivity(skip2AccountInput);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_account_list_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.action_delete_account) {
                            deleteAccount(entity);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                }
        );
        binding.recycler.setAdapter(adapter);

        //加载流水列表
        List<Long> accountIdList = initBundle == null ? new ArrayList<>() :
                Arrays.stream(Objects.requireNonNull(initBundle.getLongArray(KeyStrings.RUNNING_ID.v())))
                        .boxed()
                        .collect(Collectors.toList());
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(AccountService.getRunningAccountFlowableById(accountIdList, db)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        uiModelList -> {
                            int visibility = uiModelList.isEmpty() ? View.VISIBLE : View.GONE;
                            binding.emptyText.setVisibility(visibility);

                            adapter.submitList(uiModelList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
        StickyHeaderItemDecoration<ViewHolderSeparatorTextChipBinding> decoration = new StickyHeaderItemDecoration<>(
                adapter,
                ViewHolderSeparatorTextChipBinding::inflate,
                (binding1, data) -> binding1.separatorText.setText(data)
        );
        binding.recycler.addItemDecoration(decoration);
    }

    /**
     * 删除流水记录
     *
     * @param account 需要删除的流水记录
     */
    private void deleteAccount(AccountEntity account) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_account)
                .setMessage("确认删除该流水记录吗？其包含的媒体文件也会一并删除")
                .setPositiveButton("确定", (dialogInterface, i) ->
                        disposable.add(AccountService.deleteAccount(account, this)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Toast.makeText(this, "流水记录已删除", Toast.LENGTH_SHORT).show(),
                                        e -> ExceptionHelper.showExceptionDialog(this, e)
                                )
                        )
                )
                .setNegativeButton("取消", null)
                .show();
    }
}