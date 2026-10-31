package com.sly.coffer.ui.pages.common.app_list;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.sly.coffer.R;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ActivityPackageNameSelectBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.permission.PermissionDialogDescription;
import com.sly.coffer.helpers.permission.PermissionHelper;

import java.util.List;

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

        //申请权限
        PermissionHelper.requestPermission(
                this,
                new PermissionDialogDescription(permissionList -> {
                    StringBuilder message = new StringBuilder();

                    //逐个权限添加通知
                    if (XXPermissions.containsPermission(permissionList, PermissionLists.getGetInstalledAppsPermission())) {
                        message.append("请授予应用列表权限以允许APP获取已安装的应用。\n");
                    }

                    return message.toString();
                }),
                new PermissionHelper.PermissionCallback() {
                    @Override
                    public void allGranted(List<IPermission> permissionList) {
                    }

                    @Override
                    public void requested(List<IPermission> grantedList, List<IPermission> deniedList) {
                        boolean noDenied = deniedList.isEmpty();
                        boolean noGranted = grantedList.isEmpty();

                        if (noGranted && !noDenied) {
                            StringBuilder message = new StringBuilder();

                            //逐个权限添加通知
                            if (XXPermissions.containsPermission(deniedList, PermissionLists.getGetInstalledAppsPermission())) {
                                message.append("请授予应用列表权限以允许APP获取已安装的应用。\n");
                            }

                            new MaterialAlertDialogBuilder(AppSelectActivity.this)
                                    .setTitle("请求被拒绝")
                                    .setMessage(message)
                                    .setPositiveButton("前往设置", (dialog, which) ->
                                            XXPermissions.startPermissionActivity(AppSelectActivity.this, deniedList)
                                    )
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else if (noDenied && !noGranted) {
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, true);
                            AppListViewModel viewModel = new ViewModelProvider(AppSelectActivity.this).get(AppListViewModel.class);
                            viewModel.executeSearch("");
                        }
                    }
                },
                PermissionLists.getGetInstalledAppsPermission()
        );
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