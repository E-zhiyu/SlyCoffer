package com.sly.coffer.ui.pages.media;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.databinding.ActivityMediaInfoBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.TextHelper;
import com.sly.coffer.helpers.file.MediaHelper;

import java.time.LocalDateTime;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MediaInfoActivity extends AppCompatActivity {
    private ActivityMediaInfoBinding binding;   //绑定的 XML 布局
    @Nullable
    private Bundle initBundle;                  //包含初始化数据的数据包
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaInfoBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollView.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initBundle = getIntent().getExtras();
        initViews();
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

        //获取文件信息
        String uriStr = initBundle != null ? initBundle.getString(KeyStrings.FILE_URIS.v()) : "";
        disposable.add(Single.fromCallable(() -> {
                            Uri uri = Uri.parse(uriStr);
                            return MediaHelper.getMediaDetail(this, uri);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                detail -> {
                                    //时间信息
                                    LocalDateTime time = detail.getTime();
                                    if (time != null) {
                                        binding.timeText.setText(time.format(CustomDateTimeFormatter.DATE_TIME_MEDIA_DETAIL));
                                    } else {
                                        binding.timeText.setText("未知");
                                    }

                                    //文件信息
                                    String fileName = detail.getFileName();
                                    binding.fileNameText.setText(fileName);
                                    long fileSize = detail.getSize();
                                    int width = detail.getWidth();
                                    int height = detail.getHeight();
                                    String sizeDisplay = String.format(
                                            Locale.getDefault(),
                                            "%s  %d×%dpx",
                                            TextHelper.shortenFileSize(fileSize),
                                            width,
                                            height
                                    );
                                    binding.sizeText.setText(sizeDisplay);

                                    //拍摄设备
                                    String device = detail.getDevice();
                                    if (!device.isEmpty()) {
                                        binding.mediaParamInfoLayout.setVisibility(View.VISIBLE);

                                        //拍摄设备
                                        binding.deviceText.setText(detail.getDevice());

                                        //拍摄参数1
                                        double shutter = detail.getShutter();
                                        int iso = detail.getIso();
                                        double aperture = detail.getAperture();
                                        String shutterDisplay;
                                        if (shutter < 1.0 && shutter > 0) {
                                            shutterDisplay = "1/" + Math.round(1.0 / shutter);
                                        } else {
                                            shutterDisplay = String.valueOf(shutter);
                                        }
                                        String param1Display = String.format(
                                                Locale.getDefault(),
                                                "f/%.2f  %ss  ISO%d",
                                                aperture, shutterDisplay, iso
                                        );
                                        binding.params1Text.setText(param1Display);

                                        //拍摄参数2
                                        double focalLength = detail.getFocalLength();
                                        boolean isFlashUsed = detail.isFlashUsed();
                                        String param2Display = String.format(
                                                Locale.getDefault(),
                                                "%.2fmm  %s",
                                                focalLength,
                                                isFlashUsed ? "使用了闪光灯" : "未使用闪光灯"
                                        );
                                        binding.params2Text.setText(param2Display);
                                    }
                                },
                                e -> ExceptionHelper.showExceptionDialog(this, e)
                        )
        );
    }
}