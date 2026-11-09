package com.sly.coffer.ui.pages.tag;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.sly.coffer.R;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.TagUnionModel;
import com.sly.coffer.data.save.db.services.TagService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityTagInputBinding;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.ImmHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TagInputActivity extends AppCompatActivity {
    private ActivityTagInputBinding binding;    //绑定的XML视图的引用
    private final CompositeDisposable disposable = new CompositeDisposable();  //订阅列表（便于取消订阅）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTagInputBinding.inflate(getLayoutInflater());
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

        Bundle initBundle = getIntent().getExtras();
        if (initBundle != null) {
            TagInputViewModel inputViewModel = new ViewModelProvider(this).get(TagInputViewModel.class);
            if (inputViewModel.getTagModel() == null) {
                long tagId = initBundle.getLong(KeyStrings.TAG_ID.v());
                BookkeepingDb db = BookkeepingDb.getInstance(this);
                disposable.add(db.tagDao().getTagWithGroupSingleById(tagId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                optional -> {
                                    if (optional.isEmpty()) return;

                                    TagUnionModel model = optional.get();
                                    inputViewModel.setTagModel(model);
                                },
                                e -> ExceptionHelper.showExceptionDialog(this, e)
                        )
                );
            } else {
                initScopeChipGroup();
            }
        } else {
            initScopeChipGroup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (getIntent().getExtras() != null) {
            binding.toolbar.setTitle(R.string.modify_tag);
        } else {
            binding.toolbar.setTitle(R.string.add_tag);
        }

        //标签名称输入框
        binding.nameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String tagName = String.valueOf(binding.nameInput.getText()).trim();

                if (tagName.isEmpty()) {
                    binding.nameLayout.setErrorEnabled(true);
                    binding.nameLayout.setError("标签名不能为空");
                }
            } else {
                binding.nameLayout.setError(null);
            }
        });
        ImmHelper.showImm(binding.nameInput);

        //标签分组自动填充适配器
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.tagDao().getAllTagGroupNameSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        nameList -> {
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                                    this,
                                    R.layout.exposed_dropdown_popup_item,
                                    nameList
                            );
                            binding.groupInput.setAdapter(arrayAdapter);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //标签作用域提示文本
        binding.tagScopeExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "控制该标签的可见范围";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //完成按钮
        binding.confirmButton.setOnClickListener(v -> {
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
        TagInputViewModel inputViewModel = new ViewModelProvider(this).get(TagInputViewModel.class);
        inputViewModel.getTagModelLiveData().observe(this, model -> {
            TagEntity tag = model.getTag();

            //标签名称
            String tagName = tag.getName();
            binding.nameInput.setText(tagName);

            //分组名称
            if (model.getGroup() != null) {
                binding.groupInput.setText(model.getGroup().getName());
            }

            //初始化标签作用域选择
            initScopeChipGroup();
        });
    }

    /**
     * 初始化标签作用域 ChipGroup
     */
    private void initScopeChipGroup() {
        binding.scopeChipGroup.removeAllViews();
        TagInputViewModel inputViewModel = new ViewModelProvider(this).get(TagInputViewModel.class);
        TagUnionModel tagModel = inputViewModel.getTagModel();
        for (AccountType type : AccountType.values()) {
            Chip scopeChip = new Chip(this);
            scopeChip.setCheckable(true);
            scopeChip.setCheckedIconVisible(true);
            scopeChip.setText(type.getTitle());
            scopeChip.setId(type.ordinal());    //将枚举序数作为编号

            //设置初始选择状态
            int iBinary = (int) Math.pow(2, type.ordinal());
            if (tagModel != null) {
                scopeChip.setChecked((tagModel.getTag().getScope() & iBinary) == 0);
            }

            //添加视图到Chip组中
            binding.scopeChipGroup.addView(scopeChip);
        }
    }

    /**
     * 校验输入内容的合法性
     *
     * @return 错误提示，若无错误返回 null
     */
    private String verifyInput() {
        String err = null;
        String tagName = String.valueOf(binding.nameInput.getText()).trim();

        if (tagName.isEmpty()) {
            err = "标签名称不能为空";
            binding.nameLayout.setError(err);
        }

        return err;
    }

    /**
     * 从 ChipGroup 中获取用户选择的标签作用域
     *
     * @return 标签作用域
     */
    private int getTagScopeFromChipGroup() {
        int scope = Integer.MAX_VALUE;
        for (int id : binding.scopeChipGroup.getCheckedChipIds()) {
            scope &= ~(int) Math.pow(2, id);
        }
        return scope;
    }

    /**
     * 将输入的数据保存到数据库
     */
    private void saveData() {
        //获取输入内容
        String tagName = String.valueOf(binding.nameInput.getText()).trim();
        String groupName = String.valueOf(binding.groupInput.getText()).trim();

        //保存数据
        TagInputViewModel inputViewModel = new ViewModelProvider(this).get(TagInputViewModel.class);
        TagEntity tag = new TagEntity(tagName, getTagScopeFromChipGroup());
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        final String NAME_EXISTS_EXCEPTION = "NAME_EXISTS";
        if (inputViewModel.getTagModel() == null) {
            disposable.add(db.tagDao().isTagNameInDb(tagName, tag.getTagId())
                    .flatMapCompletable(exists -> {
                        if (exists) {
                            return Completable.error(new IllegalArgumentException(NAME_EXISTS_EXCEPTION));
                        } else {
                            return TagService.addTag(tag, groupName, db);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "标签添加成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> {
                                String message = e.getMessage();
                                if (message != null && message.equals(NAME_EXISTS_EXCEPTION)) {
                                    String nameError = "已有同名标签";
                                    Toast.makeText(this, nameError, Toast.LENGTH_SHORT).show();
                                    binding.nameLayout.setError(nameError);
                                } else {
                                    ExceptionHelper.showExceptionDialog(this, e);
                                }
                            }
                    )
            );
        } else {
            tag.setTagId(inputViewModel.getTagModel().getTag().getTagId());
            disposable.add(db.tagDao().isTagNameInDb(tagName, tag.getTagId())
                    .subscribeOn(Schedulers.io())
                    .flatMapCompletable(exists -> {
                        if (exists) {
                            return Completable.error(new IllegalArgumentException(NAME_EXISTS_EXCEPTION));
                        } else {
                            return TagService.modifyTag(tag, groupName, db);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "标签修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> {
                                String message = e.getMessage();
                                if (message != null && message.equals(NAME_EXISTS_EXCEPTION)) {
                                    String nameError = "已有同名标签";
                                    Toast.makeText(this, nameError, Toast.LENGTH_SHORT).show();
                                    binding.nameLayout.setError(nameError);
                                } else {
                                    ExceptionHelper.showExceptionDialog(this, e);
                                }
                            }
                    )
            );
        }
    }
}