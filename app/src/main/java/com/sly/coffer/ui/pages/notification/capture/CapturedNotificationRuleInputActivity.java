package com.sly.coffer.ui.pages.notification.capture;

import android.os.Bundle;
import android.view.Gravity;
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

import com.google.android.material.chip.Chip;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.InterestPeriodType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.ExtraTransferEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.services.NotificationRuleService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityCapturedNotificationRuleInputBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.ui.others.adapters.NoFilteringArrayAdapter;
import com.sly.coffer.ui.others.bottom.RelatedLoanSelectBottomSheet;
import com.sly.coffer.ui.others.bottom.TagSelectBottomSheet;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountExtraDataViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountTagAdapter;
import com.sly.coffer.ui.pages.notification.GroupSelectBottomSheet;
import com.sly.coffer.ui.pages.notification.GroupSelectViewModel;
import com.sly.coffer.ui.pages.notification.RuleGroupChipListAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CapturedNotificationRuleInputActivity extends AppCompatActivity {
    private ActivityCapturedNotificationRuleInputBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();
    @Nullable
    private Bundle initBundle = null;                       //带有初始数据的数据包
    @Nullable
    private CapturedNotificationEntity notification = null; //捕获的通知实例
    private RuleGroupChipListAdapter groupAdapter;          //规则分组适配器
    private AccountTagAdapter tagAdapter;                   //标签适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCapturedNotificationRuleInputBinding.inflate(getLayoutInflater());

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
                        tagAdapter.submitList(
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
                                () -> tagAdapter.submitList(removedList),
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
            long notificationId = initBundle.getLong(KeyStrings.CAPTURED_NOTIFICATION_ID.v());

            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.notificationRuleDao().getCapturedNotificationById(notificationId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            optional -> {
                                if (optional.isEmpty()) return;

                                notification = optional.get();

                                //通知内容文本
                                String content = notification.getContent();
                                binding.notificationContentText.setText(content);

                                CNRInputViewModel viewModel = new ViewModelProvider(this).get(CNRInputViewModel.class);
                                int groupPos = viewModel.getGroupPos();

                                //金额选择 ChipGroup
                                Pattern amountPattern = Pattern.compile("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)");
                                Matcher matcher = amountPattern.matcher(content);
                                int i = 1;
                                binding.amountSelectChipGroup.removeAllViews();
                                while (matcher.find()) {
                                    String amountText = matcher.group();

                                    int finalPosition = i;
                                    Chip amountChip = new Chip(this);
                                    amountChip.setCheckable(true);
                                    if (i == groupPos) {
                                        amountChip.setChecked(true);
                                    }
                                    amountChip.setText(amountText);
                                    amountChip.setCheckedIconVisible(true);
                                    amountChip.setOnCheckedChangeListener((compoundButton, b) -> {
                                        if (b) {
                                            viewModel.setGroupPos(finalPosition);
                                        }
                                    });
                                    binding.amountSelectChipGroup.addView(amountChip);

                                    i++;
                                }
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

        //通知内容复制按钮
        binding.contentCopyBtn.setOnClickListener(view -> {
            if (notification == null) {
                Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show();
                return;
            }
            ImmHelper.copyToClipboard("捕获的通知内容", notification.getContent(), this);
            Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
        });

        //金额文本选择说明按钮
        binding.amountSelectHelpBtn.setOnClickListener(view -> {
            final String EXPLANATION = "选择通知中表示金额的文本，此后触发自动记账都将使用该位置的数字作为金额";
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
     * 检测输入内容的有效性
     *
     * @return 错误提示字符串（无错误为null）
     */
    private String verifyInput() {
        String err = null;
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();
        String loanTargetName = String.valueOf(binding.loanTargetNameInput.getText()).trim();

        AccountExtraDataViewModel accountExtraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        AccountType type = accountExtraDataViewModel.getAccountType();
        boolean isTransfer = type.hasExtraData(AccountType.DATA_FLAG_TRANSFER);
        boolean isLoan = type.hasExtraData(AccountType.DATA_FLAG_LOAN);
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
        } else if (notification == null) {
            err = "错误：通知数据获取失败";
        } else {
            final String NUM_REGEX = "\\d";
            Matcher matcher = Pattern.compile(NUM_REGEX).matcher(notification.getContent());
            if (!matcher.find()) {
                err = "错误：通知内容不包含数字";
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
     * 将数据保存到数据库
     */
    private void saveData() {
        if (notification == null) return;

        //获取必要数据
        String name = String.valueOf(binding.nameInput.getText()).trim();
        String packageName = notification.getPackageName();
        String title = notification.getTitle();

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

        //生成通知内容正则表达式
        final String REGEX = "\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?";
        final String REPLACEMENT = "(\\\\d+(?:,\\\\d{3})*(?:\\\\.\\\\d{1,2})?)";
        String contentRegex = TextHelper.removeRegexChar(notification.getContent())
                .replaceAll(REGEX, REPLACEMENT);

        //保存数据
        AccountExtraDataViewModel extraDataViewModel = new ViewModelProvider(this).get(AccountExtraDataViewModel.class);
        CNRInputViewModel cnrViewModel = new ViewModelProvider(this).get(CNRInputViewModel.class);
        NotificationRuleEntity rule = new NotificationRuleEntity(
                name,
                extraDataViewModel.getAccountType().ordinal(),
                packageName,
                title,
                contentRegex,
                cnrViewModel.getGroupPos()
        );
        BookkeepingDb db = BookkeepingDb.getInstance(this);
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