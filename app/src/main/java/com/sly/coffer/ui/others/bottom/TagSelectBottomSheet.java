package com.sly.coffer.ui.others.bottom;

import android.app.Dialog;
import android.content.Intent;
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
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.composite.ui.TagGroupUiModel;
import com.sly.coffer.data.save.db.services.TagService;
import com.sly.coffer.databinding.BottomSheetTagSelectBinding;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.ui.others.adapters.GroupTagMultiSelectAdapter;
import com.sly.coffer.ui.others.adapters.GroupTagSingleSelectAdapter;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.others.viewmodel.TagSingleSelectViewModel;
import com.sly.coffer.ui.pages.tag.TagInputActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TagSelectBottomSheet extends BaseBottomSheetDialogFragment {
    private BottomSheetTagSelectBinding binding;        //绑定的 XML 布局
    private final CompositeDisposable disposable = new CompositeDisposable();
    private int scopePow = 0;                           //标签作用域标识符
    private boolean isMultiMode = true;                 //是否为多选模式
    private long[] exceptedTagIds = null;               //被排除的标签的 ID

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            scopePow = bundle.getInt(KeyStrings.TAG_SCOPE.v(), 0);
            isMultiMode = bundle.getBoolean(KeyStrings.TAG_MULTI_CHOICE.v(), true);
            exceptedTagIds = bundle.getLongArray(KeyStrings.TAG_ID.v());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetTagSelectBinding.inflate(inflater, container, false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.mainRecycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();

        //绑定消失监听器
        if (isMultiMode) {
            setOnDismissListener(() -> {
                TagMultiSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(TagMultiSelectViewModel.class);
                viewModel.setNeedExecute(true);
            });
        }

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
            Intent skip2TagInput = new Intent(requireContext(), TagInputActivity.class);
            startActivity(skip2TagInput);
        });
    }

    /**
     * 初始化主列表
     */
    private void initMainRecycler() {
        //设置适配器
        ListAdapter<TagGroupUiModel, RecyclerView.ViewHolder> adapter;
        if (isMultiMode) {
            TagMultiSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(TagMultiSelectViewModel.class);
            adapter = new GroupTagMultiSelectAdapter(
                    viewModel.getCheckedTagIdSet(),
                    (tag, isChecked, anchor) -> {
                        if (isChecked) {
                            viewModel.getCheckedTagIdSet().add(tag.getTagId());
                        } else {
                            viewModel.getCheckedTagIdSet().remove(tag.getTagId());
                        }
                    }
            );
        } else {
            TagSingleSelectViewModel viewModel = new ViewModelProvider(requireActivity()).get(TagSingleSelectViewModel.class);
            adapter = new GroupTagSingleSelectAdapter(
                    (entity, anchor) -> {
                        viewModel.setClickedEntity(entity);
                        dismiss();
                    }
            );
        }
        binding.mainRecycler.setAdapter(adapter);

        //订阅数据
        BookkeepingDb db = BookkeepingDb.getInstance(requireContext());
        disposable.add(TagService.getGroupedTagFlowable(db, scopePow, exceptedTagIds)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        modelList -> {
                            VisibilityHelper.toggleVisibilityWithFade(binding.loadingIndicator, false);
                            if (modelList.isEmpty()) {
                                VisibilityHelper.toggleVisibilityWithFade(binding.emptyText, true);
                            } else {
                                binding.emptyText.setVisibility(View.GONE);
                            }

                            adapter.submitList(modelList);
                        },
                        e -> ExceptionHelper.showExceptionDialog(requireContext(), e)
                )
        );
    }
}
