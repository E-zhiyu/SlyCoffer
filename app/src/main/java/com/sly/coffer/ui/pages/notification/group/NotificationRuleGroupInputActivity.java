package com.sly.coffer.ui.pages.notification.group;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
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
        ruleAdapter = new RuleListAdapter(
                (entity, anchor) -> {
                    //TODO:点击监听
                },
                (entity, anchor) -> {
                    //TODO:长按监听
                }
        );
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
            //TODO:选择规则
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
                                                    binding.scrollLayout
                                            )
                                    );
                                } else {
                                    VisibilityHelper.toggleViewExpansion(
                                            binding.scrollLayout,
                                            false,
                                            () -> ruleAdapter.submitList(ruleList),
                                            binding.scrollLayout
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
        //TODO:待实现
    }
}