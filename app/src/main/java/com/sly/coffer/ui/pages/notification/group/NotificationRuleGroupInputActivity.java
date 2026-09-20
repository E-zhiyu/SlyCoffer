package com.sly.coffer.ui.pages.notification.group;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleAndGroupUnionModel;
import com.sly.coffer.data.save.db.services.NotificationRuleService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityNotificationRuleGroupInputBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleGroupInputActivity extends AppCompatActivity {
    private ActivityNotificationRuleGroupInputBinding binding;  //绑定的 XML 布局
    private Bundle initBundle = null;                           //包含初始化数据的数据包
    private final CompositeDisposable disposable = new CompositeDisposable();
    private RuleListAdapter ruleAdapter;                        //通知规则的适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationRuleGroupInputBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            //滚动视图的内部布局
            binding.scrollLayout.setPadding(
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10) + Math.max(ime.bottom, systemBars.bottom)
            );

            return insets;
        });

        initBundle = getIntent().getExtras();
        initViews();
        observeLiveData();
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
        //规则列表
        ruleAdapter = new RuleListAdapter(this::showMenu);
        binding.ruleRecycler.setAdapter(ruleAdapter);

        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        if (initBundle != null) {
            long groupId = initBundle.getLong(KeyStrings.NOTIFICATION_RULE_GROUP_ID.v());
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().getGroupAndRuleSingleById(groupId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            optional -> {
                                if (optional.isEmpty()) return;

                                NotificationRuleAndGroupUnionModel model = optional.get();
                                NotificationRuleGroupEntity group = model.getGroup();
                                List<NotificationRuleEntity> ruleList = model.getRuleList();

                                //填充文本框
                                binding.nameInput.setText(group.getName()); //名称

                                //规则列表
                                List<Long> ruleIdList = ruleList.stream()
                                        .map(NotificationRuleEntity::getRuleId)
                                        .collect(Collectors.toList());
                                NotificationRuleSelectViewModel viewModel = new ViewModelProvider(this).get(NotificationRuleSelectViewModel.class);
                                viewModel.updateRuleLiveData(ruleIdList);
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }

        //名称
        binding.nameInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.nameLayout.setError(null);
            } else {
                String input = String.valueOf(binding.nameInput.getText()).trim();
                if (input.isEmpty()) {
                    binding.nameLayout.setError("名称不能为空");
                }
            }
        });
        ImmHelper.showImm(binding.nameInput);

        //规则选择按钮
        binding.ruleSelectBtn.setOnClickListener(view -> {
            NotificationRuleSelectBottomSheet bottomSheet = new NotificationRuleSelectBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), TagStrings.NOTIFICATION_RULE_SELECT_BOTTOM.t());
        });

        //规则解释按钮
        binding.ruleExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "处于同一分组下的规则仅触发一个，\n越靠上的规则优先级越高";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //确认按钮
        binding.confirmButton.setOnClickListener(view -> {
            String err = verifyInput();
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                return;
            }

            saveData();
        });
    }

    /**
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        //规则选择逻辑
        NotificationRuleSelectViewModel ruleSelectViewModel = new ViewModelProvider(this).get(NotificationRuleSelectViewModel.class);
        ruleSelectViewModel.getRuleIdLiveData().observe(this, ruleIdList -> {
            if (ruleIdList == null || ruleAdapter == null) return;

            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(NotificationRuleService.getNotificationByIdInOrder(ruleIdList, db)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            ruleList -> {
                                if (!ruleList.isEmpty()) {
                                    ruleAdapter.submitList(
                                            ruleList,
                                            () -> VisibilityHelper.toggleViewExpansion(
                                                    binding.scrollLayout,
                                                    true,
                                                    null,
                                                    binding.ruleRecycler
                                            )
                                    );
                                } else {
                                    VisibilityHelper.toggleViewExpansion(
                                            binding.scrollLayout,
                                            false,
                                            () -> ruleAdapter.submitList(ruleList),
                                            binding.ruleRecycler
                                    );
                                }
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        });
    }

    /**
     * 校验输入内容有效性
     *
     * @return 错误提示，无错误则返回 null
     */
    private String verifyInput() {
        String err = null;
        String name = String.valueOf(binding.nameInput.getEditableText()).trim();

        if (name.isEmpty()) {
            err = "名称不能为空";
            binding.nameLayout.setError(err);
        }

        return err;
    }

    /**
     * 保存数据
     */
    private void saveData() {
        //获取输入内容
        String name = String.valueOf(binding.nameInput.getEditableText()).trim();

        //生成选择的规则编号的列表
        List<Long> ruleIdList;
        if (ruleAdapter == null) {
            ruleIdList = new ArrayList<>();
        } else {
            ruleIdList = ruleAdapter.getCurrentList().stream()
                    .map(NotificationRuleEntity::getRuleId)
                    .collect(Collectors.toList());
        }

        //保存数据
        NotificationRuleGroupEntity group = new NotificationRuleGroupEntity(name);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        if (initBundle == null) {
            disposable.add(NotificationRuleService.addNotificationRuleGroup(group, ruleIdList, db)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "分组添加成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        } else {
            long groupId = initBundle.getLong(KeyStrings.NOTIFICATION_RULE_GROUP_ID.v(), 0);
            group.setGroupId(groupId);
            disposable.add(NotificationRuleService.modifyNotificationRuleGroup(group, ruleIdList, db)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "分组修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }
    }

    /**
     * 显示悬浮菜单
     *
     * @param rule   选择的规则
     * @param anchor 菜单的锚点
     * @param pos    选择的规则所处的下标
     */
    private void showMenu(NotificationRuleEntity rule, View anchor, int pos) {
        if (ruleAdapter == null) return;

        PopupMenu menu = new PopupMenu(this, anchor, Gravity.END);
        menu.getMenuInflater().inflate(R.menu.menu_notification_rule_group_move, menu.getMenu());

        //设置点击监听
        menu.setOnMenuItemClickListener(item -> {
            List<NotificationRuleEntity> ruleList = new ArrayList<>(ruleAdapter.getCurrentList());

            int id = item.getItemId();
            boolean result = false; //返回值
            if (id == R.id.action_move_to_top) {
                if (pos <= 0) return true;

                NotificationRuleEntity poopedRule = ruleList.remove(pos);
                ruleList.add(0, poopedRule);
                result = true;
            } else if (id == R.id.action_move_up) {
                if (pos <= 0) return true;

                NotificationRuleEntity poopedRule = ruleList.remove(pos);
                ruleList.add(pos - 1, poopedRule);
                result = true;
            } else if (id == R.id.action_move_down) {
                if (pos >= ruleList.size()) return true;

                NotificationRuleEntity poopedRule = ruleList.remove(pos);
                ruleList.add(pos + 1, poopedRule);
                result = true;
            } else if (id == R.id.action_move_to_bottom) {
                if (pos >= ruleList.size() - 1) return true;

                NotificationRuleEntity poopedRule = ruleList.remove(pos);
                ruleList.add(poopedRule);
                result = true;
            }

            if (result) {
                ruleAdapter.submitList(ruleList);
            }

            return result;
        });

        menu.show();
    }
}