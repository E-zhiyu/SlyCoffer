package com.sly.coffer.ui.pages.notification.rule;

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
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityNotificationRuleListBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.notification.capture.NotificationCaptureListActivity;
import com.sly.coffer.ui.pages.notification.group.NotificationRuleGroupListActivity;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleListActivity extends AppCompatActivity {
    private ActivityNotificationRuleListBinding binding;    //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationRuleListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();
        initGuide();
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
        //设置标题栏的图标点击监听器
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        //功能说明按钮
        binding.helpBtn.setOnClickListener(view -> {
            final String EXPLANATION = "### 1. 工作原理\n" +
                    "当手机上的其他应用（如支付宝、微信、银行APP、短信）发出包含交易金额的通知时，本APP会：\n" +
                    "\n" +
                    "1. 识别通知来自哪个应用；\n" +
                    "2. 根据应用来源和通知标题，找到合适的“通知规则”；\n" +
                    "3. 用“通知规则”中的“正则表达式”从通知内容中“抓取”金额数字；\n" +
                    "4. 自动生成一条流水记录，并通过通知向您反馈。\n" +
                    "\n" +
                    "整个过程全自动，您无需手动输入金额。\n" +
                    "\n" +
                    "### 2. 几个关键概念\n" +
                    "- **正则表达式**：一种“文本搜索模板”，可以精确地从一段文字中找出您想要的内容（比如“¥123.45”或“+88.00”）。\n" +
                    "- **捕获组**：正则表达式中用圆括号 `( )` 括起来的部分，表示“我只想提取括号里的那一小段”。  \n" +
                    "- **捕获组数量**：一个正则表达式中可以包含多个捕获组，且能够通过位置提取不同捕获组的内容。特别地，位置为0的捕获组就是整个正则表达式匹配的文本。  \n" +
                    "> 例：通知内容是“消费￥99.00元”，正则写为 `消费￥([\\d.]+)元`，捕获组 `([\\d.]+)` 就会提取出 `99.00`。\n\n" +
                    "> 若您不清楚正则表达式的用法，请尝试询问AI工具，并使用[Regex101](https://regex101.com/)进行测试。\n" +
                    "\n" +
                    "### 3. 使用建议\n" +
                    "\n" +
                    "- 不建议输入过短的通知内容正则表达式，这可能导致解析通知时没有找到符合预期的文本；\n" +
                    "> 例：如果仅输入`(\\d+)`，当遇到类似`您有3笔订单，共花费50元`这种包含多个数字的通知时，APP只会提取到“3”而不是代表金额的“50”。" +
                    "- 开启自启动权限，允许APP在手机在后台自动运行；\n" +
                    "- 电池优化设为“无限制”，避免系统休眠时关闭APP的通知监听服务；\n" +
                    "- 在最近任务列表中锁定APP，防止一键清理后台时被误杀，并提升后台保活优先级。\n" +
                    "\n" +
                    "> 通知监听性能开销极小，您无需担心应用常驻后台导致耗电异常。若未完成上述设置，可能会出现通知收不到或无法自动记账的情况。\n" +
                    "\n" +
                    "### 4. 免责声明\n" +
                    "- 本功能**完全在本地运行**，所有通知数据仅用于金额提取和记账；\n" +
                    "- **APP不会以任何形式收集、存储或上传您的通知内容、金额信息或任何个人数据**；\n" +
                    "- 您创建的所有规则仅保存在本机，请您放心使用。";
            new MarkdownDialogBuilder(this, "功能介绍", EXPLANATION)
                    .setNegativeButton("关闭", null)
                    .show();
        });

        //添加规则按钮
        binding.addFab.setOnClickListener(v -> {
            Intent skip2RuleInput = new Intent(this, NotificationCaptureListActivity.class);
            startActivity(skip2RuleInput);
        });
        binding.addFab.setOnLongClickListener(view -> {
            Intent skip2RuleInput = new Intent(this, NotificationRuleInputActivity.class);
            startActivity(skip2RuleInput);
            return true;
        });
        AppearanceHelper.attachMorphAnimation(binding.addFab);

        //分组列表界面按钮
        binding.groupListFab.setOnClickListener(view -> {
            Intent intent = new Intent(this, NotificationRuleGroupListActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.groupListFab);
        AppearanceHelper.setMarginToNavigation(binding.fabLayout, this);

        //列表
        NotificationRuleListAdapter adapter = new NotificationRuleListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.NOTIFICATION_RULE_ID.v(), entity.getRuleId());

                    Intent skip2RuleInput = new Intent(this, NotificationRuleInputActivity.class);
                    skip2RuleInput.putExtras(bundle);
                    startActivity(skip2RuleInput);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_notification_rule_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_delete_notification_rule) {
                            deleteNotificationRule(entity);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                },
                (entity, finalStat, anchor) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.notificationRuleDao().setRuleEnabled(finalStat, entity.getRuleId())
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
        disposable.add(db.notificationRuleDao().getNotificationRuleFlowable()
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
     * 初始化用户引导
     */
    private void initGuide() {
        final String ADD_LONG_CLICK_TIP = "点按根据捕获的通知生成通知规则，长按手动输入通知规则";
        TipPreference.showTip(
                binding.addFab,
                Gravity.START,
                ADD_LONG_CLICK_TIP,
                TipPreference.KEY_NOTIFICATION_RULE_LIST,
                3
        );
    }

    /**
     * 添加权限申请
     */
    private void addPermissionRequests() {
        PermissionHelper permissionHelper = new PermissionHelper(this);   //权限申请帮助器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionHelper.addPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "请授予通知权限用于触发自动记账后发送通知。"
            );
        }
        permissionHelper.addPermission(
                PermissionHelper.SpecialPermissionType.AUTO_START,
                "自启动权限",
                "通知监听服务需要常驻后台，请允许应用自启动，否则该功能可能无法正常运行。为了进一步保障在后台正常运行，建议您在最近任务锁定本应用。"
        );
        permissionHelper.addPermission(
                PermissionHelper.SpecialPermissionType.BATTERY,
                "电池优化",
                "为保证软件在后台也可以监听通知，请将本应用的电池优化策略改为“无限制”。"
        );
    }

    /**
     * 删除通知规则
     *
     * @param rule 待删除的通知规则
     */
    private void deleteNotificationRule(@NonNull NotificationRuleEntity rule) {
        String message = String.format(
                Locale.getDefault(),
                "确认删除“%s”吗？",
                rule.getName()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_notification_rule)
                .setMessage(message)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.notificationRuleDao().deleteNotificationRule(rule)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Toast.makeText(this, "通知规则删除成功", Toast.LENGTH_SHORT).show(),
                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                            )
                    );
                })
                .setNegativeButton("取消", null)
                .show();
    }
}