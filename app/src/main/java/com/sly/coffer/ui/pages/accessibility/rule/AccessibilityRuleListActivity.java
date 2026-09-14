package com.sly.coffer.ui.pages.accessibility.rule;

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
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.databinding.ActivityAccessibilityRuleListBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AccessibilityRuleListActivity extends AppCompatActivity {
    private ActivityAccessibilityRuleListBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccessibilityRuleListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
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
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        //添加按钮
        binding.addFab.setOnClickListener(view -> {
            Intent intent = new Intent(this, AccessibilityRuleInputActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.addFab);
        AppearanceHelper.setMarginToNavigation(binding.addFab, this);

        //功能说明按钮
        binding.helpBtn.setOnClickListener(view -> {
            final String EXPLANATION = "### 1. 工作原理\n" +
                    "开启无障碍自动记账时，本APP会：\n" +
                    "\n" +
                    "1. 识别当前屏幕的内容；\n" +
                    "2. 根据已保存的规则尝试定位金额视图；\n" +
                    "3. 提取视图中的数字作为金额，生成流水记录并发送通知反馈；\n" +
                    "\n" +
                    "### 2. 使用建议\n" +
                    "\n" +
                    "- 开启自启动权限，允许APP在手机在后台自动运行；\n" +
                    "- 电池优化设为“无限制”，避免系统休眠时关闭APP的通知监听服务；\n" +
                    "- 在最近任务列表中锁定APP，防止一键清理后台时被误杀，并提升后台保活优先级。\n" +
                    "\n" +
                    "> 此功能对性能影响较小，仅在目标应用中尝试解析屏幕内容。\n" +
                    "\n" +
                    "### 3. 免责声明\n" +
                    "- 本功能**完全在本地运行**，无障碍服务仅用于金额提取和记账；\n" +
                    "- **APP不会以任何形式收集、存储或上传您的屏幕内容、金额信息或任何个人数据**；\n" +
                    "- 您创建的所有规则仅保存在本机，请您放心使用。";
            new MarkdownDialogBuilder(this, "功能介绍", EXPLANATION)
                    .setNegativeButton("关闭", null)
                    .show();
        });

        //RecyclerView
        AccessibilityRuleListAdapter adapter = new AccessibilityRuleListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.ACCESSIBILITY_RULE_ID.v(), entity.getRuleId());

                    Intent skip2RuleInput = new Intent(this, AccessibilityRuleInputActivity.class);
                    skip2RuleInput.putExtras(bundle);
                    startActivity(skip2RuleInput);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_accessibility_rule_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_delete_accessibility_rule) {
                            deleteRule(entity);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                },
                (entity, finalStat, anchor) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.accessibilityRuleDao().setRuleEnabled(finalStat, entity.getRuleId())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> {
                                        String tip = String.format(
                                                Locale.getDefault(),
                                                "%s“%s”",
                                                finalStat ? "已启用" : "已禁用",
                                                entity.getName()
                                        );
                                        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
                                    },
                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                            )
                    );
                }
        );
        binding.recycler.setAdapter(adapter);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.accessibilityRuleDao().getAllAccessibilityRuleFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        ruleList -> {
                            if (ruleList.isEmpty()) {
                                binding.emptyText.setVisibility(View.VISIBLE);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(ruleList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }

    /**
     * 添加权限请求
     */
    private void addPermissionRequests() {
        PermissionHelper helper = new PermissionHelper(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            helper.addPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "请授予通知权限用于触发自动记账后发送通知。"
            );
        }
        helper.addPermission(
                PermissionHelper.SpecialPermissionType.BATTERY,
                "电池优化",
                "为保证软件在后台也可以识别屏幕内容，请将本应用的电池优化策略改为“无限制”。"
        );
        helper.addPermission(
                PermissionHelper.SpecialPermissionType.AUTO_START,
                "自启动权限",
                "通知监听服务需要常驻后台，请允许应用自启动，否则该功能可能无法正常运行。为了进一步保障在后台正常运行，建议您在最近任务锁定本应用。"
        );
    }

    /**
     * 删除通知规则
     *
     * @param rule 待删除的通知规则
     */
    private void deleteRule(@NonNull AccessibilityRuleEntity rule) {
        String message = String.format(
                Locale.getDefault(),
                "确认删除“%s”吗？",
                rule.getName()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_accessibility_rule)
                .setMessage(message)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.accessibilityRuleDao().deleteAccessibilityRule(rule)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Toast.makeText(this, "无障碍规则删除成功", Toast.LENGTH_SHORT).show(),
                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                            )
                    );
                })
                .setNegativeButton("取消", null)
                .show();
    }
}