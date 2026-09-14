package com.sly.coffer.ui.pages.accessibility.pick;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ActivityPickedPageListBinding;
import com.sly.coffer.databinding.ViewHolderSeparatorTextChipBinding;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.decoration.sticky.StickyHeaderItemDecoration;
import com.sly.coffer.ui.others.dialogs.EditTextDialogBuilder;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PickedPageListActivity extends AppCompatActivity {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private ActivityPickedPageListBinding binding;  //绑定的 XML 布局
    private BackPressedCallbackHelper backHelper;   //返回手势拦截器
    private BackPressedCallbackHelper.BackHandler searchBackHandler;    //搜索返回处理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPickedPageListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            //RecyclerView
            binding.recycler.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            return insets;
        });

        initViews();
        observeLiveData();
        initBackHandlers();
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
        //搜索框
        SearchHelper.initSearchComponents(
                binding.searchBar,
                binding.searchView,
                binding.searchHistoryRecycler,
                binding.clearHistoryBtn,
                SearchHistoryPreference.KEY_PICKED_VIEW,
                keyword -> {
                    PickedPageViewModel viewModel = new ViewModelProvider(this).get(PickedPageViewModel.class);
                    viewModel.executeSearch(keyword.trim());
                },
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_help) {
                        final String EXPLANATION = "### 1. 工作原理\n" +
                                "当界面拾取功能启用时，本APP会：\n" +
                                "1. 获取当前界面的信息（所属应用的包名、界面名称）；\n" +
                                "2. 将获取到的界面信息保存到数据库，以便输入无障碍规则时选择。\n" +
                                "\n" +
                                "### 2. 拾取的界面的用处\n" +
                                "作为输入无障碍规则时的一个必填选项。\n" +
                                "\n" +
                                "### 3. 免责声明\n" +
                                "- 本功能**完全在本地运行**，无障碍服务仅用于金额提取和记账；\n" +
                                "- **APP不会以任何形式收集、存储或上传您的屏幕内容、金额信息或任何个人数据**；\n" +
                                "- 拾取的所有界面信息仅保存在本机，请您放心使用。";
                        new MarkdownDialogBuilder(this, "功能说明", EXPLANATION)
                                .setNegativeButton("关闭", null)
                                .show();
                        return true;
                    } else if (id == R.id.action_delete_all) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.delete_all_records)
                                .setMessage("此操作将清空所有已记录的界面信息，确认继续吗？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("确定", (dialogInterface, i) -> {
                                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                                    disposable.add(db.accessibilityRuleDao().deleteAllPickedPageCompletable()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(
                                                    () -> Toast.makeText(this, "已删除所有拾取的界面", Toast.LENGTH_SHORT).show(),
                                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                                            )
                                    );
                                })
                                .show();
                    }

                    return false;
                }
        );

        //Recycler 列表
        PickedPageListAdapter adapter = new PickedPageListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.PICKED_PAGE_ID.v(), entity.getId());

                    Intent intent = new Intent(this, PickedPageInputActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                },
                this::showPopupMenu
        );
        binding.recycler.setAdapter(adapter);
        StickyHeaderItemDecoration<ViewHolderSeparatorTextChipBinding> decoration = new StickyHeaderItemDecoration<>(
                adapter,
                ViewHolderSeparatorTextChipBinding::inflate,
                (binding1, data) -> binding1.separatorText.setText(data)
        );
        binding.recycler.addItemDecoration(decoration);
        PickedPageViewModel viewModel = new ViewModelProvider(this).get(PickedPageViewModel.class);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(viewModel.getPickedPageFlowable(db)
                .subscribe(
                        roleList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, roleList.isEmpty());

                            adapter.submitList(roleList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //右下角 FAB
        binding.startStopFab.setOnClickListener(view -> {
            if (AutoBookKeepingPreference.getPagePickStat(this)) {
                AutoBookKeepingPreference.setPagePickStat(this, false);
                binding.startStopFab.setImageResource(R.drawable.outline_play_arrow_24);
                Toast.makeText(this, "已关闭界面拾取", Toast.LENGTH_SHORT).show();
            } else {
                if (!PermissionHelper.SpecialPermissionType.ACCESSIBILITY_PICK.isGranted(this)) {
                    String tip = String.format(
                            Locale.getDefault(),
                            "请开启无障碍中的“%s”服务",
                            getString(R.string.accessibility_title_page_pick)
                    );
                    Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
                    Intent intent = PermissionHelper.SpecialPermissionType.ACCESSIBILITY_PICK.getIntent(this);
                    startActivity(intent);
                    return;
                }

                AutoBookKeepingPreference.setPagePickStat(this, true);
                binding.startStopFab.setImageResource(R.drawable.outline_pause_24);
                Toast.makeText(this, "界面拾取已开始，将在5分钟后关闭", Toast.LENGTH_SHORT).show();
            }
        });
        AppearanceHelper.attachMorphAnimation(binding.startStopFab);
        AppearanceHelper.setMarginToNavigation(binding.startStopFab, this);
        binding.startStopFab.setImageResource(AutoBookKeepingPreference.getPagePickStat(this) ?
                R.drawable.outline_pause_24 : R.drawable.outline_play_arrow_24
        );
    }

    /**
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        PickedPageViewModel roleListViewModel = new ViewModelProvider(PickedPageListActivity.this).get(PickedPageViewModel.class);
        roleListViewModel.getFilterUpdatedLiveData().observe(this, v ->
                setSearchMode(!roleListViewModel.isNoFilter())
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
                PickedPageViewModel viewModel = new ViewModelProvider(PickedPageListActivity.this).get(PickedPageViewModel.class);
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
     * 显示角色长按菜单
     *
     * @param view   角色实体
     * @param anchor 锚点视图
     */
    private void showPopupMenu(PickedPageEntity view, View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.menu_picked_view_list_edit, popupMenu.getMenu());

        //设置监听
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_change_remark) {
                new EditTextDialogBuilder(this, getString(R.string.change_remark), getString(R.string.remark))
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", inputStr -> {
                            PickedPageEntity newPage = new PickedPageEntity(
                                    inputStr.trim(),
                                    view.getPackageName(),
                                    view.getActivityName(),
                                    view.getDateTime()
                            );
                            newPage.setId(view.getId());

                            BookkeepingDb db = BookkeepingDb.getInstance(this);
                            disposable.add(db.accessibilityRuleDao().updatePickedPageCompletable(newPage)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            () -> Toast.makeText(this, "备注修改成功", Toast.LENGTH_SHORT).show(),
                                            e -> ExceptionHelper.showExceptionDialog(this, e)
                                    )
                            );
                        })
                        .show();
            } else if (item.getItemId() == R.id.action_delete) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_picked_page)
                        .setMessage("即将删除该拾取记录，确认继续吗？")
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            BookkeepingDb db = BookkeepingDb.getInstance(this);
                            disposable.add(db.accessibilityRuleDao().deletePickedPageCompletable(view)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            () -> Toast.makeText(this, "拾取记录删除成功", Toast.LENGTH_SHORT).show(),
                                            e -> ExceptionHelper.showExceptionDialog(this, e)
                                    )
                            );
                        })
                        .setNegativeButton("取消", null)
                        .show();

                return true;
            }
            return false;
        });

        popupMenu.show();
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