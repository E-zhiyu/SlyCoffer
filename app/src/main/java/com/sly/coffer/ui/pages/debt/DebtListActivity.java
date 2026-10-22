package com.sly.coffer.ui.pages.debt;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sly.coffer.R;
import com.sly.coffer.databinding.ActivityDebtListBinding;

public class DebtListActivity extends AppCompatActivity {
    private ActivityDebtListBinding binding;    //绑定的 XML 布局

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDebtListBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            binding.recycler.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }
}