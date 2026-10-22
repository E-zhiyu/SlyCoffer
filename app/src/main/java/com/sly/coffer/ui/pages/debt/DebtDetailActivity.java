package com.sly.coffer.ui.pages.debt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.ExtraLoanEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccountLoanUnionModel;
import com.sly.coffer.databinding.ActivityDebtDetailBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.compute.DebtHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.main.bookkeeping.RunningAccountInputActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DebtDetailActivity extends AppCompatActivity {
    private ActivityDebtDetailBinding binding;  //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();
    @Nullable
    private Bundle initBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDebtDetailBinding.inflate(getLayoutInflater());

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
        //初始化适配器
        RepaymentAccountListAdapter adapter = new RepaymentAccountListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.RUNNING_ID.v(), entity.getAccountId());

                    Intent intent = new Intent(this, RunningAccountInputActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
        );
        binding.recycler.setAdapter(adapter);

        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        if (initBundle != null) {
            long accountId = initBundle.getLong(KeyStrings.RUNNING_ID.v());
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.extraDataDao().getAccountWithLoanFlowableById(accountId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            optional -> {
                                if (optional.isEmpty()) return;

                                AccountLoanUnionModel model = optional.get();
                                DebtDetailViewModel viewModel = new ViewModelProvider(this).get(DebtDetailViewModel.class);
                                viewModel.updateAccountLoanUnionModel(model);
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }

        //债务卡片
        AppearanceHelper.setRadius(
                this,
                binding.debtCard,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );
        binding.debtCard.setOnClickListener(view -> {
            DebtDetailViewModel viewModel = new ViewModelProvider(this).get(DebtDetailViewModel.class);
            AccountLoanUnionModel model = viewModel.getAccountLoanUnionModel();
            if (model == null) return;
            if (model.getLoan() == null) {
                Toast.makeText(this, "无法获取借款数据", Toast.LENGTH_SHORT).show();
                return;
            }

            //计算剩余欠款
            Double debt = DebtHelper.computeDebt(model);
            String debtStr;
            if (debt == null) {
                debtStr = "<无法计算>";
            } else {
                debtStr = String.format(Locale.getDefault(), "%.2f元", debt);
            }

            LocalDate repayDate = model.getLoan().getRepayDate();
            String info = String.format(
                    Locale.getDefault(),
                    "截至目前，借款信息如下：\n\n应还日期 **%s**  \n剩余欠款 **%s**\n\n> 本APP计算的数据不一定准确，请以实际约定为准。",
                    repayDate != null ? repayDate.format(CustomDateTimeFormatter.DATE) : "未约定应还日期",
                    debtStr
            );
            new MarkdownDialogBuilder(this, getString(R.string.debt_info), info)
                    .setNegativeButton("关闭", null)
                    .show();
        });
        AppearanceHelper.attachMorphAnimation(binding.debtCard);

        //借款流水记录卡片
        AppearanceHelper.setRadius(
                this,
                binding.loanAccountCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS
        );
        binding.loanAccountCard.setOnClickListener(view -> {
            DebtDetailViewModel viewModel = new ViewModelProvider(this).get(DebtDetailViewModel.class);
            AccountLoanUnionModel model = viewModel.getAccountLoanUnionModel();
            if (model == null) return;

            Bundle bundle = new Bundle();
            bundle.putLong(KeyStrings.RUNNING_ID.v(), model.getAccount().getAccountId());
            Intent intent = new Intent(this, RunningAccountInputActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.loanAccountCard);
    }

    /**
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        //待借款记录的流水记录
        DebtDetailViewModel debtDetailViewModel = new ViewModelProvider(this).get(DebtDetailViewModel.class);
        debtDetailViewModel.getAccountLoanModelLiveData().observe(this, model -> {
            AccountEntity loanAccount = model.getAccount();
            ExtraLoanEntity loan = model.getLoan();
            List<AccountEntity> repaymentList = model.getRepaymentList();

            //债务信息
            if (loan != null) {
                String repayDate = loan.getRepayDate() != null ?
                        loan.getRepayDate().format(CustomDateTimeFormatter.LOCAL_DATE) :
                        "未指定";
                binding.repayDateText.setText(repayDate);

                String remainingDebtStr;
                Double debt = DebtHelper.computeDebt(model);
                if (debt == null) {
                    remainingDebtStr = "<无法计算>";
                } else {
                    remainingDebtStr = debt == 0 ? "已还清" : TextHelper.abbreviate(debt);
                }
                binding.remainingDebtText.setText(remainingDebtStr);
            } else {
                binding.repayDateText.setText(R.string.not_applicable);
                binding.remainingDebtText.setText(R.string.not_applicable);
            }

            //借款流水记录信息
            {
                String type = AccountType.values()[loanAccount.getType()].getTitle();
                String datetime = loanAccount.getDateTime().format(CustomDateTimeFormatter.TIME);
                String remark = loanAccount.getRemark();
                double amount = loanAccount.getAmount();

                //生成自动记账种类字符串
                String autoBookkeepingType;
                int autoTag = loanAccount.getAutoTag();
                if (autoTag == AutoBookkeepingType.NOTIFICATION.ordinal()) {
                    autoBookkeepingType = "通知记账 · ";
                } else if (autoTag == AutoBookkeepingType.ACCESSIBILITY.ordinal()) {
                    autoBookkeepingType = "无障碍记账 · ";
                } else {
                    autoBookkeepingType = "";
                }

                //初始化文本视图
                binding.amountText.setText(TextHelper.abbreviate(amount));
                binding.remarkText.setText(remark.isEmpty() ? "<无备注>" : remark);
                String typeTimeAutoTag = String.format(
                        Locale.getDefault(),
                        "%s%s · %s",
                        autoBookkeepingType, type, datetime
                );
                binding.typeTimeAutoTagText.setText(typeTimeAutoTag);
            }

            //还款记录
            binding.emptyText.setVisibility(repaymentList.isEmpty() ? View.VISIBLE : View.GONE);
            RecyclerView.Adapter<?> adapter = binding.recycler.getAdapter();
            if (adapter instanceof RepaymentAccountListAdapter) {
                ((RepaymentAccountListAdapter) adapter).submitList(repaymentList);
            }
        });
    }
}