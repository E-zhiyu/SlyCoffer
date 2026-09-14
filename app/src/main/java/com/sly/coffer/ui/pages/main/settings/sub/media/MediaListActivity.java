package com.sly.coffer.ui.pages.main.settings.sub.media;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.sly.coffer.R;
import com.sly.coffer.auxiliary.enums.DirectoryPaths;
import com.sly.coffer.auxiliary.enums.KeyStrings;
import com.sly.coffer.auxiliary.enums.LogTags;
import com.sly.coffer.auxiliary.enums.TransitionName;
import com.sly.coffer.databinding.ActivityMediaListBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.helpers.file.MediaHelper;
import com.sly.coffer.ui.others.dialogs.MarkdownDialogBuilder;
import com.sly.coffer.ui.pages.media.FullScreenMediaActivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.zibin.luban.api.Luban;
import top.zibin.luban.api.OnCompressListener;

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
        binding = null;
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
                                            compressImage(entity.getUri())
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

    /**
     * 压缩图片
     *
     * @param uri 待压缩的图片（必须为 file 类型）
     */
    private void compressImage(Uri uri) {
        //判断 Uri 类型
        if (uri == null || !ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            Log.e(LogTags.MEDIA_LIST_ACTIVITY.n(), "Uri类型为空或不为file");
            Toast.makeText(this, "压缩失败", Toast.LENGTH_SHORT).show();
            return;
        }

        //获取输出目录
        File outputDir = DirectoryPaths.DATA_TEMP.getDir(this);
        if (outputDir == null) {
            Log.e(LogTags.MEDIA_LIST_ACTIVITY.n(), "无法获取压缩图片输出目录");
            Toast.makeText(this, "压缩失败", Toast.LENGTH_SHORT).show();
            return;
        }

        //启动图片压缩
        Luban.with(this)
                .load(uri)
                .bindLifecycle(this)
                .setTargetDir(outputDir)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(@NonNull File file) {
                        try {
                            //获取原始文件及其 EXIF 数据
                            File originFile = new File(Objects.requireNonNull(uri.getPath()));
                            Map<String, String> originExifMap = MediaHelper.getExifFromFile(originFile);

                            //判断文件大小
                            long originSize = Files.readAttributes(originFile.toPath(), BasicFileAttributes.class).size();
                            long newSize = Files.readAttributes(file.toPath(), BasicFileAttributes.class).size();
                            if (originSize <= newSize) {
                                FileHelper.clearMediaTempDir(MediaListActivity.this);
                                Log.w(LogTags.MEDIA_LIST_ACTIVITY.n(), "压缩后大小无变化或者更大");
                                Toast.makeText(MediaListActivity.this, "压缩失败：压缩后体积没有变小", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //替换文件并写回 EXIF 数据
                            long originModifiedTime = originFile.lastModified();
                            Path copiedPath = Files.copy(file.toPath(), originFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            boolean isLastModifyTimeRestored = copiedPath.toFile().setLastModified(originModifiedTime);
                            if (!isLastModifyTimeRestored) {
                                Log.w(LogTags.MEDIA_LIST_ACTIVITY.n(), "无法恢复原来的最后编辑时间");
                            }
                            MediaHelper.writeExifToFile(copiedPath.toFile(), originExifMap);

                            //更新列表
                            MediaListViewModel viewModel = new ViewModelProvider(MediaListActivity.this)
                                    .get(MediaListViewModel.class);
                            viewModel.setInOrder(viewModel.isInOrder());

                            //提示并清理
                            Toast.makeText(MediaListActivity.this, "压缩成功", Toast.LENGTH_SHORT).show();
                            FileHelper.clearMediaTempDir(MediaListActivity.this);
                        } catch (IOException e) {
                            FileHelper.clearMediaTempDir(MediaListActivity.this);
                            ExceptionHelper.showExceptionDialog(MediaListActivity.this, e);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        FileHelper.clearMediaTempDir(MediaListActivity.this);
                        Toast.makeText(MediaListActivity.this, "压缩失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .launch();
    }
}