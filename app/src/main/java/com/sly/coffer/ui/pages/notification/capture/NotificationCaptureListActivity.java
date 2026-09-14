package com.sly.coffer.ui.pages.notification.capture;

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
import com.sly.coffer.data.save.db.entities.CapturedNotificationEntity;
import com.sly.coffer.data.save.preference.AutoBookKeepingPreference;
import com.sly.coffer.data.save.preference.SearchHistoryPreference;
import com.sly.coffer.databinding.ActivityCapturedNotificationListBinding;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.SearchHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationCaptureListActivity extends AppCompatActivity {
    private ActivityCapturedNotificationListBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private BackPressedCallbackHelper backHelper;   //返回手势拦截器
    private BackPressedCallbackHelper.BackHandler searchBackHandler;    //搜索返回处理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCapturedNotificationListBinding.inflate(getLayoutInflater());

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
        //搜索框
        SearchHelper.initSearchComponents(
                binding.searchBar,
                binding.searchView,
                binding.searchHistoryRecycler,
                binding.clearHistoryBtn,
                SearchHistoryPreference.KEY_CAPTURED_NOTIFICATION,
                keyword -> {
                    CapturedNotificationViewModel viewModel = new ViewModelProvider(this).get(CapturedNotificationViewModel.class);
                    viewModel.executeSearch(keyword.trim());
                },
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_help) {
                        final String EXPLANATION = "### 1. 主要功能\n" +
                                "此界面会显示被捕获的应用通知，您可以根据捕获的通知快速创建通知规则以实现自动记账。\n" +
                                "### 2. 使用方法\n" +
                                "1. 点击某条通知进入规则输入界面；\n" +
                                "2. 输入界面会自动选出通知内容中的数字文本，您需要选择这些数字文本之一作为流水金额；\n" +
                                "3. 选完金额数字和流水类型后点击右上角的确认按钮，创建新通知规则；\n" +
                                "4. 返回上一级界面即可看到新增的通知规则。\n" +
                                "\n" +
                                "### 3. 通知捕获\n" +
                                "- 通知捕获功能依赖安卓的“通知使用权”，使用该功能时请确保权限已授予；\n" +
                                "- 为了节省性能，通知捕获功能将在开启一天后自动关闭，避免频繁保存通知导致性能浪费；\n" +
                                "- 仅会捕获内容中带有数字的通知；\n" +
                                "- 当捕获功能开启时，任何应用发送的通知都会被保存，包括通知标题、内容、应用来源和时间；\n" +
                                "- **捕获的通知仅保存在本地，本APP决不会利用权限窃取您的隐私。**\n";
                        new MarkdownDialogBuilder(this, "功能说明", EXPLANATION)
                                .setNegativeButton("关闭", null)
                                .show();
                        return true;
                    } else if (id == R.id.action_delete_all) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.delete_all_records)
                                .setMessage("此操作将清空所有已捕获的通知记录，确认继续吗？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("确定", (dialogInterface, i) -> {
                                    BookkeepingDb db = BookkeepingDb.getInstance(this);
                                    disposable.add(db.notificationRuleDao().clearCapturedNotification()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(
                                                    () -> Toast.makeText(this, "已删除所有捕获的通知", Toast.LENGTH_SHORT).show(),
                                                    e -> ExceptionHelper.showExceptionDialog(this, e)
                                            )
                                    );
                                })
                                .show();
                    }

                    return false;
                }
        );

        //右下角 FAB
        binding.startStopFab.setOnClickListener(view -> {
            if (AutoBookKeepingPreference.getNotificationCapture(this)) {
                AutoBookKeepingPreference.setNotificationCapture(this, false);
                binding.startStopFab.setImageResource(R.drawable.outline_play_arrow_24);
                Toast.makeText(this, "已关闭通知捕获", Toast.LENGTH_SHORT).show();
            } else {
                if (!PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.isGranted(this)) {
                    Toast.makeText(this, "请开启通知监听服务", Toast.LENGTH_SHORT).show();
                    Intent intent = PermissionHelper.SpecialPermissionType.NOTIFICATION_LISTENER.getIntent(this);
                    startActivity(intent);
                    return;
                }

                AutoBookKeepingPreference.setNotificationCapture(this, true);
                binding.startStopFab.setImageResource(R.drawable.outline_pause_24);
                Toast.makeText(this, "通知捕获已开始，将在24小时后关闭", Toast.LENGTH_SHORT).show();
            }
        });
        AppearanceHelper.attachMorphAnimation(binding.startStopFab);
        AppearanceHelper.setMarginToNavigation(binding.startStopFab, this);
        binding.startStopFab.setImageResource(AutoBookKeepingPreference.getNotificationCapture(this) ?
                R.drawable.outline_pause_24 : R.drawable.outline_play_arrow_24
        );

        //Recycler 列表
        NotificationCaptureListAdapter adapter = new NotificationCaptureListAdapter(
                (entity, anchor) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(KeyStrings.CAPTURED_NOTIFICATION_ID.v(), entity.getNotificationId());

                    Intent intent = new Intent(this, CapturedNotificationRuleInputActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                },
                this::showPopupMenu
        );
        binding.recycler.setAdapter(adapter);
        CapturedNotificationViewModel viewModel = new ViewModelProvider(this).get(CapturedNotificationViewModel.class);
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(viewModel.getCapturedNotificationFlowable(db)
                .subscribe(
                        roleList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, roleList.isEmpty());

                            adapter.submitList(roleList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
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
                CapturedNotificationViewModel viewModel = new ViewModelProvider(NotificationCaptureListActivity.this).get(CapturedNotificationViewModel.class);
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
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        CapturedNotificationViewModel roleListViewModel = new ViewModelProvider(NotificationCaptureListActivity.this).get(CapturedNotificationViewModel.class);
        roleListViewModel.getFilterUpdatedLiveData().observe(this, v ->
                setSearchMode(!roleListViewModel.isNoFilter())
        );
    }

    /**
     * 显示角色长按菜单
     *
     * @param notification 角色实体
     * @param anchor       锚点视图
     */
    private void showPopupMenu(CapturedNotificationEntity notification, View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.menu_captured_notification_edit, popupMenu.getMenu());

        //设置监听
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_captured_notification)
                        .setMessage("即将删除该通知，确认继续吗？")
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            BookkeepingDb db = BookkeepingDb.getInstance(this);
                            disposable.add(db.notificationRuleDao().deleteCapturedNotification(notification)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            () -> Toast.makeText(this, "通知删除成功", Toast.LENGTH_SHORT).show(),
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