package com.sly.coffer.helpers;

import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.sly.coffer.auxiliary.enums.unique.LogTags;

import java.util.concurrent.Executor;

public class BiometricHelper {
    public interface AuthCallback {
        /**
         * 验证成功回调
         */
        void onSuccess();

        /**
         * 验证出错回调
         *
         * @param errCode 错误代码，详见{@link BiometricPrompt}中的静态错误代码
         * @param errStr  错误提示
         */
        void onError(int errCode, CharSequence errStr);

        /**
         * 验证失败回调
         */
        void onFailed();
    }

    /**
     * 进行身份验证
     *
     * @param activity 需要弹出身份验证对话框的活动界面
     * @param callback 身份验证回调
     */
    public static void showBiometricPrompt(
            @NonNull FragmentActivity activity,
            AuthCallback callback
    ) {
        showBiometricPrompt("您已启用身份验证", "请验证您的身份", activity, callback);
    }

    /**
     * 进行身份验证
     *
     * @param title    对话框标题
     * @param subTitle 对话框副标题
     * @param activity 需要弹出身份验证对话框的活动界面
     * @param callback 身份验证回调
     */
    public static void showBiometricPrompt(
            String title,
            String subTitle,
            @NonNull FragmentActivity activity,
            AuthCallback callback
    ) {
        //获取主线程执行器
        Executor executor = ContextCompat.getMainExecutor(activity);

        //定义验证结果回调
        BiometricPrompt biometricPrompt = getBiometricPrompt(activity, callback, executor);

        BiometricManager biometricManager = BiometricManager.from(activity);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        //显示身份验证对话框
        if (result == BiometricManager.BIOMETRIC_SUCCESS) {
            //配置对话框信息
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subTitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            //开始验证
            biometricPrompt.authenticate(promptInfo);
        } else {
            String errTip = "未知错误，无法使用身份验证";
            if (result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                errTip = "您的设备不支持生物识别";
                Log.e(LogTags.BIOMETRIC_HELPER.n(), "设备不支持生物识别");
            } else if (result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                Log.e(LogTags.BIOMETRIC_HELPER.n(), "硬件忙或不可用");
                errTip = "身份验证不可用，请稍后重试";
            } else if (result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                Log.e(LogTags.BIOMETRIC_HELPER.n(), "用户未设置指纹或锁屏密码");
                errTip = "您还未设置任何锁屏验证方式";
            }
            callback.onError(result, errTip);
        }
    }

    @NonNull
    private static BiometricPrompt getBiometricPrompt(FragmentActivity activity, AuthCallback callback, Executor executor) {
        BiometricPrompt.AuthenticationCallback authCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onFailed();
            }
        };

        // 3. 构建 BiometricPrompt
        return new BiometricPrompt(activity, executor, authCallback);
    }
}
