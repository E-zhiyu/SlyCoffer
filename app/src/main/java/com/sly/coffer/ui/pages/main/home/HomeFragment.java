package com.sly.coffer.ui.pages.main.home;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.AccountDao;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.databinding.FragmentHomeBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.pages.accessibility.pick.PickedPageListActivity;
import com.sly.coffer.ui.pages.accessibility.rule.AccessibilityRuleListActivity;
import com.sly.coffer.ui.pages.budget.BudgetListActivity;
import com.sly.coffer.ui.pages.notification.rule.NotificationRuleListActivity;
import com.sly.coffer.ui.pages.report.ReportActivity;
import com.sly.coffer.ui.pages.tag.TagListActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;                        //XML视图绑定引用
    private final CompositeDisposable disposable = new CompositeDisposable();   //订阅列表
    private final List<String> tipsList = new ArrayList<>();    //提示文本列表

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        initViews();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        disposable.dispose();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //随机提示文本
        binding.randomTipCard.setOnClickListener(view -> showNextRandomTipText());
        AppearanceHelper.attachMorphAnimation(binding.randomTipCard);
        showNextRandomTipText();

        initDateCard();
        initReportCard();
        initTagCard();
        initNotificationRuleCard();
        initBudgetCard();
        initPickedPageCard();
        initAccessibilityRuleCard();
    }

    /**
     * 初始化日期统计卡片
     */
    private void initDateCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.dateCard,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //设置点击监听
        binding.dateCard.setOnClickListener(view -> {
            Intent skip2Report = new Intent(requireContext(), ReportActivity.class);
            startActivity(skip2Report);
        });
        AppearanceHelper.attachMorphAnimation(binding.dateCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        AccountDao dao = db.accountDao();
        disposable.add(dao.getEarliestDateFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        dateOptional -> {
                            LocalDate date = dateOptional.orElse(null);
                            if (date == null) {
                                binding.startDateText.setText(R.string.not_applicable);
                                binding.dateDifferenceText.setText(R.string.not_applicable);
                            } else {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\nEEEE");
                                binding.startDateText.setText(date.format(formatter));

                                //计算时间差
                                long difference = ChronoUnit.DAYS.between(date, LocalDate.now());
                                binding.dateDifferenceText.setText(String.format(
                                        Locale.getDefault(),
                                        "%d天",
                                        difference
                                ));
                            }
                        },
                        e -> {
                            binding.startDateText.setText(R.string.not_applicable);
                            binding.dateDifferenceText.setText(R.string.not_applicable);
                            ExceptionHelper.showExceptionDialog(requireContext(), e);
                        }
                )
        );
    }

    /**
     * 初始化报表卡片
     */
    private void initReportCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.reportCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //设置点击监听
        binding.reportCard.setOnClickListener(view -> {
            Intent skip2Report = new Intent(requireContext(), ReportActivity.class);
            startActivity(skip2Report);
        });
        AppearanceHelper.attachMorphAnimation(binding.reportCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        AccountDao dao = db.accountDao();
        disposable.add(dao.getAccountInDateRange(LocalDate.now(), LocalDate.now().plusDays(1))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        accountList -> {
                            double balance = 0;
                            double expense = 0;
                            double income = 0;

                            //计算收支
                            AccountType[] types = AccountType.values();
                            for (AccountEntity account : accountList) {
                                AccountType type = types[account.getType()];
                                double amount = account.getAmount();
                                if (type.isExpenseType()) {
                                    balance -= amount;
                                    expense += amount;
                                } else if (type.isIncomeType()) {
                                    balance += amount;
                                    income += amount;
                                }
                            }


                            //结余
                            binding.todayBalanceText.setText(String.format(
                                    Locale.getDefault(),
                                    "%s",
                                    TextHelper.abbreviate(balance)
                            ));

                            //收支
                            binding.todayIncomeAndExpenseText.setText(String.format(
                                    Locale.getDefault(),
                                    "+%s/-%s",
                                    TextHelper.abbreviate(income),
                                    TextHelper.abbreviate(expense)
                            ));
                        },
                        e -> {
                            binding.todayBalanceText.setText(R.string.not_applicable);
                            binding.todayIncomeAndExpenseText.setText(R.string.not_applicable);
                            ExceptionHelper.showExceptionDialog(requireContext(), e);
                        }
                )
        );
    }

    /**
     * 初始化标签卡片
     */
    private void initTagCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.tagCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //设置点击监听
        binding.tagCard.setOnClickListener(view -> {
            Intent skip2TagList = new Intent(requireContext(), TagListActivity.class);
            startActivity(skip2TagList);
        });
        AppearanceHelper.attachMorphAnimation(binding.tagCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.tagDao().getTagCountFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        count -> binding.tagCountText.setText(String.valueOf(count)),
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }

    /**
     * 初始化通知规则卡片
     */
    private void initNotificationRuleCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.notificationRuleCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //点击监听
        binding.notificationRuleCard.setOnClickListener(view -> {
            Intent skip2NotificationRuleList = new Intent(requireContext(), NotificationRuleListActivity.class);
            startActivity(skip2NotificationRuleList);
        });
        AppearanceHelper.attachMorphAnimation(binding.notificationRuleCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.notificationRuleDao().getNotificationRuleCountFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        count -> binding.notificationRuleCountText.setText(String.valueOf(count)),
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }

    /**
     * 初始化预算卡片
     */
    private void initBudgetCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.budgetCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //点击监听
        binding.budgetCard.setOnClickListener(view -> {
            Intent skip2BudgetList = new Intent(requireContext(), BudgetListActivity.class);
            startActivity(skip2BudgetList);
        });
        AppearanceHelper.attachMorphAnimation(binding.budgetCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.budgetDao().getBudgetCountFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        count -> binding.budgetCountText.setText(String.valueOf(count)),
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }

    /**
     * 初始化拾取视图卡片
     */
    private void initPickedPageCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.pickedPageCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS
        );

        //点击监听
        binding.pickedPageCard.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), PickedPageListActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.pickedPageCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.accessibilityRuleDao().getPickedPageCountFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        count -> binding.pickedPageCountText.setText(String.valueOf(count)),
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }

    /**
     * 初始化无障碍规则卡片
     */
    private void initAccessibilityRuleCard() {
        //设置卡片圆角
        AppearanceHelper.setRadius(
                requireContext(),
                binding.accessibilityRuleCard,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.SMALL_CARD_RADIUS,
                AppearanceHelper.MEDIUM_CARD_RADIUS
        );

        //点击监听
        binding.accessibilityRuleCard.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), AccessibilityRuleListActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.accessibilityRuleCard);

        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.accessibilityRuleDao().getAccessibilityRuleCountFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        count -> binding.accessibilityRuleCountText.setText(String.valueOf(count)),
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }

    /**
     * 显示下一个随机的提示文本
     */
    private void showNextRandomTipText() {
        //如果提示文本列表为空，则重新获取提示文本资源
        if (tipsList.isEmpty()) {
            String[] tipsArray = getResources().getStringArray(R.array.tips_array);
            tipsList.addAll(Arrays.stream(tipsArray).collect(Collectors.toList()));

            //添加小米专属的提示文本
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            if (manufacturer.contains("xiaomi")) {
                String[] xiaomiTips = getResources().getStringArray(R.array.xiaomi_tips);
                tipsList.addAll(Arrays.stream(xiaomiTips).collect(Collectors.toList()));
            }
        }

        //获取随机下标
        Random random = new Random();
        int randomNum = random.nextInt();
        if (randomNum < 0) {
            randomNum = -randomNum;
        }
        int tipIndex = randomNum % tipsList.size();

        //设置高度变化的动画
        TransitionSet set = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .setInterpolator(new FastOutSlowInInterpolator())
                .addTransition(new ChangeBounds())
                .setDuration(250);
        TransitionManager.beginDelayedTransition(binding.scrollViewLayout, set);

        //显示对应的文本
        String tip = "tip : " + tipsList.get(tipIndex);
        binding.randomTipText.setText(tip);

        //删除刚刚显示的文本防止重复
        tipsList.remove(tipIndex);
    }
}