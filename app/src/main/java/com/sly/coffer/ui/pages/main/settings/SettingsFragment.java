package com.sly.coffer.ui.pages.main.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.auxiliary.enums.RadiusStyle;
import com.sly.coffer.data.save.preference.MediaPreference;
import com.sly.coffer.data.save.preference.SecurityPreference;
import com.sly.coffer.data.save.preference.VersionPreference;
import com.sly.coffer.databinding.FragmentSettingsBinding;
import com.sly.coffer.auxiliary.enums.settings.AuthOpportunity;
import com.sly.coffer.auxiliary.enums.settings.FirstScreen;
import com.sly.coffer.auxiliary.enums.settings.ThemeMode;
import com.sly.coffer.helpers.BiometricHelper;
import com.sly.coffer.helpers.UpdateHelper;
import com.sly.coffer.helpers.AboutHelper;
import com.sly.coffer.ui.pages.main.settings.sub.AboutActivity;
import com.sly.coffer.ui.pages.main.settings.sub.AutoBookkeepingActivity;
import com.sly.coffer.ui.pages.main.settings.sub.DataManageActivity;
import com.sly.coffer.ui.pages.main.settings.sub.PermissionManageActivity;
import com.sly.coffer.helpers.appearence.ThemeHelper;
import com.sly.coffer.data.save.preference.AppSettingsPreference;
import com.sly.coffer.ui.pages.main.settings.components.SettingClickableTextView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSpinnerView;
import com.sly.coffer.ui.pages.main.settings.components.SettingSwitchView;
import com.sly.coffer.ui.pages.main.settings.sub.media.MediaListActivity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;


