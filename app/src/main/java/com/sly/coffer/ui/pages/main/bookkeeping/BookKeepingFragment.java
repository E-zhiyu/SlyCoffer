package com.sly.coffer.ui.pages.main.bookkeeping;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.interfaces.RecyclerViewScrollListener;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.daos.AccountDao;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.composite.ui.AccountUiModel;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.ScrollHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.ui.others.bottom.AccountFilterBottomSheet;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderItemDecoration;
import com.sly.coffer.ui.others.viewmodel.AccountFilterViewModel;
import com.sly.coffer.ui.pages.main.MainActivity;
import com.sly.coffer.databinding.FragmentBookkeepingBinding;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import java.time.LocalDate;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BookKeepingFragment extends Fragment {
    private FragmentBookkeepingBinding binding;                         //绑定的XML视图
    private final CompositeDisposable disposable = new CompositeDisposable();  //订阅列表（便于取消订阅）
    private BackPressedCallbackHelper backHelper;                       //返回手势拦截器
    private BackPressedCallbackHelper.BackHandler searchBackHandler;    //搜索返回处理器

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookkeepingBinding.inflate(inflater, container, false);

        initViews();
        initBackHandlers();
        observeLiveData();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        disposable.dispose();

        //清除SearchView的监听器，避免内存泄漏
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).getSearchView().setupWithSearchBar(null);
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //初始化搜索视图
        if (requireActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) requireActivity();
            SearchHelper.initSearchComponents(
                    binding.remarkSearchBar,
                    mainActivity.getSearchView(),
                    mainActivity.getSearchHistoryView(),
                    mainActivity.getClearHistoryBtn(),
                    SearchHistoryPreference.KEY_ACCOUNT_REMARK,
                    keyword -> {
                        AccountFilterViewModel viewModel = new ViewModelProvider(requireActivity()).get(AccountFilterViewModel.class);
                        viewModel.executeSearch(keyword);
                    },
                    item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_filter_account) {
                            AccountFilterBottomSheet bottomSheet = new AccountFilterBottomSheet();
                            bottomSheet.show(getParentFragmentManager(), TagStrings.ACCOUNT_FILTER_BOTTOM.t());
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
                                    getParentFragmentManager(),
                                    "选择跳转到的日期",
                                    selection -> {
                                        LocalDate selectedDate = DateTimePickerHelper.getLocalDateFromTimeMilli(selection);
                                        AccountDao dao = BookkeepingDb.getInstance(requireContext()).accountDao();
                                        disposable.add(dao.getAccountCountAfterDateSingle(selectedDate.plusDays(1))
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeOn(Schedulers.io())
                                                .subscribe(
                                                        count -> scrollToTargetPosition(count, selectedDate),
                                                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                                                )
                                        );
                                    }
                            );
                        }
                        return false;
                    }
            );
        }

        //添加按钮
        binding.addFloatingBtn.setOnClickListener(v -> {
            Intent skip2NewRunningAccount = new Intent(requireContext(), RunningAccountInputActivity.class);
            startActivity(skip2NewRunningAccount);
        });
        AppearanceHelper.attachMorphAnimation(binding.addFloatingBtn);

        //流水列表
        AccountListAdapter adapter = new AccountListAdapter(
                (entity, anchor) -> {
                    long accountId = entity.getAccountId();
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.RUNNING_ID.v(), accountId);

                    Intent skip2AccountInput = new Intent(requireContext(), RunningAccountInputActivity.class);
                    skip2AccountInput.putExtras(bundle);
                    startActivity(skip2AccountInput);
                },
                (entity, anchor) -> {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), anchor, Gravity.END);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_account_list_edit, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.action_delete_account) {
                            deleteAccount(entity);
                            return true;
                        }

                        return false;
                    });

                    popupMenu.show();
                }
        );
        binding.accountRecycler.setAdapter(adapter);
        AccountFilterViewModel viewModel = new ViewModelProvider(requireActivity()).get(AccountFilterViewModel.class);
        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(viewModel.loadAccountListDataFlowable(db)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        accountList -> {
                            int visibility = accountList.isEmpty() ? View.VISIBLE : View.GONE;
                            binding.emptyText.setVisibility(visibility);

                            adapter.submitList(accountList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
        StickyHeaderItemDecoration<ViewHolderSeparatorTextChipBinding> decoration = new StickyHeaderItemDecoration<>(
                adapter,
                ViewHolderSeparatorTextChipBinding::inflate,
                (binding1, data) -> binding1.separatorText.setText(data)
        );
        binding.accountRecycler.addItemDecoration(decoration);
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
        requireActivity().getOnBackPressedDispatcher().addCallback(backPressedCallback);
        backHelper = new BackPressedCallbackHelper(backPressedCallback);

        //搜索
        searchBackHandler = new BackPressedCallbackHelper.BackHandler() {
            @Override
            public boolean handleBack() {
                AccountFilterViewModel viewModel = new ViewModelProvider(requireActivity()).get(AccountFilterViewModel.class);
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
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        AccountFilterViewModel filterViewModel = new ViewModelProvider(requireActivity()).get(AccountFilterViewModel.class);
        filterViewModel.getFilterUpdatedLiveData().observe(getViewLifecycleOwner(), v ->
                setSearchMode(!filterViewModel.isNoFilter())
        );
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
            Toast.makeText(requireContext(), "已跳转至最早的日期", Toast.LENGTH_SHORT).show();
        } else if (targetPosition == 0) {
            Toast.makeText(requireContext(), "已跳转至最晚的日期", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "当天未记账，已跳转至相邻日期", Toast.LENGTH_SHORT).show();
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
                AppearanceHelper.dpToPx(requireContext(), 63),
                new RecyclerViewScrollListener() {
                    @Override
                    public void onSucceed() {
                    }

                    @Override
                    public void onFailed(String errMessage) {
                        Toast.makeText(requireContext(), errMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * 删除流水记录
     *
     * @param account 需要删除的流水记录
     */
    private void deleteAccount(AccountEntity account) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account)
                .setMessage("确认删除该流水记录吗？其包含的媒体文件也会一并删除")
                .setPositiveButton("确定", (dialogInterface, i) ->
                        disposable.add(AccountService.deleteAccount(account, requireContext())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Toast.makeText(requireContext(), "流水记录已删除", Toast.LENGTH_SHORT).show(),
                                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                                )
                        )
                )
                .setNegativeButton("取消", null)
                .show();
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