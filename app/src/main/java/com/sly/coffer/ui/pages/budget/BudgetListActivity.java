package com.sly.coffer.ui.pages.budget;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.services.BudgetService;
import com.sly.coffer.databinding.ActivityBudgetListBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.time.LocalDate;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BudgetListActivity extends AppCompatActivity {
    private ActivityBudgetListBinding binding;              //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityBudgetListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //设置界面边距以防内容被小白条遮挡
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        addPermissionRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
        binding = null;
    }

    /**
     * 申请一些必要的权限
     */
    private void addPermissionRequests() {
        PermissionHelper permissionHelper = new PermissionHelper(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionHelper.addPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "通知权限：预算不足时发送提醒"
            );
        }
        permissionHelper.addPermission(
                PermissionHelper.SpecialPermissionType.ALARM,
                "精确闹钟权限",
                "自动重置预算需要精确闹钟权限，以确保每天0点能够检查并重置预算"
        );
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        //浮动添加按钮
        binding.addFab.setOnClickListener(v -> {
            Intent skip2BudgetInput = new Intent(this, BudgetInputActivity.class);
            startActivity(skip2BudgetInput);
        });
        AppearanceHelper.attachMorphAnimation(binding.addFab);
        AppearanceHelper.setMarginToNavigation(binding.addFab, this);

        //功能说明按钮
        binding.helpBtn.setOnClickListener(view -> {
            final String EXPLANATION = "### 1. 工作原理\n" +
                    "当您添加/修改/删除流水记录时，APP会：\n" +
                    "\n" +
                    "1. 获取添加/修改/删除的流水记录的`流水标签`；\n" +
                    "2. 根据流水记录的`流水标签`和`时间`获取受影响的预算数据；\n" +
                    "3. 根据`流水类型`和`金额`自动更新受影响的预算的`余额`；\n" +
                    "4. 检查预算`余额`，若低于阈值则发送提醒通知。\n" +
                    "\n" +
                    "整个过程完全自动化，您只需使用`流水标签`标记流水记录。\n" +
                    "\n" +
                    "### 2. 预算重置\n" +
                    "\n" +
                    "- 预算重置可以将`余额`设置为初始值，并将`起算日期`设置为“**今天**”；\n" +
                    "- 长按预算后弹出菜单，点击菜单中的“重置”可以手动触发预算重置；\n" +
                    "- 若预算重置频率没有设置为“**永不**”，则该预算会在间隔了相应的天数后自动触发重置。\n" +
                    "> 例：某预算`起算日期`为2026-07-03，重置频率为“每星期”，则该预算会在2026-07-10早上0点重置。\n" +
                    "### 3. 起算日期\n" +
                    "\n" +
                    "- 只有时间晚于或等于`起算日期`的流水记录才会更新预算`余额`；\n" +
                    "> 例：某预算的`起算日期`为2026-07-03，只有流水记录的`时间`至少为2026-07-03 00:00且其他条件符合要求才会更新预算`余额`。\n" +
                    "- 修改流水记录时，APP底层会按照原来的流水记录时间撤销该流水记录对预算的影响，再按照新的流水记录时间应用其对预算的影响。\n" +
                    "> 例：有一条流水记录，其`金额`为10，`时间`为2026-07-03，`种类`为“支出”。有一个预算，其`初始金额`为30，`余额`为25，`起算日期`为2026-07-03。" +
                    "假设修改前后标签永远匹配，将流水记录的`时间`修改为2026-07-01后，预算`余额`变化过程为：25→35→35，最后由于`余额`超过`初始金额`而被修正为30\n" +
                    "### 4. 流水标签\n" +
                    "\n" +
                    "预算中的`流水标签`用于实现预算`余额`自动更新，仅当流水记录的流水标签与预算的流水标签匹配，且其他条件均符合要求时，预算`余额`才能自动更新。\n" +
                    "> 例：某预算的标签中包含“早中晚餐”，当新增一条标记了“早中晚餐”的流水记录时，若其他条件都符合要求，预算`余额`才能自动更新。\n";
            new MarkdownDialogBuilder(this, "功能介绍", EXPLANATION)
                    .setNegativeButton("关闭", null)
                    .show();
        });

        //列表
        BudgetListAdapter adapter = new BudgetListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.BUDGET_ID.v(), entity.getBudgetId());

                    Intent skip2BudgetInput = new Intent(this, BudgetInputActivity.class);
                    skip2BudgetInput.putExtras(bundle);
                    startActivity(skip2BudgetInput);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_budget_list_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_delete_budget) {
                            deleteBudget(entity);
                            return true;
                        } else if (id == R.id.action_reset_budget) {
                            resetBudget(entity);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                }
        );
        binding.recycler.setAdapter(adapter);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.budgetDao().getAllBudgetFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        budgetList -> {
                            if (budgetList.isEmpty()) {
                                binding.emptyText.setVisibility(View.VISIBLE);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(budgetList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }

    /**
     * 删除预算
     *
     * @param budget 需要删除的预算
     */
    private void deleteBudget(@NonNull BudgetEntity budget) {
        String message = String.format(
                Locale.getDefault(),
                "确认删除“%s”吗？",
                budget.getName()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_budget)
                .setMessage(message)
                .setPositiveButton("确定", (dialogInterface, i) ->
                        disposable.add(BudgetService.deleteBudget(budget, this)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Toast.makeText(this, "预算删除成功", Toast.LENGTH_SHORT).show(),
                                        e -> ExceptionHelper.showExceptionDialog(this, e)
                                )
                        )
                )
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 重置预算
     *
     * @param budget 需要重置的预算
     */
    private void resetBudget(@NonNull BudgetEntity budget) {
        String message = String.format(
                Locale.getDefault(),
                "此操作会将余额重置为初始金额，并将起算日期设置为当前日期。确认重置“%s”吗？",
                budget.getName()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_budget)
                .setMessage(message)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.budgetDao().resetBudgetById(budget.getBudgetId(), LocalDate.now())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Toast.makeText(this, "预算重置成功", Toast.LENGTH_SHORT).show(),
                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                            )
                    );
                })
                .setNegativeButton("取消", null)
                .show();
    }
}