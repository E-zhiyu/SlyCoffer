package com.sly.coffer.ui.pages.notification.group;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.databinding.BottomSheetNotificationRuleSelectBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.bottom.BaseBottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRuleSelectBottomSheet extends BaseBottomSheetDialogFragment {
    private BottomSheetNotificationRuleSelectBinding binding;
    private final List<Long> checkedIdList = new ArrayList<>();
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetNotificationRuleSelectBinding.inflate(inflater, container, false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();

        //绑定消失监听器
        setOnDismissListener(() -> {
            NotificationRuleSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(NotificationRuleSelectViewModel.class);
            viewModel.updateRuleLiveData(checkedIdList);
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            // 1. 捞出系统的底座容器
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                int desiredHeight = (int) (screenHeight * 0.75);

                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = desiredHeight;
                bottomSheet.setLayoutParams(layoutParams);

                // 3. 配置展开状态：一探头就直接进入完全展开状态，不给它留半折腾的空间
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true); // 往下滑直接关闭，不允许停留在半高状态

                // 4. 设置默认的起跳高度，防止高度坍塌
                behavior.setPeekHeight(desiredHeight);
            }
        }
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
        //初始化已选择的 ID
        NotificationRuleSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(NotificationRuleSelectViewModel.class);
        List<Long> initList = viewModel.getRuleIdLiveData().getValue();
        if (initList != null && !initList.isEmpty()) {
            checkedIdList.addAll(initList);
        }

        //设置适配器
        RuleSelectListAdapter adapter = new RuleSelectListAdapter(
                checkedIdList,
                (entity, isChecked, anchor) -> {
                    long ruleId = entity.getRuleId();
                    if (isChecked && !checkedIdList.contains(ruleId)) {
                        checkedIdList.add(ruleId);
                    } else {
                        checkedIdList.remove(ruleId);
                    }
                }
        );
        binding.recycler.setAdapter(adapter);

        //订阅数据
        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.notificationRuleDao().getNotificationRuleFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ruleList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, false);
                            if (ruleList.isEmpty()) {
                                VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, true);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(ruleList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }
}
