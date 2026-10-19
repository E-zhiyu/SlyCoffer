package com.sly.coffer.ui.others.bottom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.auxiliary.enums.bottom_options.MediaAddOption;
import com.sly.coffer.databinding.BottomSheetMediaAddOptionBinding;
import com.sly.coffer.ui.others.viewmodel.MediaAddOptionViewModel;

public class MediaAddBottomSheet extends BaseBottomSheetDialogFragment {
    private BottomSheetMediaAddOptionBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMediaAddOptionBinding.inflate(inflater, container, false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        initViews();

        return binding.getRoot();
    }

    private void initViews() {
        MediaAddOptionViewModel viewModel = new ViewModelProvider(requireActivity()).get(MediaAddOptionViewModel.class);

        binding.addViaCamera.setOnClickListener(v -> {
            viewModel.setClickEvent(MediaAddOption.TAKE_PICTURE.ordinal());
            dismiss();
        });

        binding.addViaAlbum.setOnClickListener(v -> {
            viewModel.setClickEvent(MediaAddOption.OPEN_ALBUM.ordinal());
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