public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;                                        //绑定的XML视图
    private final CompositeDisposable disposable = new CompositeDisposable();       //多线程任务列表

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        initViews();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.dispose();
        binding = null;
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        initAppSettings();
        initCommonSettings();
        initMediaSettings();
        initPrivacySettings();
        initAboutSettings();
    }

    /**
     * 初始化应用设置部分
     */
    private void initAppSettings() {
        //主题模式
        SettingClickableTextView themeModeOption = new SettingClickableTextView(
                requireContext(),
                binding.themeModeOption,
                R.string.theme_mode,
                "切换深浅色模式",
                R.drawable.outline_dark_mode_24,
                RadiusStyle.TOP
        );
        themeModeOption.setFunctionListener(v -> showThemeModeSelectDialog());

        //动态配色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            SettingSwitchView dynamicColorOption = new SettingSwitchView(
                    requireContext(),
                    binding.dynamicColorOption,
                    R.string.dynamic_color,
                    "将壁纸颜色作为APP主题色",
                    R.drawable.outline_colorize_24,
                    RadiusStyle.MIDDLE
            );
            dynamicColorOption.setChecked(AppSettingsPreference.getDynamicColorStat(requireContext()));
            dynamicColorOption.setFunctionListener(
                    (buttonView, isChecked) -> {
                        AppSettingsPreference.setDynamicColorStat(requireContext(), isChecked);
                        ThemeHelper.switchDynamicColorWithAnimation(requireActivity(), isChecked);
                    }
            );
        } else {
            binding.dynamicColorOption.getRoot().setVisibility(View.GONE);
        }

        //首页选项
        SettingSpinnerView firstScreenOption = new SettingSpinnerView(
                requireContext(),
                binding.firstScreenOption,
                R.string.select_first_screen,
                "选择启动的第一屏",
                R.drawable.outline_mobile_24,
                RadiusStyle.MIDDLE
        );
        int screenCode = AppSettingsPreference.getFirstScreen(requireContext());
        firstScreenOption.setSpinnerText(FirstScreen.values()[screenCode].getTitle());
        firstScreenOption.setFunctionListener(v -> {
            PopupMenu firstScreenMenu = new PopupMenu(requireContext(), firstScreenOption.getFunctionComponent());

            //填充菜单选项
            for (FirstScreen firstScreen : FirstScreen.values()) {
                int groupId = firstScreen.getGroupId();
                int itemId = firstScreen.getItemId();
                int order = firstScreen.getOrder();
                String title = firstScreen.getTitle();
                firstScreenMenu.getMenu().add(groupId, itemId, order, title);
            }

            //设置点击监听
            firstScreenMenu.setOnMenuItemClickListener(item -> {
                //获取选项编号列表
                List<Integer> itemIdList = Arrays.stream(FirstScreen.values())
                        .map(FirstScreen::getItemId)
                        .collect(Collectors.toList());

                //判断是否选中
                if (itemIdList.contains(item.getItemId())) {
                    int index = itemIdList.indexOf(item.getItemId());
                    AppSettingsPreference.setFirstScreen(requireContext(), index);
                    firstScreenOption.setSpinnerText(item.getTitle());
                    return true;
                } else {
                    return false;
                }
            });

            firstScreenMenu.show();
        });

        //自动检测更新
        SettingSwitchView autoUpdateCheck = new SettingSwitchView(
                requireContext(),
                binding.autoUpdateCheckSwitch,
                R.string.auto_update_check,
                "应用启动后自动检查更新",
                R.drawable.outline_deployed_code_update_24,
                RadiusStyle.BOTTOM
        );
        autoUpdateCheck.setChecked(VersionPreference.getAutoUpdateCheck(requireContext()));
        autoUpdateCheck.setFunctionListener((compoundButton, b) ->
                VersionPreference.setAutoUpdateCheck(requireContext(), b)
        );
    }

    /**
     * 初始化通用设置
     */
    private void initCommonSettings() {
        //权限管理
        SettingClickableTextView permissionsOption = new SettingClickableTextView(
                requireContext(),
                binding.permissionsOption,
                R.string.permissions_setting,
                "点击进入权限管理界面",
                R.drawable.outline_admin_panel_settings_24,
                RadiusStyle.TOP
        );
        permissionsOption.setFunctionListener(v -> {
            Intent skip2PermissionManage = new Intent(
                    requireContext(),
                    PermissionManageActivity.class
            );
            startActivity(skip2PermissionManage);
        });

        //数据管理
        SettingClickableTextView dataManage = new SettingClickableTextView(
                requireContext(),
                binding.dataManageOption,
                R.string.data_manage,
                "点击进入数据管理设置界面",
                R.drawable.outline_database_24,
                RadiusStyle.MIDDLE
        );
        dataManage.setFunctionListener(view -> {
            Intent intent = new Intent(requireContext(), DataManageActivity.class);
            startActivity(intent);
        });

        //自动记账
        SettingClickableTextView autoBookkeeping = new SettingClickableTextView(
                requireContext(),
                binding.autoBookkeepingOption,
                R.string.auto_bookkeeping,
                "点击进入自动记账设置界面",
                R.drawable.outline_checkbook_24,
                RadiusStyle.BOTTOM
        );
        autoBookkeeping.setFunctionListener(view -> {
            Intent intent = new Intent(requireContext(), AutoBookkeepingActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 初始化媒体设置
     */
    private void initMediaSettings() {
        // HDR 显示开关
        SettingSwitchView hdrSwitch = new SettingSwitchView(
                requireContext(),
                binding.hdrSwitch,
                R.string.hdr_display_effect,
                "全屏查看图片时支持HDR显示",
                R.drawable.outline_hdr_on_24,
                RadiusStyle.TOP
        );
        hdrSwitch.setChecked(MediaPreference.getHdrDisplay(requireContext()));
        hdrSwitch.setFunctionListener((compoundButton, b) ->
                MediaPreference.setHdrDisplay(requireContext(), b)
        );

        //媒体列表
        SettingClickableTextView mediaList = new SettingClickableTextView(
                requireContext(),
                binding.mediaList,
                R.string.media_list,
                "点击进入媒体文件列表",
                R.drawable.outline_perm_media_24,
                RadiusStyle.BOTTOM
        );
        mediaList.setFunctionListener(view -> {
            Intent intent = new Intent(requireContext(), MediaListActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 初始化隐私设置
     */
    private void initPrivacySettings() {
        //身份验证开关
        SettingSwitchView authenticationSwitch = new SettingSwitchView(
                requireContext(),
                binding.authenticationSwitch,
                R.string.authentication,
                "进入APP时需要进行身份验证",
                R.drawable.outline_security_24,
                RadiusStyle.TOP
        );
        boolean isAuthOpened = SecurityPreference.getAuthSwitchStat(requireContext());
        authenticationSwitch.setChecked(isAuthOpened);
        authenticationSwitch.setFunctionListener(new CompoundButton.OnCheckedChangeListener() {
            private boolean isBlocked = false;  //监听器是否阻塞

            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean b) {
                //被阻塞时不执行操作，防止无限递归
                if (isBlocked) {
                    return;
                }

                String title = b ? "启用身份验证" : "关闭身份验证";

                BiometricHelper.showBiometricPrompt(
                        title,
                        "请验证您的身份",
                        requireActivity(),
                        new BiometricHelper.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                SecurityPreference.setAuthSwitchStat(requireContext(), b);
                                Toast.makeText(requireContext(), b ? "已启用身份验证" : "已关闭身份验证", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(int errCode, CharSequence errStr) {
                                Toast.makeText(requireContext(), errStr, Toast.LENGTH_SHORT).show();

                                isBlocked = true;
                                if (errCode == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                                        || errCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                                ) {
                                    //设备无锁屏/不支持生物验证时关闭身份验证
                                    SecurityPreference.setAuthSwitchStat(requireContext(), false);

                                    //开关设置为关闭
                                    authenticationSwitch.setChecked(false);
                                } else {
                                    //开关设置为原始状态
                                    authenticationSwitch.setChecked(!b);
                                }
                                isBlocked = false;
                            }

                            @Override
                            public void onFailed() {

                            }
                        }
                );
            }
        });

        //身份验证时机
        SettingSpinnerView authenticationOpportunity = new SettingSpinnerView(
                requireContext(),
                binding.authenticationOpportunityOption,
                R.string.authentication_opportunity,
                "进行身份验证的时机",
                R.drawable.outline_safety_check_24,
                RadiusStyle.MIDDLE
        );
        int opportunityCode = SecurityPreference.getAuthOpportunity(requireContext());
        authenticationOpportunity.setSpinnerText(AuthOpportunity.values()[opportunityCode].getTitle());
        authenticationOpportunity.setFunctionListener(view -> {
            PopupMenu opportunityMenu = new PopupMenu(requireContext(), authenticationOpportunity.getFunctionComponent());

            //初始化菜单项
            for (AuthOpportunity opportunity : AuthOpportunity.values()) {
                int groupId = opportunity.getGroupId();
                int itemId = opportunity.getItemId();
                int order = opportunity.getOrder();
                String title = opportunity.getTitle();
                opportunityMenu.getMenu().add(groupId, itemId, order, title);
            }

            //设置选项点击监听
            opportunityMenu.setOnMenuItemClickListener(item -> {
                //获取选项编号列表
                List<Integer> itemIdList = Arrays.stream(AuthOpportunity.values())
                        .map(AuthOpportunity::getItemId)
                        .collect(Collectors.toList());

                //判断是否选中
                if (itemIdList.contains(item.getItemId())) {
                    int index = itemIdList.indexOf(item.getItemId());
                    SecurityPreference.setAuthOpportunity(requireContext(), index);
                    authenticationOpportunity.setSpinnerText(item.getTitle());
                    return true;
                } else {
                    return false;
                }
            });

            opportunityMenu.show();
        });

        //最近任务隐藏
        SettingSwitchView hideRecentTask = new SettingSwitchView(
                requireContext(),
                binding.hideRecentTaskOption,
                R.string.hide_recent_task,
                "在最近任务列表中隐藏",
                R.drawable.outline_visibility_off_24,
                RadiusStyle.BOTTOM
        );
        boolean isHidden = SecurityPreference.getHideRecentTask(requireContext());
        hideRecentTask.setChecked(isHidden);
        hideRecentTask.setFunctionListener((compoundButton, checked) -> {
            SecurityPreference.setHideRecentTask(checked, requireContext());
            SlyCoffer.hideTaskFromRecents(requireContext(), checked);
        });
    }

    /**
     * 初始化关于设置项
     */
    private void initAboutSettings() {
        //关于软件
        SettingClickableTextView aboutOption = new SettingClickableTextView(
                requireContext(),
                binding.aboutOption,
                R.string.about_software,
                null,
                R.drawable.outline_info_24,
                RadiusStyle.TOP
        );
        aboutOption.setFunctionListener(v -> {
            Intent skip2About = new Intent(requireContext(), AboutActivity.class);
            startActivity(skip2About);
        });

        //更新日志
        SettingClickableTextView updateLogOption = new SettingClickableTextView(
                requireContext(),
                binding.updateLogOption,
                R.string.changelog,
                null,
                R.drawable.outline_lab_profile_24,
                RadiusStyle.MIDDLE
        );
        updateLogOption.setFunctionListener(
                v -> AboutHelper.showUpdateLogDialog(requireContext())
        );

        //更新检测
        SettingClickableTextView updateCheckOption = new SettingClickableTextView(
                requireContext(),
                binding.updateCheckOption,
                R.string.update_check,
                null,
                R.drawable.outline_update_24,
                RadiusStyle.BOTTOM
        );
        updateCheckOption.setFunctionListener(
                v -> {
                    Toast.makeText(requireContext(), "正在检查更新……", Toast.LENGTH_SHORT).show();
                    UpdateHelper.checkUpdate(requireContext(), disposable, true);
                }
        );
    }

    /**
     * 显示主题模式选择对话框
     */
    private void showThemeModeSelectDialog() {
        String[] themeModeStr = Arrays.stream(ThemeMode.values())
                .map(ThemeMode::getTitle)
                .toArray(String[]::new);
        int themeMode = AppSettingsPreference.getThemeMode(requireContext());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("主题模式")
                .setSingleChoiceItems(themeModeStr, themeMode, (dialog, which) -> {
                    AppSettingsPreference.setThemeMode(requireContext(), which);
                    ThemeHelper.switchNightModeWithAnimation(requireActivity(), which);
                    dialog.dismiss();
                })
                .setNegativeButton("关闭", null)
                .show();
    }
}