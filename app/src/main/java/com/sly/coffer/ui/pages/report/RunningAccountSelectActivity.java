package com.sly.coffer.ui.pages.report;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.KeyStrings;
import com.sly.coffer.auxiliary.enums.TagStrings;
import com.sly.coffer.auxiliary.interfaces.RecyclerViewScrollListener;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.AccountDao;
import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ActivityRunningAccountSelectBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.ScrollHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.ui.others.bottom.AccountFilterBottomSheet;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderItemDecoration;
import com.sly.coffer.ui.others.selections.account.AccountKeyProvider;
import com.sly.coffer.ui.others.selections.account.AccountLookup;
import com.sly.coffer.ui.others.viewmodel.AccountFilterViewModel;
import com.sly.coffer.ui.pages.main.bookkeeping.AccountListAdapter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RunningAccountSelectActivity extends AppCompatActivity {
    private ActivityRunningAccountSelectBinding binding;                        //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();   //订阅列表（便于取消订阅）
    @Nullable
    private Bundle initBundle = null;                                           //包含初始数据的数据包
    private BackPressedCallbackHelper backHelper;                               //返回手势拦截器
    private BackPressedCallbackHelper.BackHandler searchBackHandler;            //搜索返回处理器
    private SelectionTracker<Long> selectionTracker;                            //选择追踪器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRunningAccountSelectBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        initBundle = getIntent().getExtras();
        initViews();
        initBackHandlers();
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
        //初始化搜索视图
        SearchHelper.initSearchComponents(
                binding.remarkSearchBar,
                binding.searchView,
                binding.searchHistoryRecycler,
                binding.clearHistoryBtn,
                SearchHistoryPreference.KEY_ACCOUNT_REMARK,
                keyword -> {
                    AccountFilterViewModel viewModel = new ViewModelProvider(this).get(AccountFilterViewModel.class);
                    viewModel.executeSearch(keyword);
                },
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_filter_account) {
                        AccountFilterBottomSheet bottomSheet = new AccountFilterBottomSheet();
                        bottomSheet.show(getSupportFragmentManager(), TagStrings.ACCOUNT_FILTER_BOTTOM.t());
                        return true;
                    } else if (id == R.id.action_skip_date) {
                        //获取初始日期
                        LocalDate initDate = null;
                        if (binding.accountRecycler.getLayoutManager() instanceof LinearLayoutManager) {
                            int firstViewPosition = ((LinearLayoutManager) binding.accountRecycler.getLayoutManager())
                                    .findFirstVisibleItemPosition();
                            if (binding.accountRecycler.getAdapter() instanceof AccountListAdapter &&
                                    firstViewPosition != RecyclerView.NO_POSITION) {
                                AccountUiModel model = ((AccountListAdapter) binding.accountRecycler.getAdapter())
                                        .getCurrentList().get(firstViewPosition);
                                if (model instanceof AccountUiModel.Item) {
                                    initDate = ((AccountUiModel.Item) model).entity.getDateTime().toLocalDate();
                                } else if (model instanceof AccountUiModel.Separator) {
                                    initDate = LocalDate.parse(((AccountUiModel.Separator) model).text, CustomDateTimeFormatter.DATE_WITH_WEEK);
                                }
                            }
                        }

                        //弹出日期选择对话框
                        DateTimePickerHelper.selectDate(
                                initDate,
                                getSupportFragmentManager(),
                                "选择跳转到的日期",
                                selection -> {
                                    LocalDate selectedDate = DateTimePickerHelper.getLocalDateFromTimeMilli(selection);
                                    AccountDao dao = BookkeepingDb.getInstance(this).accountDao();
                                    disposable.add(dao.getAccountCountAfterDateSingle(selectedDate.plusDays(1))
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(
                                                    count -> scrollToTargetPosition(count, selectedDate),
                                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                                            )
                                    );
                                }
                        );
                    } else if (id == R.id.action_clear_selection) {
                        selectionTracker.clearSelection();
                    }

                    return false;
                }
        );

        //确认按钮
        binding.confirmBtn.setOnClickListener(v -> {
            Selection<Long> selection = selectionTracker.getSelection();
            long[] ids = new long[selection.size()];
            Iterator<Long> iterator = selection.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                ids[index++] = iterator.next();
            }

            Intent result = new Intent();
            Bundle bundle = new Bundle();
            bundle.putLongArray(KeyStrings.RUNNING_ID.v(), ids);
            result.putExtras(bundle);
            setResult(Activity.RESULT_OK, result);
            finish();
        });
        AppearanceHelper.attachMorphAnimation(binding.confirmBtn);

        initRecycler();
    }

    /**
     * 初始化流水记录列表
     */
    private void initRecycler() {
        //RecyclerView
        AccountSelectListAdapter adapter = new AccountSelectListAdapter();
        binding.accountRecycler.setAdapter(adapter);
        StickyHeaderItemDecoration<ViewHolderSeparatorTextChipBinding> decoration = new StickyHeaderItemDecoration<>(
                adapter,
                ViewHolderSeparatorTextChipBinding::inflate,
                (binding1, data) -> binding1.separatorText.setText(data)
        );
        binding.accountRecycler.addItemDecoration(decoration);

        //多选追踪器
        selectionTracker = new SelectionTracker.Builder<>(
                TagStrings.ACCOUNT_SELECTION.t(),
                binding.accountRecycler,
                new AccountKeyProvider(adapter),
                new AccountLookup(binding.accountRecycler),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
                new SelectionTracker.SelectionPredicate<>() {
                    @Override
                    public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                        if (key < 0) return false;

                        boolean isItem = false;
                        List<AccountUiModel> currentList = adapter.getCurrentList();
                        for (int i = 0; i < currentList.size(); i++) {
                            AccountUiModel item = currentList.get(i);
                            if ((item instanceof AccountUiModel.Item) && ((AccountUiModel.Item) item).entity.getAccountId() == key) {
                                isItem = true;
                                break;
                            }
                        }
                        return isItem;
                    }

                    @Override
                    public boolean canSetStateAtPosition(int position, boolean nextState) {
                        try {
                            return adapter.getCurrentList().get(position) instanceof AccountUiModel.Item;
                        } catch (IndexOutOfBoundsException e) {
                            return false;
                        }
                    }

                    @Override
                    public boolean canSelectMultiple() {
                        return true;
                    }
                }
        ).build();
        adapter.setSelectionTracker(selectionTracker);

        //初始化数据
        AccountFilterViewModel viewModel = new ViewModelProvider(this).get(AccountFilterViewModel.class);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(viewModel.loadAccountListDataFlowable(db)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        accountList -> {
                            int visibility = accountList.isEmpty() ? View.VISIBLE : View.GONE;
                            binding.emptyText.setVisibility(visibility);

                            adapter.submitList(accountList);

                            if (initBundle != null) {
                                long[] selectedIds = initBundle.getLongArray(KeyStrings.RUNNING_ID.v());
                                if (selectedIds != null) {
                                    List<Long> selectedIdList = Arrays.stream(selectedIds)
                                            .boxed()
                                            .collect(Collectors.toList());
                                    selectionTracker.setItemsSelected(selectedIdList, true);
                                }
                            }
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );
    }

    /**
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        AccountFilterViewModel filterViewModel = new ViewModelProvider(this).get(AccountFilterViewModel.class);
        filterViewModel.getFilterUpdatedLiveData().observe(this, v ->
                setSearchMode(!filterViewModel.isNoFilter())
        );
    }

    /**
     * 初始化返回手势拦截器
     */
    private void initBackHandlers() {
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                backHelper.dispatchBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
        backHelper = new BackPressedCallbackHelper(backPressedCallback);

        //搜索
        searchBackHandler = new BackPressedCallbackHelper.BackHandler() {
            @Override
            public boolean handleBack() {
                AccountFilterViewModel viewModel = new ViewModelProvider(RunningAccountSelectActivity.this).get(AccountFilterViewModel.class);
                viewModel.clearFilter();
                return true;
            }

            @Override
            public int getPriority() {
                return 1;
            }
        };
    }

    /**
     * 跳转到指定位置
     *
     * @param targetPosition 目标下标，实际为大于目标日期的日记数量
     * @param targetDate     希望跳转到的日期
     */
    private void scrollToTargetPosition(int targetPosition, LocalDate targetDate) {
        //判断位置是否在有效范围内
        RecyclerView.Adapter<?> adapter = binding.accountRecycler.getAdapter();
        if (adapter == null || targetPosition >= adapter.getItemCount()) {
            targetPosition -= 1;    //防止超出范围
            Toast.makeText(this, "已跳转至最早的日期", Toast.LENGTH_SHORT).show();
        } else if (targetPosition == 0) {
            Toast.makeText(this, "已跳转至最晚的日期", Toast.LENGTH_SHORT).show();
        } else {
            //判断跳转到的位置是否是目标日期
            if (adapter instanceof AccountListAdapter) {
                AccountUiModel model = ((AccountListAdapter) adapter).getCurrentList().get(targetPosition);
                LocalDate exactDate = null;
                if (model instanceof AccountUiModel.Item) {
                    exactDate = ((AccountUiModel.Item) model).entity.getDateTime().toLocalDate();
                } else if (model instanceof AccountUiModel.Separator) {
                    exactDate = LocalDate.parse(((AccountUiModel.Separator) model).text, CustomDateTimeFormatter.DATE_WITH_WEEK);
                }
                if (exactDate == null || !exactDate.isEqual(targetDate)) {
                    Toast.makeText(this, "当天未记账，已跳转至相邻日期", Toast.LENGTH_SHORT).show();
                }
            }
        }

        //折叠 AppBarLayout
        binding.appBarLayout.setExpanded(false);

        //滚动列表视图
        ScrollHelper.scrollRecycler(
                binding.accountRecycler,
                (LinearLayoutManager) binding.accountRecycler.getLayoutManager(),
                targetPosition,
                30,
                AppearanceHelper.dpToPx(this, 63),
                new RecyclerViewScrollListener() {
                    @Override
                    public void onSucceed() {
                    }

                    @Override
                    public void onFailed(String errMessage) {
                        Toast.makeText(RunningAccountSelectActivity.this, errMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * 设置搜索模式
     *
     * @param isSearchMode 是否为搜索模式
     */
    private void setSearchMode(boolean isSearchMode) {
        if (!isSearchMode) {
            binding.remarkSearchBar.setText("");
            backHelper.unregisterHandler(searchBackHandler);
        } else {
            backHelper.registerHandler(searchBackHandler);
        }
    }
}