package com.sly.coffer.ui.pages.media;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.sly.coffer.databinding.ActivityFullScreenMediaBinding;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.helpers.file.MediaHelper;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FullScreenMediaActivity extends AppCompatActivity {
    private ActivityFullScreenMediaBinding binding;   //绑定的 XML 布局
    @Nullable
    private Bundle initBundle = null;       //传递初始化数据的数据包
    private final CompositeDisposable disposable = new CompositeDisposable();   //任务订阅列表
    @Nullable
    private ViewPager2.OnPageChangeCallback pageChangeCallback = null;  // ViewPager2 的翻页监听器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMediaBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        initBundle = getIntent().getExtras();
        initViews();
        getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                //动画播放完毕后隐藏状态栏
                hideSystemUI();
            }
        });

        //设置返回监听
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        };
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (pageChangeCallback != null) {
            binding.viewPager2.unregisterOnPageChangeCallback(pageChangeCallback);
            pageChangeCallback = null;
        }

        disposable.dispose();
        binding = null;
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //翻页视图
        String[] mediaUriStrings = initBundle != null ? initBundle.getStringArray(KeyStrings.FILE_URIS.v()) : null;
        FullScreenMediaAdapter adapter = new FullScreenMediaAdapter(mediaUriStrings);
        binding.viewPager2.setAdapter(adapter);
        List<Uri> mediaUriList;
        if (mediaUriStrings != null) {
            mediaUriList = Arrays.stream(mediaUriStrings)
                    .map(Uri::parse)
                    .collect(Collectors.toList());
        } else {
            mediaUriList = new ArrayList<>();
        }
        adapter.submitList(mediaUriList);
        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                //设置 HDR 显示效果
                if (position >= 0 && position < mediaUriList.size()) {
                    Uri mediaUri = mediaUriList.get(position);
                    disposable.add(MediaHelper.isHdrImage(FullScreenMediaActivity.this, mediaUri)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    isHdr -> setHDR(isHdr),
                                    e -> ExceptionHelper.showExceptionDialog(FullScreenMediaActivity.this, e)
                            )
                    );
                } else {
                    setHDR(false);
                }
            }
        };
        binding.viewPager2.registerOnPageChangeCallback(pageChangeCallback);

        //设置初始位置
        int startIndex = initBundle.getInt(KeyStrings.VIEW_HOLDER_POSITION.v(), 0);
        binding.viewPager2.setCurrentItem(startIndex, false);

        //保存媒体
        binding.saveMediaBtn.setOnClickListener(v -> savePicture());
        AppearanceHelper.attachMorphAnimation(binding.saveMediaBtn);

        //分享媒体
        binding.shareMediaBtn.setOnClickListener(v -> sharePicture());
        AppearanceHelper.attachMorphAnimation(binding.shareMediaBtn);

        //图片详情
        binding.infoBtn.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            String currentUriStr = "";
            if (mediaUriStrings != null) {
                currentUriStr = mediaUriStrings[binding.viewPager2.getCurrentItem()];
            }
            bundle.putString(KeyStrings.FILE_URIS.v(), currentUriStr);

            Intent intent = new Intent(this, MediaInfoActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.infoBtn);
    }

    /**
     * 设置全屏，隐藏状态栏
     */
    private void hideSystemUI() {
        Window window = getWindow();

        // 强制让临时滑出的系统栏图标和文字变成白色，确保在黑底大图上清晰可见
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        // 确保当前 Window 铺满全屏
        WindowCompat.setDecorFitsSystemWindows(window, false);

        //根据不同 API 使用不同的方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android 11 (API 30) 及以上的全新标准写法
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                //隐藏状态栏
                controller.hide(WindowInsets.Type.statusBars());
                controller.hide(WindowInsets.Type.navigationBars());

                //设置状态栏行为：用户从边缘滑动手势时，临时半透明拉出系统栏，不影响应用本身的布局
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            //针对 Android 10 及以下老系统的兼容写法
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // 隐藏状态栏
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 粘性沉浸
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 保存图片到系统相册
     */
    private void savePicture() {
        int currentIndex = binding.viewPager2.getCurrentItem();
        String[] mediaUriStrings = initBundle != null ? initBundle.getStringArray(KeyStrings.FILE_URIS.v()) : null;
        if (mediaUriStrings != null) {
            Uri currentUri = Uri.parse(mediaUriStrings[currentIndex]);
            disposable.add(MediaHelper.saveMediaToGalleryObservable(this, currentUri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            uri -> Toast.makeText(this, "媒体文件已保存至相册", Toast.LENGTH_SHORT).show(),
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }
    }

    /**
     * 分享图片
     */
    private void sharePicture() {
        int currentIndex = binding.viewPager2.getCurrentItem();
        String[] mediaUriStrings = initBundle != null ? initBundle.getStringArray(KeyStrings.FILE_URIS.v()) : null;
        if (mediaUriStrings != null) {
            Uri currentUri = Uri.parse(mediaUriStrings[currentIndex]);
            File pictureFile = new File(Objects.requireNonNull(currentUri.getPath()));
            String extension = MimeTypeMap.getFileExtensionFromUrl(currentUri.toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            disposable.add(FileHelper.shareFileCompletable(this, pictureFile, mimeType)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> Log.i(LogTags.FULL_SCREEN_MEDIA_ACTIVITY.n(), "调用分享API成功"),
                            e -> {
                                ExceptionHelper.showExceptionDialog(this, e);
                                Log.e(LogTags.FULL_SCREEN_MEDIA_ACTIVITY.n(), "无法调用分享API");
                            }
                    )
            );
        }
    }

    /**
     * 设置 HDR 显示效果
     *
     * @param isHDR 是否为 HDR 显示状态
     */
    private void setHDR(boolean isHDR) {
        //色彩范围
        int colorMode = isHDR ? ActivityInfo.COLOR_MODE_HDR : ActivityInfo.COLOR_MODE_DEFAULT;
        getWindow().setColorMode(colorMode);

        // HDR 提示
        VisibilityHelper.toggleVisibilityWithFade(binding.hdrTextCard, isHDR);
    }
}