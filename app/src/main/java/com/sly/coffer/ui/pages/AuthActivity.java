package com.sly.coffer.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.data.save.preference.SecurityPreference;
import com.sly.coffer.databinding.ActivityAuthBinding;
import com.sly.coffer.helpers.BiometricHelper;

import java.util.Locale;

public class AuthActivity extends AppCompatActivity {
    private ActivityAuthBinding binding;            //绑定的 XML 布局
    private int failCount = 0;                      //验证失败次数
    private static final int MAX_FAIL_COUNT = 7;    //最大失败次数
    private static long lastSuccessTimeMillis = 0;  //上次验证成功的时间戳

    public static long getLastSuccessTimeMillis() {
        return lastSuccessTimeMillis;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();

        //拦截返回手势，禁止手动 finish 该 Activity
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onAuthFailed();
            }
        };
        getOnBackPressedDispatcher().addCallback(backPressedCallback);

        //进入界面时显示身份验证对话框
        showBiometric();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        binding.getRoot().setOnClickListener(view -> showBiometric());
    }

    /**
     * 显示 Biometric 对话框并进行身份验证
     */
    private void showBiometric() {
        BiometricHelper.showBiometricPrompt(this, new BiometricHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                lastSuccessTimeMillis = System.currentTimeMillis();

                Toast.makeText(AuthActivity.this, "身份验证成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(int errCode, CharSequence errStr) {
                Log.w(LogTags.AUTH_ACTIVITY.n(), "身份验证出错");

                if (errCode == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                        || errCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                ) {
                    //设备无锁屏/不支持生物验证时关闭身份验证，否则用户可能无法进入APP
                    Toast.makeText(AuthActivity.this, errStr + "，已自动关闭身份验证", Toast.LENGTH_SHORT).show();
                    SecurityPreference.setAuthSwitchStat(AuthActivity.this, false);
                    finish();
                } else {
                    Toast.makeText(AuthActivity.this, errStr, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed() {
                failCount++;

                if (MAX_FAIL_COUNT - failCount <= 3) {
                    String message = String.format(
                            Locale.getDefault(),
                            "您还有%d次重试机会",
                            MAX_FAIL_COUNT - failCount
                    );
                    Toast.makeText(AuthActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                if (failCount >= MAX_FAIL_COUNT) {
                    onAuthFailed();
                }
            }
        });
    }

    /**
     * 身份验证失败回调
     */
    private void onAuthFailed() {
        //强制退出但是保持后台运行
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        // 设置 FLAG_ACTIVITY_NEW_TASK 确保在任意 Context 下都能正常跳转
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}