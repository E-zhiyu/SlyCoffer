package com.sly.coffer.ui.pages.notification.rule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.R;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleUnionModel;
import com.sly.coffer.data.save.db.services.RuleService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityNotificationRuleInputBinding;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.adapters.NoFilteringArrayAdapter;
import com.sly.coffer.ui.others.bottom.TagSelectBottomSheet;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountTagAdapter;
import com.sly.coffer.ui.pages.app_list.AppSelectActivity;
import com.sly.coffer.ui.pages.notification.GroupSelectBottomSheet;
import com.sly.coffer.ui.pages.notification.GroupSelectViewModel;
import com.sly.coffer.ui.pages.notification.RuleGroupChipListAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleInputActivity extends AppCompatActivity {
    @Nullable
    private Bundle initBundle = null;                               //存有初始数据数据包
    private AccountType type = AccountType.EXPENSE;                 //流水种类
    private ActivityResultLauncher<Intent> packageNameSelectLauncher;   //包名选择启动器
    private ActivityNotificationRuleInputBinding binding;           //绑定的XML视图引用
    private final CompositeDisposable disposable = new CompositeDisposable();
    private RuleGroupChipListAdapter groupAdapter;          //规则分组适配器
    private AccountTagAdapter tagAdapter;                           //标签适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationRuleInputBinding.inflate(getLayoutInflater());
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
        initLaunchers();
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
        //分组 Recycler
        groupAdapter = new RuleGroupChipListAdapter(
                (entity, anchor, adapter) -> {
                    List<NotificationRuleGroupEntity> removedList = new ArrayList<>(adapter.getCurrentList());
                    removedList.remove(entity);
                    if (!removedList.isEmpty()) {
                        adapter.submitList(
                                removedList,
                                () -> VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        true,
                                        null,
                                        binding.tagRecycler
                                )
                        );
                    } else {
                        VisibilityHelper.toggleViewExpansion(
                                binding.scrollLayout,
                                false,
                                () -> adapter.submitList(removedList),
                                binding.tagRecycler
                        );
                    }

                    //更新 ViewModel 中的数据
                    GroupSelectViewModel viewModel = new ViewModelProvider(this).get(GroupSelectViewModel.class);
                    if (viewModel.getGroupIdSetLiveData().getValue() != null) {
                        viewModel.getGroupIdSetLiveData().getValue().remove(entity.getGroupId());
                    }
                }
        );
        binding.groupRecycler.setAdapter(groupAdapter);

        //标签 Recycler
        tagAdapter = new AccountTagAdapter(
                (entity, anchor, adapter) -> {
                    //切换视图可见性
                    List<TagEntity> removedList = new ArrayList<>(adapter.getCurrentList());
                    removedList.remove(entity);
                    if (!removedList.isEmpty()) {
                        adapter.submitList(
                                removedList,
                                () -> VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        true,
                                        null,
                                        binding.tagRecycler
                                )
                        );
                    } else {
                        VisibilityHelper.toggleViewExpansion(
                                binding.scrollLayout,
                                false,
                                () -> adapter.submitList(removedList),
                                binding.tagRecycler
                        );
                    }

                    //移除ViewModel集合中的数据
                    TagMultiSelectViewModel viewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
                    viewModel.getCheckedTagIdSet().remove(entity.getTagId());
                }
        );
        binding.tagRecycler.setAdapter(tagAdapter);

        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        if (initBundle != null) {
            binding.toolbar.setTitle(R.string.modify_rule);

            long ruleId = initBundle.getLong(KeyStrings.NOTIFICATION_RULE_ID.v());
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().getNotificationRuleWithDetailSingleById(ruleId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            optional -> {
                                if (optional.isEmpty()) return;

                                //解析数据
                                NotificationRuleUnionModel model = optional.get();
                                NotificationRuleEntity rule = model.getRule();
                                NotificationRuleTransferEntity transfer = model.getTransfer();
                                List<TagEntity> tagList = model.getTagList();
                                List<NotificationRuleGroupEntity> groupList = model.getGroupList();

                                //填充文本框
                                binding.nameInput.setText(rule.getName());                      //名称
                                type = AccountType.values()[rule.getType()];
                                binding.typeInput.setText(type.getTitle());                     //种类
                                binding.packageNameInput.setText(rule.getPackageName());        //包名
                                binding.notificationTitleInput.setText(rule.getTargetTitle());  //标题
                                binding.regexInput.setText(rule.getContentRegex());             //正则表达式
                                binding.captureGroupPositionInput.setText(String.valueOf(rule.getCaptureGroupPos()));   //捕获组位置
                                if (type == AccountType.TRANSFER) {
                                    binding.exportAccountLayout.setVisibility(View.VISIBLE);
                                    binding.importAccountLayout.setVisibility(View.VISIBLE);

                                    binding.exportAccountInput.setText(transfer.getExportAccount());    //转出账户
                                    binding.importAccountInput.setText(transfer.getImportAccount());    //转入账户
                                }

                                //显示标签
                                if (!tagList.isEmpty() && tagAdapter != null) {
                                    tagAdapter.submitList(tagList);
                                    binding.tagRecycler.setVisibility(View.VISIBLE);
                                } else {
                                    binding.tagRecycler.setVisibility(View.GONE);
                                }
                                List<Long> tagIdList = tagList.stream()
                                        .map(TagEntity::getTagId)
                                        .collect(Collectors.toList());
                                TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
                                tagMultiSelectViewModel.getCheckedTagIdSet().clear();
                                tagMultiSelectViewModel.getCheckedTagIdSet().addAll(tagIdList);

                                //显示分组
                                if (!groupList.isEmpty() && groupAdapter != null) {
                                    groupAdapter.submitList(groupList);
                                    binding.groupRecycler.setVisibility(View.VISIBLE);
                                } else {
                                    binding.groupRecycler.setVisibility(View.GONE);
                                }
                                List<Long> groupIdList = groupList.stream()
                                        .map(NotificationRuleGroupEntity::getGroupId)
                                        .collect(Collectors.toList());
                                GroupSelectViewModel groupSelectViewModel = new ViewModelProvider(this).get(GroupSelectViewModel.class);
                                groupSelectViewModel.updateCheckedGroupId(new HashSet<>(groupIdList));
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

        //种类
        NoFilteringArrayAdapter<String> typeAdapter = new NoFilteringArrayAdapter<>(
                this,
                Arrays.stream(AccountType.values())
                        .map(AccountType::getTitle)
                        .toArray(String[]::new)
        );
        binding.typeInput.setText(type.getTitle());
        binding.typeInput.setAdapter(typeAdapter);
        binding.typeInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    boolean transferVisible = position == AccountType.TRANSFER.ordinal();
                    VisibilityHelper.toggleViewExpansion(
                            binding.scrollLayout,
                            transferVisible,
                            null,
                            binding.exportAccountLayout,
                            binding.importAccountLayout
                    );

                    type = AccountType.values()[position];
                }
        );

        //转出和转入账户的自动填充适配器
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.accountDao().getTransferAccountsSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        transferAccountList -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    this,
                                    R.layout.exposed_dropdown_popup_item,
                                    transferAccountList
                            );

                            binding.exportAccountInput.setAdapter(adapter);
                            binding.importAccountInput.setAdapter(adapter);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //转出账户
        binding.exportAccountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.exportAccountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.exportAccountInput.getText()).trim();
                if (type == AccountType.TRANSFER && input.isEmpty()) {
                    binding.exportAccountLayout.setError("转出账户不能为空");
                }
            }
        });

        //转入账户
        binding.importAccountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.importAccountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.importAccountInput.getText()).trim();
                if (type == AccountType.TRANSFER && input.isEmpty()) {
                    binding.importAccountLayout.setError("转出账户不能为空");
                }
            }
        });

        //包名
        binding.packageNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.packageNameLayout.setError(null);
                Intent skip2PackageNameSelect = new Intent(this, AppSelectActivity.class);
                packageNameSelectLauncher.launch(skip2PackageNameSelect);
            } else {
                String packageName = String.valueOf(binding.packageNameInput.getText());
                if (packageName.isEmpty()) {
                    binding.packageNameLayout.setErrorEnabled(true);
                    binding.packageNameLayout.setError("包名不能为空");
                }
            }
        });
        binding.packageNameInput.setOnClickListener(view -> {
            binding.packageNameLayout.setError(null);
            Intent skip2PackageNameSelect = new Intent(this, AppSelectActivity.class);
            packageNameSelectLauncher.launch(skip2PackageNameSelect);
        });

        //通知标题
        binding.notificationTitleInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.notificationTitleLayout.setError(null);
            } else {
                String input = String.valueOf(binding.notificationTitleInput.getText()).trim();
                if (input.isEmpty()) {
                    binding.notificationTitleLayout.setError("通知标题不能为空");
                }
            }
        });

        //内容正则表达式
        binding.regexInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.regexLayout.setError(null);
            } else {
                String input = String.valueOf(binding.regexInput.getText()).trim();
                if (input.isEmpty()) {
                    binding.regexLayout.setError("内容正则表达式不能为空");
                }
            }
        });
        binding.regexLayout.setEndIconOnClickListener(v -> {
            int cursorPosition = binding.regexInput.getSelectionStart();
            Editable editable = binding.regexInput.getEditableText();
            final String INSERT_REGEX = "(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)";

            //在光标位置插入文本
            editable.insert(cursorPosition, INSERT_REGEX);
        });

        //分组选择按钮
        binding.groupSelectBtn.setOnClickListener(view -> {
            GroupSelectBottomSheet bottomSheet = new GroupSelectBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), TagStrings.NOTIFICATION_RULE_GROUP_SELECT_BOTTOM.t());
        });

        //分组解释按钮
        binding.groupExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "处于同一分组下的规则仅会触发一个";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //标签选择按钮
        binding.tagSelectBtn.setOnClickListener(view -> {
            TagSelectBottomSheet bottomSheet = new TagSelectBottomSheet();
            Bundle bundle = new Bundle();
            bundle.putInt(KeyStrings.TAG_SCOPE.v(), (int) Math.pow(2, type.ordinal())); //传递标签作用域标识符
            bottomSheet.setArguments(bundle);
            bottomSheet.show(getSupportFragmentManager(), TagStrings.TAG_SELECT_BOTTOM.t());
        });

        //标签说明按钮
        binding.tagExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "自动为生成的流水记录添加下列标签";
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
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        //标签选择
        TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
        tagMultiSelectViewModel.getNeedExecute().observe(this, b -> {
            if (b) {
                BookkeepingDb db = BookkeepingDb.getInstance(this);
                disposable.add(db.tagDao().getTagSingleById(tagMultiSelectViewModel.getCheckedTagIdSet())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                tagList -> {
                                    if (!tagList.isEmpty()) {
                                        tagAdapter.submitList(
                                                tagList,
                                                () -> VisibilityHelper.toggleViewExpansion(
                                                        binding.scrollLayout,
                                                        true,
                                                        null,
                                                        binding.tagRecycler
                                                )
                                        );
                                    } else {
                                        VisibilityHelper.toggleViewExpansion(
                                                binding.scrollLayout,
                                                false,
                                                () -> tagAdapter.submitList(tagList),
                                                binding.tagRecycler
                                        );
                                    }
                                },
                                e -> ExceptionHelper.showExceptionDialog(this, e)
                        )
                );
            }
        });

        //分组选择
        GroupSelectViewModel groupSelectViewModel = new ViewModelProvider(this).get(GroupSelectViewModel.class);
        groupSelectViewModel.getGroupIdSetLiveData().observe(this, checkedIdSet -> {
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().getRuleGroupById(checkedIdSet)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            groupList -> {
                                if (!groupList.isEmpty()) {
                                    groupAdapter.submitList(
                                            groupList,
                                            () -> VisibilityHelper.toggleViewExpansion(
                                                    binding.scrollLayout,
                                                    true,
                                                    null,
                                                    binding.groupRecycler
                                            )
                                    );
                                } else {
                                    VisibilityHelper.toggleViewExpansion(
                                            binding.scrollLayout,
                                            false,
                                            () -> groupAdapter.submitList(groupList),
                                            binding.groupRecycler
                                    );
                                }
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        });
    }

    /**
     * 初始化启动器
     */
    private void initLaunchers() {
        packageNameSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();

                    if (resultCode == Activity.RESULT_OK) {
                        if (data != null) {
                            String packageName = data.getStringExtra(KeyStrings.PACKAGE_NAME.v());
                            binding.packageNameInput.setText(packageName);
                            binding.packageNameLayout.setError(null);
                        } else {
                            NullPointerException e = new NullPointerException("无法获取包名");
                            ExceptionHelper.showExceptionDialog(this, e);
                        }
                    }
                }
        );
    }

    /**
     * 检测输入内容的有效性
     *
     * @return 错误提示字符串（无错误为null）
     */
    @Nullable
    private String verifyInput() {
        String err = null;
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String packageName = String.valueOf(binding.packageNameInput.getText()).trim();
        String notificationTitle = String.valueOf(binding.notificationTitleInput.getText()).trim();
        String contentRegexStr = String.valueOf(binding.regexInput.getText()).trim();
        String captureGroupPosStr = String.valueOf(binding.captureGroupPositionInput.getText()).trim();
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();

        if (name.isEmpty()) {
            err = "名称不能为空";
            binding.nameLayout.setError(err);
        } else if (packageName.isEmpty()) {
            err = "包名不能为空";
            binding.packageNameLayout.setError(err);
        } else if (notificationTitle.isEmpty()) {
            err = "通知标题不能为空";
            binding.notificationTitleLayout.setError(err);
        } else if (contentRegexStr.isEmpty()) {
            err = "内容正则表达式不能为空";
            binding.regexLayout.setError(err);
        } else if (type == AccountType.TRANSFER && exportAccount.isEmpty()) {
            err = "转出账户不能为空";
            binding.exportAccountLayout.setError(err);
        } else if (type == AccountType.TRANSFER && importAccount.isEmpty()) {
            err = "转入账户不能为空";
            binding.importAccountLayout.setError(err);
        } else {
            try {
                Pattern pattern = Pattern.compile(contentRegexStr);
                int captureGroupPos;
                try {
                    captureGroupPos = Integer.parseInt(captureGroupPosStr);
                } catch (NumberFormatException e) {
                    captureGroupPos = 1;
                }
                int groupCount = pattern.matcher("").groupCount();
                if (groupCount < 1) {
                    err = "正则表达式至少应有一个捕获组";
                    binding.regexLayout.setError(err);
                    return err;
                } else if (captureGroupPos <= 0) {
                    err = "捕获组位置必须为非负数";
                    binding.captureGroupPositionLayout.setError(err);
                    return err;
                } else if (captureGroupPos > groupCount) {
                    err = "捕获组位置不能超出捕获组总数";
                    binding.captureGroupPositionLayout.setError(err);
                    return err;
                }
            } catch (PatternSyntaxException e) {
                err = "内容正则表达式存在语法错误";
                binding.regexLayout.setError(err);
            }
        }

        return err;
    }

    /**
     * 将数据保存到数据库
     */
    private void saveData() {
        //获取输入内容
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String packageName = String.valueOf(binding.packageNameInput.getText()).trim();
        String targetTitle = String.valueOf(binding.notificationTitleInput.getText()).trim();
        String contentRegexStr = String.valueOf(binding.regexInput.getText()).trim();
        String captureGroupPosStr = String.valueOf(binding.captureGroupPositionInput.getText()).trim();
        int captureGroupPos;
        try {
            captureGroupPos = Integer.parseInt(captureGroupPosStr);
        } catch (NumberFormatException e) {
            captureGroupPos = 1;
        }
        int typeOrdinal = this.type.ordinal();
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();

        //生成标签 ID 列表
        List<Long> tagIdList;
        if (tagAdapter == null) {
            tagIdList = new ArrayList<>();
        } else {
            tagIdList = tagAdapter.getCurrentList().stream()
                    .map(TagEntity::getTagId)
                    .collect(Collectors.toList());
        }

        //生成规则分组 ID 列表
        List<Long> groupIdList;
        if (groupAdapter == null) {
            groupIdList = new ArrayList<>();
        } else {
            groupIdList = groupAdapter.getCurrentList().stream()
                    .map(NotificationRuleGroupEntity::getGroupId)
                    .collect(Collectors.toList());
        }

        //保存数据
        NotificationRuleEntity rule = new NotificationRuleEntity(
                name,
                typeOrdinal,
                packageName,
                targetTitle,
                contentRegexStr,
                captureGroupPos
        );
        NotificationRuleTransferEntity transfer = new NotificationRuleTransferEntity(exportAccount, importAccount);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        if (initBundle == null) {
            disposable.add(RuleService.addNewNotificationRule(rule, transfer, tagIdList, groupIdList, db)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "规则添加成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        } else {
            long ruleId = initBundle.getLong(KeyStrings.NOTIFICATION_RULE_ID.v());
            rule.setRuleId(ruleId);
            disposable.add(RuleService.modifyNotificationRule(rule, transfer, tagIdList, groupIdList, db)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "规则修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }
    }
}