package com.sly.coffer.ui.pages.notification.group;

import android.content.Intent;
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
import com.sly.coffer.auxiliary.enums.types.MoveDirectionType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.services.NotificationRuleService;
import com.sly.coffer.databinding.ActivityNotificationRuleGroupListBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleGroupListActivity extends AppCompatActivity {
    private ActivityNotificationRuleGroupListBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationRuleGroupListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

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

        //添加规则按钮
        binding.addFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationRuleGroupInputActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.setMarginToNavigation(binding.addFab, this);
        AppearanceHelper.attachMorphAnimation(binding.addFab);

        //帮助按钮
        binding.helpBtn.setOnClickListener(view -> {
            final String EXPLANATION = "### 1. 功能概述\n" +
                    "有时进行一次交易后会有多个通知在短时间内被发送，若不设置分组会导致短时间内触发多个金额相同的规则。为了尽可能去除重复的规则触发，可以将多个规则添加至同一个分组中，处于同一个分组的规则仅会触发一个。\n" +
                    "\n" +
                    "### 2. 规则互斥原理\n" +
                    "\n" +
                    "当解析到符合通知规则的通知时，APP的处理流程如下：\n" +
                    "1. 缓冲一定时间内触发的多个通知规则；\n" +
                    "2. 依次遍历缓冲区内的通知规则，若规则不属于任何一个分组，直接触发；\n" +
                    "3. 按照分组排序从上到下的顺序扫描缓冲区中的通知规则；\n" +
                    "4. 每个分组仅触发找到的优先级最高的规则；\n" +
                    "5. 每个分组中触发过一个规则后，会将该规则所在分组中所有优先级更低的规则从缓冲区移除，从而达到互斥目的；\n" +
                    "6. 重复3~5步，直到所有分组都遍历完毕。\n" +
                    "\n" +
                    "> 例：第一组中有A、B规则，第二组中有B、C规则。当三个规则都成功匹配通知内容时，第一组中A触发，同时排斥B，由于B被排斥，第二组中C触发，最后成功触发A、C规则。\n" +
                    "\n" +
                    "### 3. 分组和规则的优先级\n" +
                    "\n" +
                    "- 规则分组具有优先级，排序越靠前的分组优先级越高，优先级高的分组会优先尝试触发其中的规则。\n" +
                    "\n" +
                    "> 例：第一组中包含A、B规则，第二组包含B、A规则。当A、B规则都尝试触发时，由于第一组优先级更高，因此A触发，B被排斥。\n" +
                    "\n" +
                    "- 分组内的规则具有优先级，排序越靠前的优先级越高，APP会按照优先级由高到低依次尝试触发通知规则，当触发一个规则后排斥所有低优先级的规则。\n" +
                    "\n" +
                    "> 例：第一组包含A、B、C规则，第二组包含B、D、E规则，当B触发时，C、D、E规则都会被排斥。\n" +
                    "\n";
            new MarkdownDialogBuilder(this, "功能介绍", EXPLANATION)
                    .setNegativeButton("关闭", null)
                    .show();
        });

        //列表
        RuleGroupCardListAdapter adapter = new RuleGroupCardListAdapter(
                (entity, anchor) -> {
                    //生成数据包
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.NOTIFICATION_RULE_GROUP_ID.v(), entity.getGroup().getGroupId());

                    //生成 Intent
                    Intent intent = new Intent(this, NotificationRuleGroupInputActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_notification_rule_group_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_delete_notification_rule_group) {
                            deleteGroup(entity.getGroup());
                            return true;
                        } else if (id == R.id.action_move_to_top) {
                            moveGroup(entity.getGroup(), MoveDirectionType.TOP);
                            return true;
                        } else if (id == R.id.action_move_up) {
                            moveGroup(entity.getGroup(), MoveDirectionType.UP);
                            return true;
                        } else if (id == R.id.action_move_down) {
                            moveGroup(entity.getGroup(), MoveDirectionType.DOWN);
                            return true;
                        } else if (id == R.id.action_move_to_bottom) {
                            moveGroup(entity.getGroup(), MoveDirectionType.BOTTOM);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                }
        );
        binding.recycler.setAdapter(adapter);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.notificationRuleDao().getRuleGroupFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        modelList -> {
                            if (modelList.isEmpty()) {
                                binding.emptyText.setVisibility(View.VISIBLE);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(modelList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }

    /**
     * 删除分组
     *
     * @param group 待删除的分组
     */
    private void deleteGroup(@NonNull NotificationRuleGroupEntity group) {
        String message = String.format(
                Locale.getDefault(),
                "确认删除“%s”吗？",
                group.getName()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_notification_rule_group)
                .setMessage(message)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                    disposable.add(db.notificationRuleDao().deleteRuleGroupCompletable(group)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Toast.makeText(this, "分组删除成功", Toast.LENGTH_SHORT).show(),
                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                            )
                    );
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 移动规则分组
     *
     * @param group     待移动的规则分组
     * @param direction 移动的方向种类
     */
    private void moveGroup(@NonNull NotificationRuleGroupEntity group, MoveDirectionType direction) {
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(NotificationRuleService.moveRuleGroup(group.getGroupId(), direction, db)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Toast.makeText(this, "移动成功", Toast.LENGTH_SHORT).show(),
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }
}