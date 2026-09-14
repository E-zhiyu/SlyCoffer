package com.sly.coffer.ui.pages.main.settings.sub;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.auxiliary.enums.settings.RepositoryAddress;
import com.sly.coffer.databinding.ActivityAboutBinding;
import com.sly.coffer.helpers.AboutHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import java.util.Arrays;

public class AboutActivity extends AppCompatActivity {
    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.scrollView.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();
    }

    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        //版本名称文本
        try {
            String versionName = "v" + AboutHelper.getVersionName(this);
            binding.versionNameText.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.versionNameText.setVisibility(View.INVISIBLE);
        }

        //作者卡片
        binding.authorCard.setOnClickListener(view -> {
            SlyCoffer.lockLifecycleObserver();
            Uri uri = Uri.parse("https://github.com/E-zhiyu");
            Intent skip2GitHub = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(skip2GitHub);
        });
        AppearanceHelper.attachMorphAnimation(binding.authorCard);
        AppearanceHelper.setRadiusStyle(binding.authorCard, RadiusStyle.TOP);

        //项目地址卡片
        binding.projectAddressCard.setOnClickListener(view -> showRepositoryAddressDialog());
        AppearanceHelper.attachMorphAnimation(binding.projectAddressCard);
        AppearanceHelper.setRadiusStyle(binding.projectAddressCard, RadiusStyle.BOTTOM);

        //疑难解答卡片
        binding.troubleshootingCard.setOnClickListener(view -> {
            Intent intent = new Intent(this, TroubleshootingActivity.class);
            startActivity(intent);
        });
        AppearanceHelper.attachMorphAnimation(binding.troubleshootingCard);
    }

    /**
     * 显示项目仓库地址对话框
     */
    private void showRepositoryAddressDialog() {
        RepositoryAddress[] addresses = RepositoryAddress.values();
        String[] addressTitles = Arrays.stream(addresses)
                .map(RepositoryAddress::getTitle)
                .toArray(String[]::new);

        new MaterialAlertDialogBuilder(this)
                .setTitle("选择项目仓库")
                .setItems(addressTitles, (dialogInterface, i) -> {
                    SlyCoffer.lockLifecycleObserver();
                    Uri uri = Uri.parse(addresses[i].getAddress());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}