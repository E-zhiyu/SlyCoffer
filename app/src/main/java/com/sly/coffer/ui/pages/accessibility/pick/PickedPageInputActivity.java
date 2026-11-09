package com.sly.coffer.ui.pages.accessibility.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.PickedPageEntity;
import com.sly.coffer.data.save.db.services.AccessibilityRuleService;
import com.sly.coffer.databinding.ActivityPickedPageInputBinding;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.ui.pages.common.app_list.AppSelectActivity;

import java.time.LocalDateTime;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PickedPageInputActivity extends AppCompatActivity {
    private ActivityPickedPageInputBinding binding;                     //绑定的 XML 布局
    private ActivityResultLauncher<Intent> packageNameSelectLauncher;   //包名选择启动器
    private final CompositeDisposable disposable = new CompositeDisposable();

    {
        //初始化意图启动器
        initLaunchers();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPickedPageInputBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            //滚动视图的内部布局
            binding.scrollLayout.setPadding(
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10),
                    AppearanceHelper.dpToPx(this, 10) + Math.max(ime.bottom, systemBars.bottom)
            );

            return insets;
        });

        initViews();
        observeLiveData();

        //加载初始数据
        Bundle initBundle = getIntent().getExtras();
        if (initBundle != null) {
            PickedPageInputViewModel viewModel = new ViewModelProvider(this).get(PickedPageInputViewModel.class);
            if (viewModel.getPickedPage() == null) {
                long id = initBundle.getLong(KeyStrings.PICKED_PAGE_ID.v());
                BookkeepingDb db = BookkeepingDb.getInstance(this);
                disposable.add(db.accessibilityRuleDao().getPickedPageSingleById(id)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                optional -> {
                                    if (optional.isEmpty()) return;

                                    PickedPageEntity pickedPage = optional.get();
                                    viewModel.setPickedPage(pickedPage);
                                },
                                e -> ExceptionHelper.showExceptionDialog(this, e)
                        )
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    /**
     * 初始化启动器
     */
    private void initLaunchers() {
        packageNameSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();

                    if (resultCode == Activity.RESULT_OK) {
                        if (data != null) {
                            String packageName = data.getStringExtra(KeyStrings.PACKAGE_NAME.v());
                            binding.packageNameInput.setText(packageName);
                            binding.packageNameLayout.setError(null);
                        } else {
                            NullPointerException e = new NullPointerException("无法获取包名");
                            ExceptionHelper.showExceptionDialog(this, e);
                        }
                    }
                }
        );
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        //包名
        binding.packageNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.packageNameLayout.setError(null);
                Intent skip2PackageNameSelect = new Intent(this, AppSelectActivity.class);
                packageNameSelectLauncher.launch(skip2PackageNameSelect);
            } else {
                String packageName = String.valueOf(binding.packageNameInput.getText());
                if (packageName.isEmpty()) {
                    binding.packageNameLayout.setErrorEnabled(true);
                    binding.packageNameLayout.setError("包名不能为空");
                }
            }
        });
        binding.packageNameInput.setOnClickListener(view -> {
            binding.packageNameLayout.setError(null);
            Intent skip2PackageNameSelect = new Intent(this, AppSelectActivity.class);
            packageNameSelectLauncher.launch(skip2PackageNameSelect);
        });

        //活动名
        binding.activityNameInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.activityNameLayout.setError(null);
            } else {
                String input = String.valueOf(binding.activityNameInput.getEditableText()).trim();
                if (input.isEmpty()) {
                    binding.activityNameLayout.setError("界面名称不能为空");
                }
            }
        });

        //确认按钮
        binding.confirmButton.setOnClickListener(view -> {
            String err = verifyInput();
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                return;
            }

            saveData();
        });
    }

    /**
     * 观察 ViewModel 中的 LiveData
     */
    private void observeLiveData() {
        //输入框数据
        PickedPageInputViewModel inputViewModel = new ViewModelProvider(this).get(PickedPageInputViewModel.class);
        inputViewModel.getPickedPageLiveData().observe(this, pickedPage -> {
            binding.remarkInput.setText(pickedPage.getRemark());                //备注
            binding.packageNameInput.setText(pickedPage.getPackageName());      //应用包名
            binding.activityNameInput.setText(pickedPage.getActivityName());    //界面名称
        });
    }

    /**
     * 校验输入内容的有效性
     *
     * @return 错误提示，无错误返回 null
     */
    private String verifyInput() {
        String err = null;
        String packageName = String.valueOf(binding.packageNameInput.getEditableText()).trim();
        String activityName = String.valueOf(binding.activityNameInput.getEditableText()).trim();

        if (packageName.isEmpty()) {
            err = "包名不能为空";
            binding.packageNameLayout.setError(err);
        } else if (activityName.isEmpty()) {
            err = "界面名称不能为空";
            binding.activityNameLayout.setError(err);
        }

        return err;
    }

    /**
     * 保存数据
     */
    private void saveData() {
        //获取输入的内容
        String remark = String.valueOf(binding.remarkInput.getEditableText()).trim();
        String packageName = String.valueOf(binding.packageNameInput.getEditableText()).trim();
        String activityName = String.valueOf(binding.activityNameInput.getEditableText()).trim();

        //实例化数据实体
        PickedPageEntity pickedPage = new PickedPageEntity(
                remark,
                packageName,
                activityName,
                LocalDateTime.now()
        );

        //保存数据
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        PickedPageInputViewModel inputViewModel = new ViewModelProvider(this).get(PickedPageInputViewModel.class);
        if (inputViewModel.getPickedPage() != null) {
            pickedPage.setId(inputViewModel.getPickedPage().getId());
            disposable.add(db.accessibilityRuleDao().updatePickedPageCompletable(pickedPage)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "界面信息修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        } else {
            disposable.add(AccessibilityRuleService.addPickedPage(pickedPage, db)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            id -> {
                                if (id >= 0) {
                                    Toast.makeText(this, "界面信息添加成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "界面信息添加失败", Toast.LENGTH_SHORT).show();
                                }
                            },
                            e -> ExceptionHelper.showExceptionDialog(this, e)
                    )
            );
        }
    }
}