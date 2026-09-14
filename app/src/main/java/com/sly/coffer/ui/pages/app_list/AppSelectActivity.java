package com.sly.coffer.ui.pages.app_list;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.R;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ActivityPackageNameSelectBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AppSelectActivity extends AppCompatActivity {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private AppListAdapter appListAdapter;              //应用列表适配器
    private ActivityPackageNameSelectBinding binding;   //绑定的 XML 视图
    private BackPressedCallbackHelper backHelper;   //返回手势拦截器
    private BackPressedCallbackHelper.BackHandler searchBackHandler;    //搜索返回处理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPackageNameSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //边距设置
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom + AppearanceHelper.dpToPx(this, 10)
            );
            return insets;
        });

        initViews();
        observeLiveData();
        initBackHandlers();
        addPermissionRequests();
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
        //列表视图
        appListAdapter = new AppListAdapter((entity, anchor) -> {
            Intent result2RuleAddActivity = new Intent();
            result2RuleAddActivity.putExtra(KeyStrings.PACKAGE_NAME.v(), entity.getPackageName());
            setResult(Activity.RESULT_OK, result2RuleAddActivity);
            finish();
        });
        binding.recycler.setAdapter(appListAdapter);
        AppListViewModel listViewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        disposable.add(listViewModel.getAppListFlowable(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        appList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, false);
                            if (appList.isEmpty()) {
                                binding.emptyText.setVisibility(View.VISIBLE);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            appListAdapter.submitList(appList);
                        },
                        e -> {
                            ExceptionHelper.showExceptionDialog(this, e);
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, false);
                        }
                )
        );

        //搜索视图
        SearchHelper.initSearchComponents(
                binding.searchBar,
                binding.searchView,
                binding.searchHistoryRecycler,
                binding.clearHistoryBtn,
                SearchHistoryPreference.KEY_APP_LIST,
                keyword -> {
                    VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, true);
                    AppListViewModel viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
                    viewModel.executeSearch(keyword);
                },
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_show_sys_app) {
                        //隐藏加载指示器
                        VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, true);

                        //更新列表
                        AppListViewModel viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
                        boolean toggledStat = !viewModel.isSysAppVisible();
                        item.setChecked(toggledStat);
                        viewModel.toggleSysAppVisibility(toggledStat);

                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        AppListViewModel listViewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        listViewModel.getFilterUpdatedLiveData().observe(this, v ->
                setSearchMode(!listViewModel.isNoFilter())
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
                VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, true);
                AppListViewModel viewModel = new ViewModelProvider(AppSelectActivity.this).get(AppListViewModel.class);
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
     * 添加权限申请
     */
    private void addPermissionRequests() {
        ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissionMap -> {
                    boolean allGranted = true;
                    for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
                        if (!entry.getValue()) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, true);
                        AppListViewModel viewModel1 = new ViewModelProvider(this).get(AppListViewModel.class);
                        viewModel1.executeSearch("");
                    }
                }
        );

        //权限申请器
        PermissionHelper permissionHelper = new PermissionHelper(    //权限申请器
                this,
                requestPermissionLauncher
        );

        permissionHelper.addPermission(
                "com.android.permission.GET_INSTALLED_APPS",
                "请授予应用列表权限以允许APP获取已安装的应用。"
        );
    }

    /**
     * 设置搜索模式
     *
     * @param isSearchMode 是否为搜索模式
     */
    private void setSearchMode(boolean isSearchMode) {
        if (!isSearchMode) {
            binding.searchBar.setText("");
            backHelper.unregisterHandler(searchBackHandler);
        } else {
            backHelper.registerHandler(searchBackHandler);
        }
    }
}