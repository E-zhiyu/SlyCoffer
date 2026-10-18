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
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.NotificationRuleUnionModel;
import com.sly.coffer.data.save.db.services.NotificationRuleService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityNotificationRuleInputBinding;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.ui.others.adapters.NoFilteringArrayAdapter;
import com.sly.coffer.ui.others.bottom.RelatedLoanSelectBottomSheet;
import com.sly.coffer.ui.others.bottom.TagSelectBottomSheet;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountExtraDataViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountTagAdapter;
import com.sly.coffer.ui.pages.common.app_list.AppSelectActivity;
import com.sly.coffer.ui.pages.notification.GroupSelectBottomSheet;
import com.sly.coffer.ui.pages.notification.GroupSelectViewModel;
import com.sly.coffer.ui.pages.notification.RuleGroupChipListAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleInputActivity extends AppCompatActivity {
    @Nullable
    private Bundle initBundle = null;                               //存有初始数据数据包
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
                                        binding.groupRecycler
                                )
                        );
                    } else {
                        VisibilityHelper.toggleViewExpansion(
                                binding.scrollLayout,
                                false,
                                () -> adapter.submitList(removedList),
                                binding.groupRecycler
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
                    if (viewModel.getCheckedIdLiveData().getValue() != null) {
                        viewModel.getCheckedIdLiveData().getValue().remove(entity.getTagId());
                    }
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
                                List<TagEntity> tagList = model.getTagList();
                                List<NotificationRuleGroupEntity> groupList = model.getGroupList();

                                //填充文本框
                                AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
                                AccountType type = AccountType.values()[rule.getType()];
                                accountExtraDataViewModel.updateAccountType(type);
                                binding.nameInput.setText(rule.getName());                      //名称
                                binding.typeInput.setText(type.getTitle());                     //种类
                                binding.packageNameInput.setText(rule.getPackageName());        //包名
                                binding.notificationTitleInput.setText(rule.getTargetTitle());  //标题
                                binding.regexInput.setText(rule.getContentRegex());             //正则表达式
                                binding.captureGroupPositionInput.setText(String.valueOf(rule.getCaptureGroupPos()));   //捕获组位置

                                //转账相关的文本框
                                if (type.hasExtraData(AccountType.DATA_FLAG_TRANSFER)) {
                                    binding.transferInputLayout.setVisibility(View.VISIBLE);

                                    ExtraTransferEntity transfer = model.getTransfer();
                                    if (rule.getTransferId() != null && transfer != null) {
                                        binding.exportAccountInput.setText(transfer.getExportAccount());    //转出账户
                                        binding.importAccountInput.setText(transfer.getImportAccount());    //转入账户
                                    }
                                }

                                //还款额外数据文本框
                                if (type.hasExtraData(AccountType.DATA_FLAG_REPAY)) {
                                    binding.repayExtraLayout.setVisibility(View.VISIBLE);

                                    AccountEntity repayAccount = model.getRepayRelatedAccount();
                                    accountExtraDataViewModel.updateRelatedLoan(repayAccount);
                                    if (rule.getRepayAccountId() != null && repayAccount != null) {
                                        binding.relatedLoanInput.setText(repayAccount.getRemark()); //关联的借款记录备注
                                    }
                                }

                                //借款额外数据文本框
                                if (type.hasExtraData(AccountType.DATA_FLAG_LOAN)) {
                                    binding.loanExtraLayout.setVisibility(View.VISIBLE);

                                    ExtraLoanEntity loan = model.getLoan();
                                    if (rule.getLoanId() != null && loan != null) {
                                        binding.loanTargetNameInput.setText(loan.getLoanTargetName());      //借款对象
                                        InterestPeriodType interestPeriodType = InterestPeriodType.values()[loan.getInterestPeriodType()];
                                        binding.interestPeriodInput.setText(interestPeriodType.getTitle()); //结息周期
                                        binding.annualInterestRateInput.setText(String.valueOf(loan.getYearBasepoint() / 100.0));   //年利率
                                        if (loan.getRepayDate() != null) {
                                            binding.repayDateInput.setText(loan.getRepayDate().format(CustomDateTimeFormatter.DATE));   //应还日期
                                        }
                                        binding.compoundInterestSwitch.setChecked(loan.isAllowCompoundInterest());  //允许复利
                                    }
                                }

                                //显示标签
                                if (!tagList.isEmpty() && tagAdapter != null) {
                                    tagAdapter.submitList(tagList);
                                    binding.tagRecycler.setVisibility(View.VISIBLE);
                                } else {
                                    binding.tagRecycler.setVisibility(View.GONE);
                                }
                                Set<Long> tagIdList = tagList.stream()
                                        .map(TagEntity::getTagId)
                                        .collect(Collectors.toSet());
                                TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
                                tagMultiSelectViewModel.updateCheckedIdLiveData(tagIdList);

                                //显示分组
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
        AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        NoFilteringArrayAdapter<String> typeAdapter = new NoFilteringArrayAdapter<>(
                this,
                Arrays.stream(AccountType.values())
                        .map(AccountType::getTitle)
                        .toArray(String[]::new)
        );
        binding.typeInput.setText(accountExtraDataViewModel.getAccountType().getTitle());
        binding.typeInput.setAdapter(typeAdapter);
        binding.typeInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
                    viewModel.updateAccountType(AccountType.values()[position]);
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

        //借款对象适配器
        disposable.add(db.accountDao().getLoanTargetNameSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        loanTargetNameList -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    this,
                                    R.layout.exposed_dropdown_popup_item,
                                    loanTargetNameList
                            );
                            binding.loanTargetNameInput.setAdapter(adapter);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //结息周期自动填充适配器
        List<String> interestPeriodTypeTitleList = Arrays.stream(InterestPeriodType.values())
                .map(InterestPeriodType::getTitle)
                .collect(Collectors.toList());
        NoFilteringArrayAdapter<String> interestPeriodAdapter = new NoFilteringArrayAdapter<>(
                this, interestPeriodTypeTitleList
        );
        binding.interestPeriodInput.setAdapter(interestPeriodAdapter);

        //转出账户
        binding.exportAccountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.exportAccountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.exportAccountInput.getText()).trim();
                boolean isTransfer = accountExtraDataViewModel.getAccountType().hasExtraData(AccountType.DATA_FLAG_TRANSFER);
                if (isTransfer && input.isEmpty()) {
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
                boolean isTransfer = accountExtraDataViewModel.getAccountType().hasExtraData(AccountType.DATA_FLAG_TRANSFER);
                if (isTransfer && input.isEmpty()) {
                    binding.importAccountLayout.setError("转出账户不能为空");
                }
            }
        });

        //关联的借款记录
        binding.relatedLoanInput.setOnClickListener(view -> {
            AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
            Bundle bundle = new Bundle();
            bundle.putInt(KeyStrings.RUNNING_TYPE.v(), viewModel.getAccountType().ordinal());

            RelatedLoanSelectBottomSheet bottomSheet = new RelatedLoanSelectBottomSheet();
            bottomSheet.setArguments(bundle);
            bottomSheet.show(getSupportFragmentManager(), TagStrings.RELATED_LOAN_SELECT_BOTTOM.t());
        });
        binding.relatedLoanInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
                Bundle bundle = new Bundle();
                bundle.putInt(KeyStrings.RUNNING_TYPE.v(), viewModel.getAccountType().ordinal());

                RelatedLoanSelectBottomSheet bottomSheet = new RelatedLoanSelectBottomSheet();
                bottomSheet.setArguments(bundle);
                bottomSheet.show(getSupportFragmentManager(), TagStrings.RELATED_LOAN_SELECT_BOTTOM.t());
            }
        });

        //应还日期
        binding.repayDateInput.setOnClickListener(v -> selectRepayDate());
        binding.repayDateInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectRepayDate();
            }
        });

        //借款对象
        binding.loanTargetNameInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.loanTargetNameLayout.setError(null);
            } else {
                String input = String.valueOf(binding.loanTargetNameInput.getText()).trim();
                boolean isLoan = accountExtraDataViewModel.getAccountType().hasExtraData(AccountType.DATA_FLAG_LOAN);
                if (isLoan && input.isEmpty()) {
                    binding.loanTargetNameLayout.setError("借款对象不能为空");
                }
            }
        });

        //结息周期
        binding.interestPeriodInput.setText(accountExtraDataViewModel.getInterestPeriodType().getTitle());
        binding.interestPeriodInput.setOnItemClickListener((parent, view, position, id) -> {
            AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
            viewModel.setInterestPeriodType(InterestPeriodType.values()[position]);
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
            bundle.putInt(KeyStrings.TAG_SCOPE.v(), (int) Math.pow(2, accountExtraDataViewModel.getAccountType().ordinal())); //传递标签作用域标识符
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
        //流水记录种类
        AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        accountExtraDataViewModel.getAccountTypeLiveData().observe(this, type -> {
            //转账账户输入框
            VisibilityHelper.toggleViewExpansion(
                    binding.scrollLayout,
                    type.hasExtraData(AccountType.DATA_FLAG_TRANSFER),
                    null,
                    binding.transferInputLayout
            );

            //还款信息输入框
            VisibilityHelper.toggleViewExpansion(
                    binding.scrollLayout,
                    type.hasExtraData(AccountType.DATA_FLAG_REPAY),
                    null,
                    binding.repayExtraLayout
            );

            //借款信息输入框
            VisibilityHelper.toggleViewExpansion(
                    binding.scrollLayout,
                    type.hasExtraData(AccountType.DATA_FLAG_LOAN),
                    null,
                    binding.loanExtraLayout
            );
        });

        //关联的还款记录
        accountExtraDataViewModel.getRelatedLoanAccountLiveData().observe(this, account ->
                binding.relatedLoanInput.setText(account == null ? "" : account.getRemark())
        );

        //标签选择
        TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
        tagMultiSelectViewModel.getCheckedIdLiveData().observe(this, idSet -> {
            if (idSet == null) return;

            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.tagDao().getTagSingleById(idSet)
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
        });

        //分组选择
        GroupSelectViewModel groupSelectViewModel = new ViewModelProvider(this).get(GroupSelectViewModel.class);
        groupSelectViewModel.getGroupIdSetLiveData().observe(this, checkedIdSet -> {
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().getRuleGroupSingleById(checkedIdSet)
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
        String loanTargetName = String.valueOf(binding.loanTargetNameInput.getText()).trim();

        //判断额外信息是否显示
        AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccountType type = accountExtraDataViewModel.getAccountType();
        boolean isTransfer = type.hasExtraData(AccountType.DATA_FLAG_TRANSFER);
        boolean isLoan = type.hasExtraData(AccountType.DATA_FLAG_LOAN);

        //判断输入内容有效性
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
        } else if (isTransfer && exportAccount.isEmpty()) {
            err = "转出账户不能为空";
            binding.exportAccountLayout.setError(err);
        } else if (isTransfer && importAccount.isEmpty()) {
            err = "转入账户不能为空";
            binding.importAccountLayout.setError(err);
        } else if (isLoan && loanTargetName.isEmpty()) {
            err = "借款对象不能为空";
            binding.loanTargetNameLayout.setError(err);
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
     * 首先保存额外的数据
     *
     * @param rule 通知规则
     * @return 是否完成
     */
    private Completable saveExtraDataFirstly(NotificationRuleEntity rule) {
        AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccountType type = viewModel.getAccountType();
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        if (type.hasExtraData()) {
            //判断是否需要保存额外数据
            boolean isTransfer = type.hasExtraData(AccountType.DATA_FLAG_TRANSFER);
            boolean isLoan = type.hasExtraData(AccountType.DATA_FLAG_LOAN);

            //转账额外数据
            Completable transferTask, loanTask;
            if (isTransfer) {
                String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
                String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();
                ExtraTransferEntity transfer = new ExtraTransferEntity(
                        exportAccount,
                        importAccount
                );
                transferTask = db.notificationRuleDao().insertExtraTransferSingle(transfer)
                        .flatMapCompletable(transferId -> {
                            rule.setTransferId(transferId);
                            return Completable.complete();
                        });
            } else {
                transferTask = Completable.complete();
            }

            //借款额外数据
            if (isLoan) {
                String loanTargetName = String.valueOf(binding.loanTargetNameInput.getText()).trim();
                int interestPeriod = viewModel.getInterestPeriodType().ordinal();
                int yearBasepoint;
                String annualInterestStr = String.valueOf(binding.annualInterestRateInput.getText()).trim();
                if (annualInterestStr.isEmpty()) {
                    yearBasepoint = 0;
                } else {
                    try {
                        double annualInterest = Double.parseDouble(annualInterestStr);
                        yearBasepoint = (int) (annualInterest * 100);
                    } catch (NumberFormatException e) {
                        yearBasepoint = 0;
                    }
                }
                String repayDateStr = String.valueOf(binding.repayDateInput.getText()).trim();
                LocalDate repayDate;
                if (repayDateStr.isEmpty()) {
                    repayDate = null;
                } else {
                    repayDate = LocalDate.parse(repayDateStr, CustomDateTimeFormatter.DATE);
                }
                boolean allowCompoundInterest = binding.compoundInterestSwitch.isChecked();

                ExtraLoanEntity loan = new ExtraLoanEntity(
                        loanTargetName,
                        interestPeriod,
                        yearBasepoint,
                        allowCompoundInterest,
                        repayDate
                );
                loanTask = db.notificationRuleDao().insertExtraLoanSingle(loan)
                        .flatMapCompletable(loanId -> {
                            rule.setLoanId(loanId);
                            return Completable.complete();
                        });
            } else {
                loanTask = Completable.complete();
            }

            return transferTask.andThen(loanTask);
        } else {
            return Completable.complete();
        }
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
        AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        NotificationRuleEntity rule = new NotificationRuleEntity(
                name,
                viewModel.getAccountType().ordinal(),
                packageName,
                targetTitle,
                contentRegexStr,
                captureGroupPos
        );
        rule.setRepayAccountId(viewModel.getRepayAccountId());  //设置关联的借款记录的编号
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        if (initBundle == null) {
            disposable.add(saveExtraDataFirstly(rule)
                    .andThen(NotificationRuleService.addNewNotificationRule(rule, tagIdList, groupIdList, db))
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
            disposable.add(saveExtraDataFirstly(rule)
                    .andThen(NotificationRuleService.modifyNotificationRule(rule, tagIdList, groupIdList, db))
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

    /**
     * 选择应还日期
     */
    private void selectRepayDate() {
        //解析已输入的时间
        String datetimeStr = String.valueOf(binding.repayDateInput.getText()).trim();
        LocalDate inputDate = datetimeStr.isEmpty() ?
                LocalDate.now() :
                LocalDate.parse(datetimeStr, CustomDateTimeFormatter.DATE);

        DateTimePickerHelper.selectDate(
                inputDate,
                new Pair<>(LocalDate.now(), null),
                getSupportFragmentManager(),
                dateTimeMillis -> {
                    //组合日期和时间
                    LocalDate selectedDate = DateTimePickerHelper.getLocalDateFromTimeMilli(dateTimeMillis);
                    binding.repayDateInput.setText(selectedDate.format(CustomDateTimeFormatter.DATE));
                }
        );
    }
}