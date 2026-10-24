package com.sly.coffer.ui.pages.accessibility.rule;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccessibilityRuleUnionModel;
import com.sly.coffer.data.save.db.services.AccessibilityRuleService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityAccessibilityRuleInputBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.ui.others.adapters.NoFilteringArrayAdapter;
import com.sly.coffer.ui.others.bottom.PickedPageSelectBottomSheet;
import com.sly.coffer.ui.others.bottom.RelatedLoanSelectBottomSheet;
import com.sly.coffer.ui.others.bottom.TagSelectBottomSheet;
import com.sly.coffer.ui.others.dialogs.EditTextDialogBuilder;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountExtraDataViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountTagAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AccessibilityRuleInputActivity extends AppCompatActivity {
    private ActivityAccessibilityRuleInputBinding binding;
    @Nullable
    private Bundle initBundle = null;               //存有初始数据数据包
    private final CompositeDisposable disposable = new CompositeDisposable();
    private AccountTagAdapter tagAdapter;           //标签适配器
    private KeywordGroupListAdapter keywordAdapter; //关键词组合适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccessibilityRuleInputBinding.inflate(getLayoutInflater());

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
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //关键词组合 Recycler
        keywordAdapter = new KeywordGroupListAdapter(
                (entity, anchor, adapter) -> {
                    //切换视图可见性
                    List<AccessibilityRuleKeywordGroupEntity> removedList = new ArrayList<>(adapter.getCurrentList());
                    removedList.remove(entity);
                    if (!removedList.isEmpty()) {
                        adapter.submitList(
                                removedList,
                                () -> VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        true,
                                        null,
                                        binding.keywordGroupRecycler
                                )
                        );
                    } else {
                        VisibilityHelper.toggleViewExpansion(
                                binding.scrollLayout,
                                false,
                                () -> adapter.submitList(removedList),
                                binding.keywordGroupRecycler
                        );
                    }
                },
                (entity, anchor) -> {
                    String[] parts = entity.getContent().split("\\s+");
                    StringBuilder display = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) {
                        display.append(parts[i]);
                        if (i < parts.length - 1) {
                            display.append("、");
                        }
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.keyword_group)
                            .setMessage(display.toString())
                            .setNegativeButton("关闭", null)
                            .show();
                }
        );
        binding.keywordGroupRecycler.setAdapter(keywordAdapter);

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

            long ruleId = initBundle.getLong(KeyStrings.ACCESSIBILITY_RULE_ID.v());
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.accessibilityRuleDao().getRuleWithDetailById(ruleId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            optional -> {
                                if (optional.isEmpty()) return;

                                //读取数据
                                AccessibilityRuleUnionModel model = optional.get();
                                AccessibilityRuleEntity rule = model.getRule();
                                List<TagEntity> tagList = model.getTagList();
                                List<AccessibilityRuleKeywordGroupEntity> keywordGroupList = model.getKeywordGroupList();

                                //填充数据
                                AccountExtraDataViewModel extraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
                                AccountType type = AccountType.values()[rule.getType()];
                                extraDataViewModel.updateAccountType(type);
                                AccessibilityRuleInputViewModel ruleViewModel = new ViewModelProvider(this).get(AccessibilityRuleInputViewModel.class);
                                binding.nameInput.setText(rule.getName());                  //名称
                                binding.typeInput.setText(extraDataViewModel.getAccountType().getTitle());  //种类
                                if (ruleViewModel.getPickedPage().getValue() == null) { //仅没有设置拾取时才填充
                                    //填充拾取结果
                                    PickedPageEntity pickedPage = new PickedPageEntity(
                                            "拾取的界面",
                                            rule.getPackageName(),
                                            rule.getActivityName(),
                                            LocalDateTime.now()
                                    );
                                    ruleViewModel.setPickResult(pickedPage);
                                }

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
                                    extraDataViewModel.updateRelatedLoan(repayAccount);
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

                                //显示关键词组合
                                if (!keywordGroupList.isEmpty()) {
                                    keywordAdapter.submitList(keywordGroupList);
                                    binding.keywordGroupRecycler.setVisibility(View.VISIBLE);
                                } else {
                                    binding.keywordGroupRecycler.setVisibility(View.GONE);
                                }

                                //显示标签
                                if (!tagList.isEmpty()) {
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
        disposable.add(db.extraDataDao().getLoanTargetNameSingle()
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

        //拾取界面选择按钮
        binding.amountPageSelectBtn.setOnClickListener(view -> {
            PickedPageSelectBottomSheet bottomSheet = new PickedPageSelectBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), TagStrings.PICKED_VIEW_BOTTOM.t());
        });

        //拾取界面说明按钮
        binding.amountPageExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "当处于选中的界面时尝试提取金额信息并生成流水记录";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //关键词组合添加按钮
        binding.keywordGroupAddBtn.setOnClickListener(view -> {
            String keywordGroup = getString(R.string.keyword_group);
            new EditTextDialogBuilder(this, "输入" + keywordGroup, keywordGroup)
                    .setHelpText("可通过空格隔开不同的词语")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", inputStr -> {
                        long ruleId = initBundle == null ? 0 : initBundle.getLong(KeyStrings.ACCESSIBILITY_RULE_ID.v());
                        List<AccessibilityRuleKeywordGroupEntity> keywordGroupList = new ArrayList<>(keywordAdapter.getCurrentList());
                        keywordGroupList.add(new AccessibilityRuleKeywordGroupEntity(ruleId, inputStr.trim()));
                        keywordAdapter.submitList(
                                keywordGroupList,
                                () -> VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        true,
                                        null,
                                        binding.keywordGroupRecycler
                                )
                        );
                    })
                    .show();
        });

        //关键词组合说明按钮
        binding.keywordGroupExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "当任意一个组合中的关键词都在界面中出现时，才触发该规则并生成流水记录\n使用空格区分不同的关键词";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //标签选择按钮
        binding.tagSelectBtn.setOnClickListener(view -> {
            TagSelectBottomSheet bottomSheet = new TagSelectBottomSheet();
            Bundle bundle = new Bundle();
            bundle.putInt(KeyStrings.TAG_SCOPE.v(), (int) Math.pow(2, accountExtraDataViewModel.getAccountType().ordinal()));  //传递标签作用域标识符
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

        //金额文本选择
        AccessibilityRuleInputViewModel ruleInputViewModel = new ViewModelProvider(this)
                .get(AccessibilityRuleInputViewModel.class);
        ruleInputViewModel.getPickedPage().observe(this, pickedPage -> {
            if (pickedPage == null) {
                //清空文本框
                binding.packageNameInput.setText("");
                binding.activityNameInput.setText("");
            } else {
                //填充文本框
                binding.packageNameInput.setText(pickedPage.getPackageName());      //包名
                binding.activityNameInput.setText(pickedPage.getActivityName());    //界面名称

                //消除错误提示
                binding.packageNameLayout.setError(null);   //包名
                binding.activityNameLayout.setError(null);  //界面名称
            }
        });
    }

    /**
     * 校验输入内容的有效性
     *
     * @return 错误提示，无错误则返回 null
     */
    private String verifyInput() {
        String err = null;
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();
        String loanTargetName = String.valueOf(binding.loanTargetNameInput.getText()).trim();

        //判断额外信息是否显示
        AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccountType type = accountExtraDataViewModel.getAccountType();
        boolean isTransfer = type.hasExtraData(AccountType.DATA_FLAG_TRANSFER);
        boolean isLoan = type.hasExtraData(AccountType.DATA_FLAG_LOAN);

        AccessibilityRuleInputViewModel viewModel = new ViewModelProvider(this).get(AccessibilityRuleInputViewModel.class);
        if (name.isEmpty()) {
            err = "名称不能为空";
            binding.nameLayout.setError(err);
        } else if (isTransfer && exportAccount.isEmpty()) {
            err = "转出账户不能为空";
            binding.exportAccountLayout.setError(err);
        } else if (isTransfer && importAccount.isEmpty()) {
            err = "转入账户不能为空";
            binding.importAccountLayout.setError(err);
        } else if (isLoan && loanTargetName.isEmpty()) {
            err = "借款对象不能为空";
            binding.loanTargetNameLayout.setError(err);
        } else if (viewModel.getPickedPage().getValue() == null) {
            err = "必须设置金额界面";
            binding.packageNameLayout.setError(err);
            binding.activityNameLayout.setError(err);
        }

        return err;
    }

    /**
     * 首先保存额外的数据
     *
     * @param rule 无障碍规则
     * @return 是否完成
     */
    private Completable saveExtraDataFirstly(AccessibilityRuleEntity rule) {
        AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccountType type = viewModel.getAccountType();
        BookkeepingDb db = BookkeepingDb.getInstance(this);

        if (type.hasExtraData()) {
            rule.setRepayAccountId(viewModel.getRepayAccountId());  //设置关联的借款记录的编号

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
                transferTask = db.extraDataDao().insertExtraTransferSingle(transfer)
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
                loanTask = db.extraDataDao().insertExtraLoanSingle(loan)
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
     * 保存数据
     */
    private void saveData() {
        //获取输入内容
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String packageName = String.valueOf(binding.packageNameInput.getEditableText()).trim();
        String activityName = String.valueOf(binding.activityNameInput.getEditableText()).trim();

        //生成标签 ID 列表
        List<Long> tagIdList = tagAdapter.getCurrentList().stream()
                .map(TagEntity::getTagId)
                .collect(Collectors.toList());

        //保存数据
        AccountExtraDataViewModel viewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccessibilityRuleEntity rule = new AccessibilityRuleEntity(
                name,
                viewModel.getAccountType().ordinal(),
                packageName,
                activityName
        );
        rule.setRepayAccountId(viewModel.getRepayAccountId());  //设置关联的借款记录的编号
        List<AccessibilityRuleKeywordGroupEntity> keywordGroupList = new ArrayList<>(keywordAdapter.getCurrentList());
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        if (initBundle == null) {
            disposable.add(saveExtraDataFirstly(rule)
                    .andThen(AccessibilityRuleService.addNewAccessibilityRule(rule, keywordGroupList, tagIdList, db))
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
            long ruleId = initBundle.getLong(KeyStrings.ACCESSIBILITY_RULE_ID.v());
            rule.setRuleId(ruleId);
            disposable.add(saveExtraDataFirstly(rule)
                    .andThen(AccessibilityRuleService.modifyAccessibilityRule(rule, keywordGroupList, tagIdList, db))
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