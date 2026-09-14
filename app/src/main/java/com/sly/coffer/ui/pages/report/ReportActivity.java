package com.sly.coffer.ui.pages.report;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.types.DateRangeType;
import com.sly.coffer.auxiliary.classes.AmountProportionInfo;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.AccountWithDetailModel;
import com.sly.coffer.databinding.ActivityReportBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;

import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReportActivity extends AppCompatActivity {
    private ActivityReportBinding binding;  //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();
    private ActivityResultLauncher<Intent> filterLauncher;  //启动流水记录过滤界面的启动器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollLayout.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initLaunchers();
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
        //工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        //日期选择按钮
        binding.dateSelectBtn.setOnClickListener(view -> {
            ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
            showDatePickerDialog(viewModel.getRangeType());
        });
        binding.dateSelectBtn.setText(LocalDate.now().format(CustomDateTimeFormatter.LOCAL_DATE));

        //日期范围种类选择按钮
        binding.dateRangeSelectBtn.addOnCheckedChangeListener((materialButton, b) -> {
            if (b) {
                showDateRangeSelectPopupMenu(materialButton);
            }
        });

        //流水记录筛选按钮
        binding.filterBtn.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
            Set<Long> selectedIdSet = viewModel.getIncludedAccountIdProcessor().getValue();
            if (selectedIdSet != null) {
                long[] selectedIds = selectedIdSet.stream()
                        .mapToLong(l -> l)
                        .toArray();
                bundle.putLongArray(KeyStrings.RUNNING_ID.v(), selectedIds);
            }

            Intent intent = new Intent(this, RunningAccountSelectActivity.class);
            intent.putExtras(bundle);
            filterLauncher.launch(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.filterBtn);

        //日期范围和金额卡片
        AppearanceHelper.setRadius(
                this,
                binding.dateCard,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );
        AppearanceHelper.setRadius(
                this,
                binding.amountCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS
        );

        //收支来源
        AmountProportionAdapter expenseAdapter = new AmountProportionAdapter();
        binding.expenseSourceRecycler.setAdapter(expenseAdapter);
        AmountProportionAdapter incomeAdapter = new AmountProportionAdapter();
        binding.incomeSourceRecycler.setAdapter(incomeAdapter);
        ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(viewModel.getRunningAccountDataFlowable(db)  //获取流水记录数据
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        modelList -> {
                            Log.d(LogTags.REPORT_ACTIVITY.n(), "流水数据更新");

                            //收入来源
                            List<AccountWithDetailModel> incomeModelList = modelList.stream()
                                    .filter(model -> model.getAccount().getType() == AccountType.INCOME.ordinal())
                                    .collect(Collectors.toList());
                            Pair<List<AmountProportionInfo>, Double> incomePair = convertToTagProportions(incomeModelList);
                            List<AmountProportionInfo> incomeProportionList = incomePair.first;
                            if (incomeProportionList.isEmpty()) {
                                VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        false,
                                        () -> incomeAdapter.submitList(new ArrayList<>()),
                                        binding.incomeSourceCard
                                );
                            } else {
                                incomeAdapter.submitList(
                                        incomeProportionList,
                                        () -> VisibilityHelper.toggleViewExpansion(
                                                binding.scrollLayout,
                                                true,
                                                null,
                                                binding.incomeSourceCard
                                        )
                                );
                            }

                            //支出来源
                            List<AccountWithDetailModel> expenseModelList = modelList.stream()
                                    .filter(model -> model.getAccount().getType() == AccountType.EXPENSE.ordinal())
                                    .collect(Collectors.toList());
                            Pair<List<AmountProportionInfo>, Double> expensePair = convertToTagProportions(expenseModelList);
                            List<AmountProportionInfo> expenseProportionList = expensePair.first;
                            if (expenseProportionList.isEmpty()) {
                                VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        false,
                                        () -> expenseAdapter.submitList(new ArrayList<>()),
                                        binding.expenseSourceCard
                                );
                            } else {
                                expenseAdapter.submitList(
                                        expenseProportionList,
                                        () -> VisibilityHelper.toggleViewExpansion(
                                                binding.scrollLayout,
                                                true,
                                                null,
                                                binding.expenseSourceCard
                                        )
                                );
                            }

                            //结余、收支文本
                            double income = incomePair.second;
                            double expense = expensePair.second;
                            double balance = income - expense;
                            binding.balanceText.setText(String.format(
                                    Locale.getDefault(),
                                    "%s",
                                    TextHelper.abbreviate(balance, 1)
                            ));
                            binding.incomeAndExpenseText.setText(String.format(
                                    Locale.getDefault(),
                                    "+%s/-%s",
                                    TextHelper.abbreviate(income, 1),
                                    TextHelper.abbreviate(expense, 1)
                            ));
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
        disposable.add(viewModel.processAccountId(BookkeepingDb.getInstance(this))  //处理流水记录编号
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        idList -> {
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //每月结余
        AmountProportionAdapter monthAdapter = new AmountProportionAdapter();
        binding.monthAccountRecycler.setAdapter(monthAdapter);
        disposable.add(viewModel.getMonthAccountDataFlowable(db)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        modelList -> {
                            if (modelList.isEmpty()) {
                                VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        false,
                                        () -> monthAdapter.submitList(new ArrayList<>()),
                                        binding.monthAccountCard
                                );
                            } else {
                                List<AmountProportionInfo> proportionInfoList = convertToMonthProportions(modelList);
                                monthAdapter.submitList(
                                        proportionInfoList,
                                        () -> VisibilityHelper.toggleViewExpansion(
                                                binding.scrollLayout,
                                                true,
                                                null,
                                                binding.monthAccountCard
                                        )
                                );
                            }
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }

    /**
     * 初始化界面启动器
     */
    private void initLaunchers() {
        filterLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (resultCode != Activity.RESULT_OK || result.getData() == null || result.getData().getExtras() == null) {
                        return;
                    }

                    long[] selectedIds = result.getData().getExtras().getLongArray(KeyStrings.RUNNING_ID.v());
                    if (selectedIds == null) {
                        return;
                    }

                    Set<Long> selectedIdSet = Arrays.stream(selectedIds)
                            .boxed()
                            .collect(Collectors.toSet());
                    ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
                    viewModel.updateIncludedAccountId(selectedIdSet);
                }
        );
    }

    /**
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        //日期范围种类
        ReportViewModel reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        reportViewModel.getRangeTypeLiveData().observe(this, type ->
                binding.dateRangeTypeText.setText(type.getTitle())
        );

        //日期范围
        reportViewModel.getDateRangeLiveData().observe(this, rangePair -> {
            LocalDate start = rangePair.first;
            LocalDate end = rangePair.second;

            //切换文本可见性
            int visibility;
            if (start.isEqual(end)) {
                visibility = View.GONE;
            } else {
                visibility = View.VISIBLE;
            }
            binding.toText.setVisibility(visibility);
            binding.endDateText.setVisibility(visibility);

            //设置文本内容
            binding.startDateText.setText(start.format(CustomDateTimeFormatter.DATE_SHORT));
            binding.endDateText.setText(end.format(CustomDateTimeFormatter.DATE_SHORT));
        });
    }

    /**
     * 将 AccountWithDetailModel 列表转换为根据标签分类的比例列表，并返回总金额
     *
     * @param modelList Room 查询出来的原始数据
     * @return 转换后的比例数据列表和总金额
     */
    @NonNull
    @Contract("null -> new")
    private Pair<List<AmountProportionInfo>, Double> convertToTagProportions(List<AccountWithDetailModel> modelList) {
        if (modelList == null || modelList.isEmpty()) {
            return new Pair<>(new ArrayList<>(), 0.0);
        }

        final String OTHERS_NAME = ContextCompat.getString(this, R.string.others);
        Map<String, Double> amountMap = new HashMap<>();
        double totalAmount = 0.0;
        for (AccountWithDetailModel model : modelList) {
            AccountEntity account = model.getAccount();
            List<TagEntity> tagList = model.getTagList();
            double amount = account.getAmount();

            //根据标签分类
            if (!tagList.isEmpty()) {
                for (TagEntity tag : tagList) {
                    String name = tag.getName();
                    Double oldAmount = amountMap.getOrDefault(name, 0.0);
                    if (oldAmount != null) {
                        amountMap.put(name, oldAmount + amount);
                    } else {
                        amountMap.put(name, amount);
                    }
                }
            } else {
                //处理没有被标签标记的流水记录
                Double oldAmount = amountMap.getOrDefault(OTHERS_NAME, 0.0);
                if (oldAmount != null) {
                    amountMap.put(OTHERS_NAME, oldAmount + amount);
                } else {
                    amountMap.put(OTHERS_NAME, amount);
                }
            }
            totalAmount += amount;
        }

        //转换为 AmountProportionInfo 列表
        List<AmountProportionInfo> proportionList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : amountMap.entrySet()) {
            String name = entry.getKey();
            double amount = entry.getValue();

            // 计算百分比（防止分母为 0）
            int percentage = 0;
            if (totalAmount > 0) {
                // 使用 Math.round 四舍五入计算百分比
                percentage = (int) Math.round(amount * 100 / totalAmount);
            }

            proportionList.add(new AmountProportionInfo(percentage, amount, name));
        }
        proportionList.sort(Comparator.comparing(AmountProportionInfo::getAmount).reversed());  //从大到小排序

        return new Pair<>(proportionList, totalAmount);
    }

    /**
     * 转换为每月结余占比
     *
     * @param modelList 从数据库读出的原始数据
     * @return 金额占比数据列表
     */
    @NonNull
    private List<AmountProportionInfo> convertToMonthProportions(@NonNull List<AccountWithDetailModel> modelList) {
        Map<Integer, Double> amountMap = new HashMap<>();
        double totalBalance = 0.0;
        AccountType[] types = AccountType.values();
        for (int i = 1; i <= 12; i++) {
            amountMap.put(i, 0.0);
        }

        //根据月份分类并存放到 Map 中
        for (AccountWithDetailModel model : modelList) {
            AccountEntity account = model.getAccount();
            AccountType type = types[account.getType()];
            double amount = account.getAmount();
            int monthValue = account.getDateTime().getMonthValue();

            if (type.isExpenseType()) {
                Double oldAmount = amountMap.getOrDefault(monthValue, 0.0);
                if (oldAmount != null) {
                    amountMap.put(monthValue, oldAmount - amount);
                } else {
                    amountMap.put(monthValue, -amount);
                }
            } else if (type.isIncomeType()) {
                Double oldAmount = amountMap.getOrDefault(monthValue, 0.0);
                if (oldAmount != null) {
                    amountMap.put(monthValue, oldAmount + amount);
                } else {
                    amountMap.put(monthValue, amount);
                }
            }
            totalBalance += amount;
        }

        //转换为 AmountProportionInfo 列表
        List<AmountProportionInfo> proportionList = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : amountMap.entrySet()) {
            String name = String.format(Locale.getDefault(), "%d月", entry.getKey());
            double amount = entry.getValue();

            // 计算百分比（防止分母为 0）
            int percentage = 0;
            if (totalBalance != 0) {
                // 使用 Math.round 四舍五入计算百分比
                percentage = Math.abs((int) Math.round(amount * 100 / totalBalance));
            }

            proportionList.add(new AmountProportionInfo(percentage, amount, name));
        }

        return proportionList;
    }

    /**
     * 弹出日期选择页
     *
     * @param dateRangeType 日期范围种类，决定了是日期选择对话框还是日期范围选择对话框
     */
    private void showDatePickerDialog(DateRangeType dateRangeType) {
        ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        if (dateRangeType != DateRangeType.CUSTOM) {
            LocalDate selectedDate = viewModel.getSelectedDate();
            DateTimePickerHelper.selectDate(
                    selectedDate,
                    getSupportFragmentManager(),
                    selection -> {
                        //更新 ViewModel 中的选中日期的数据
                        LocalDate selected = DateTimePickerHelper.getLocalDateFromTimeMilli(selection);
                        viewModel.updateSelectedDate(selected);

                        //更新日期选中按钮文本
                        binding.dateSelectBtn.setText(selected.format(CustomDateTimeFormatter.LOCAL_DATE));

                        //更新日期范围
                        Pair<LocalDate, LocalDate> dateRangePair = getDateRangePair(selected, viewModel.getRangeType());
                        viewModel.updateDateRange(dateRangePair);
                    }
            );
        } else {
            Pair<LocalDate, LocalDate> dateRangePair = viewModel.getDateRange();
            DateTimePickerHelper.selectDateRange(
                    dateRangePair.first,
                    dateRangePair.second,
                    getSupportFragmentManager(),
                    this,
                    selection -> {
                        //更新流水日期范围种类
                        viewModel.updateRangeType(DateRangeType.CUSTOM);

                        //更新日期范围
                        LocalDate selectStart = DateTimePickerHelper.getLocalDateFromTimeMilli(selection.first);
                        LocalDate selectEnd = DateTimePickerHelper.getLocalDateFromTimeMilli(selection.second);
                        Pair<LocalDate, LocalDate> selectedDateRangePair = new Pair<>(selectStart, selectEnd);
                        viewModel.updateDateRange(selectedDateRangePair);

                        //更换日期选择按钮的文本
                        binding.dateSelectBtn.setText(R.string.select_date);
                    }
            );
        }
    }

    /**
     * 获取日期范围
     *
     * @param selectedDate 选中的日期
     * @param type         日期范围种类
     * @return 计算得到的日期范围
     */
    @NonNull
    @Contract("_, _ -> new")
    private Pair<LocalDate, LocalDate> getDateRangePair(LocalDate selectedDate, @NonNull DateRangeType type) {
        switch (type) {
            case MONTH:
                LocalDate monthStart = selectedDate.withDayOfMonth(1);
                return new Pair<>(monthStart, monthStart.plusMonths(1));
            case YEAR:
                LocalDate yearStart = selectedDate.withDayOfYear(1);
                return new Pair<>(yearStart, yearStart.plusYears(1));
            case THAT_DAY:
            default:
                return new Pair<>(selectedDate, selectedDate);
        }
    }

    /**
     * 显示日期范围选择的PopupMenu
     *
     * @param anchor PopupMenu 的锚点
     */
    private void showDateRangeSelectPopupMenu(View anchor) {
        ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        PopupMenu dateRangeSelectMenu = new PopupMenu(this, anchor);
        dateRangeSelectMenu.getMenuInflater().inflate(R.menu.popup_menu_date_range_select, dateRangeSelectMenu.getMenu());

        dateRangeSelectMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_today) {
                viewModel.updateRangeType(DateRangeType.THAT_DAY);
                return true;
            } else if (item.getItemId() == R.id.action_this_month) {
                viewModel.updateRangeType(DateRangeType.MONTH);
                return true;
            } else if (item.getItemId() == R.id.action_this_year) {
                viewModel.updateRangeType(DateRangeType.YEAR);
                return true;
            } else if (item.getItemId() == R.id.action_custom) {
                showDatePickerDialog(DateRangeType.CUSTOM);
                return true;
            }

            return false;
        });

        //设置菜单消失监听并显示菜单
        dateRangeSelectMenu.setOnDismissListener(menu -> binding.dateRangeSelectBtn.setChecked(false));
        dateRangeSelectMenu.show();
    }
}