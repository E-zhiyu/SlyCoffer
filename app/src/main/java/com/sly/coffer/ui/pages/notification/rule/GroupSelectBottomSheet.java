package com.sly.coffer.ui.pages.notification.rule;

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
import com.sly.coffer.databinding.BottomSheetNotificationRuleGroupSelectBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.bottom.BaseBottomSheetDialogFragment;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GroupSelectBottomSheet extends BaseBottomSheetDialogFragment {
    private BottomSheetNotificationRuleGroupSelectBinding binding;
    private final Set<Long> checkedIdSet = new HashSet<>();
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetNotificationRuleGroupSelectBinding.inflate(inflater, container, false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();

        //绑定消失监听器
        setOnDismissListener(() -> {
            GroupSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(GroupSelectViewModel.class);
            viewModel.updateCheckedGroupId(checkedIdSet);
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
        //主 Recycler
        initMainRecycler();

        //角色添加按钮
        binding.addBtn.setOnClickListener(view -> {
            //TODO:添加逻辑
        });
    }

    /**
     * 初始化主列表
     */
    private void initMainRecycler() {
        //设置适配器
        GroupSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(GroupSelectViewModel.class);
        NotiRuleGroupSelectListAdapter adapter = new NotiRuleGroupSelectListAdapter(
                viewModel.getGroupIdSetLiveData().getValue(),
                (entity, isChecked, anchor) -> {
                    if (isChecked) {
                        checkedIdSet.add(entity.getGroupId());
                    } else {
                        checkedIdSet.remove(entity.getGroupId());
                    }
                }
        );
        binding.recycler.setAdapter(adapter);

        Set<Long> initSet = viewModel.getGroupIdSetLiveData().getValue();
        if (initSet != null) {
            checkedIdSet.addAll(initSet);
        }

        //订阅数据
        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(db.notificationRuleDao().getRuleGroupFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        groupList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, false);
                            if (groupList.isEmpty()) {
                                VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, true);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(groupList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }
}
