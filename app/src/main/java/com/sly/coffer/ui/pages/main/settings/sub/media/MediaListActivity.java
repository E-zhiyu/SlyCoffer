package com.sly.coffer.ui.pages.main.settings.sub.media;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.unique.TransitionName;
import com.sly.coffer.auxiliary.interfaces.ImageCompressCallback;
import com.sly.coffer.databinding.ActivityMediaListBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.file.MediaHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.media.FullScreenMediaActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MediaListActivity extends AppCompatActivity {
    private ActivityMediaListBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollView.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

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
        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        //媒体列表
        MediaListAdapter adapter = new MediaListAdapter(
                this,
                (pos, uriList, view) -> {
                    String[] uriStrArray = uriList.stream()
                            .map(Uri::toString)
                            .toArray(String[]::new);

                    //实例化 Intent 并放入数据
                    Intent skip2FullScreen = new Intent(this, FullScreenMediaActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(KeyStrings.FILE_URIS.v(), uriStrArray);
                    bundle.putInt(KeyStrings.VIEW_HOLDER_POSITION.v(), pos);
                    skip2FullScreen.putExtras(bundle);

                    //添加动画并启动
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            view,
                            TransitionName.FULLSCREEN_MEDIA.getS()
                    );
                    startActivity(skip2FullScreen, options.toBundle());
                },
                (entity, anchor) -> {
                    PopupMenu menu = new PopupMenu(this, anchor);
                    menu.getMenuInflater().inflate(R.menu.menu_media_list_long_click, menu.getMenu());

                    //设置监听
                    menu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_compress) {
                            new MarkdownDialogBuilder(
                                    this,
                                    getString(R.string.compress),
                                    "即将压缩图片的体积，这可能导致以下不可逆的结果：\n" +
                                            "\n" +
                                            "1. 图片变得模糊；\n" +
                                            "2. 原本的HDR显示效果丢失；\n" +
                                            "3. 原本的动态照片效果丢失。\n" +
                                            "\n" +
                                            "确认继续吗？"
                            )
                                    .setPositiveButton("确定", (dialogInterface, i) ->
                                            MediaHelper.compressImage(
                                                    entity.getUri(),
                                                    this,
                                                    this,
                                                    new ImageCompressCallback() {
                                                        @Override
                                                        public void failed(String message) {
                                                            Toast.makeText(MediaListActivity.this, message, Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void succeed() {
                                                            //更新列表
                                                            MediaListViewModel viewModel = new ViewModelProvider(MediaListActivity.this)
                                                                    .get(MediaListViewModel.class);
                                                            viewModel.setInOrder(viewModel.isInOrder());

                                                            Toast.makeText(MediaListActivity.this, "压缩成功", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                            )
                                    )
                                    .setNegativeButton("取消", null)
                                    .show();
                            return true;
                        }

                        return false;
                    });

                    menu.show();
                }
        );
        binding.recycler.setAdapter(adapter);
        final int SPAN_COUNT = AppearanceHelper.getScreenHeight(this) > AppearanceHelper.getScreenWidth(this) ?
                4 : 9;
        GridLayoutManager layoutManager = new GridLayoutManager(this, SPAN_COUNT);
        binding.recycler.setLayoutManager(layoutManager);

        //读取文件数据并加载列表
        MediaListViewModel viewModel = new ViewModelProvider(this).get(MediaListViewModel.class);
        disposable.add(viewModel.getMediaFileInfoFlowable(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        infoList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, infoList.isEmpty());
                            adapter.submitList(infoList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //排序菜单按钮
        binding.orderSelectBtn.addOnCheckedChangeListener((materialButton, b) -> {
            if (b) {
                showOrderingMenu();
            }
        });
    }

    /**
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        MediaListViewModel mediaListViewModel = new ViewModelProvider(this).get(MediaListViewModel.class);
        mediaListViewModel.getOrdering().observe(this, ordering ->
                binding.orderLeadingBtn.setText(ordering.getTitle(this))
        );
    }

    /**
     * 显示排序菜单
     */
    private void showOrderingMenu() {
        PopupMenu popupMenu = new PopupMenu(this, binding.orderSelectBtn);
        popupMenu.getMenuInflater().inflate(R.menu.menu_media_list_ordering, popupMenu.getMenu());

        //初始化选中的对象
        MediaListViewModel viewModel = new ViewModelProvider(this).get(MediaListViewModel.class);
        boolean isInOrder = viewModel.isInOrder();
        if (isInOrder) {
            popupMenu.getMenu().getItem(3).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(4).setChecked(true);
        }

        //设置监听
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_order_by_name) {
                viewModel.setOrdering(Ordering.NAME);
                return true;
            } else if (id == R.id.action_order_by_size) {
                viewModel.setOrdering(Ordering.SIZE);
                return true;
            } else if (id == R.id.action_order_by_time) {
                viewModel.setOrdering(Ordering.TIME);
                return true;
            } else if (id == R.id.action_sort_in_order) {
                viewModel.setInOrder(true);
                item.setChecked(true);
                return true;
            } else if (id == R.id.action_sort_in_reverse) {
                viewModel.setInOrder(false);
                item.setChecked(true);
                return true;
            }

            return false;
        });

        popupMenu.setOnDismissListener(menu -> binding.orderSelectBtn.setChecked(false));
        popupMenu.show();
    }
}