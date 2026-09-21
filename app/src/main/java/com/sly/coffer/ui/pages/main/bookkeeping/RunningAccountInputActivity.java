package com.sly.coffer.ui.pages.main.bookkeeping;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sly.coffer.R;
import com.sly.coffer.SlyCoffer;
import com.sly.coffer.auxiliary.classes.CustomDateTimeFormatter;
import com.sly.coffer.auxiliary.enums.types.AutoBookkeepingType;
import com.sly.coffer.auxiliary.enums.unique.TransitionName;
import com.sly.coffer.auxiliary.enums.bottom_options.MediaAddOption;
import com.sly.coffer.data.save.db.BookkeepingDb;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.composite.union.AccountUnionModel;
import com.sly.coffer.data.save.db.services.AccountService;
import com.sly.coffer.data.save.preference.TipPreference;
import com.sly.coffer.databinding.ActivityRunningAccountInputBinding;
import com.sly.coffer.auxiliary.enums.DirectoryPaths;
import com.sly.coffer.auxiliary.enums.unique.KeyStrings;
import com.sly.coffer.auxiliary.enums.types.AccountType;
import com.sly.coffer.auxiliary.enums.unique.LogTags;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;
import com.sly.coffer.helpers.BackPressedCallbackHelper;
import com.sly.coffer.helpers.time.DateTimePickerHelper;
import com.sly.coffer.helpers.ExceptionHelper;
import com.sly.coffer.helpers.PermissionHelper;
import com.sly.coffer.helpers.appearence.AppearanceHelper;
import com.sly.coffer.helpers.appearence.VisibilityHelper;
import com.sly.coffer.helpers.file.FileHelper;
import com.sly.coffer.ui.others.adapters.NoFilteringArrayAdapter;
import com.sly.coffer.ui.others.bottom.MediaAddBottomSheet;
import com.sly.coffer.ui.others.bottom.TagSelectBottomSheet;
import com.sly.coffer.ui.others.dialogs.ProgressDialogBuilder;
import com.sly.coffer.ui.others.selections.media.MediaIdKeyProvider;
import com.sly.coffer.ui.others.selections.media.MediaLookup;
import com.sly.coffer.ui.others.viewmodel.MediaAddOptionViewModel;
import com.sly.coffer.ui.others.viewmodel.TagMultiSelectViewModel;
import com.sly.coffer.ui.pages.media.FullScreenMediaActivity;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RunningAccountInputActivity extends AppCompatActivity {
    private AccountType type = AccountType.EXPENSE;         //流水种类
    private ActivityRunningAccountInputBinding binding;     //绑定的XML视图引用
    @Nullable
    private Bundle initBundle = null;                       //传递初始数据的数据包
    private final CompositeDisposable disposable = new CompositeDisposable();
    private SelectionTracker<Long> selectionTracker;        //图片列表选择追踪器
    private BackPressedCallbackHelper backHelper;           //返回逻辑管理器
    private BackPressedCallbackHelper.BackHandler selectionBackHandler; //媒体多选返回处理器
    private AccountMediaAdapter mediaAdapter;               //媒体适配器
    private AccountTagAdapter tagAdapter;                   //标签适配器
    private ActivityResultLauncher<PickVisualMediaRequest> albumLauncher;   //相册图片选择启动器
    private ActivityResultLauncher<Uri> takePictureLauncher;    //调用系统相机的启动器
    private ActivityResultLauncher<String> permissionLauncher;  //权限申请启动器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRunningAccountInputBinding.inflate(getLayoutInflater());
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

        initBundle = getIntent().getExtras();
        initViews();
        observeLiveData();
        initLaunchers();
        initOnBackPressedHandlers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disposable.dispose();
        binding = null;

        //移除临时媒体目录中的文件
        FileHelper.clearMediaTempDir(this);
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        //标签 Recycler
        tagAdapter = new AccountTagAdapter(
                (entity, anchor, adapter) -> {
                    //切换视图可见性
                    List<TagEntity> removedList = new ArrayList<>(adapter.getCurrentList());
                    removedList.remove(entity);
                    if (!removedList.isEmpty()) {
                        tagAdapter.submitList(
                                removedList,
                                () -> VisibilityHelper.toggleViewExpansion(
                                        binding.scrollLayout,
                                        true,
                                        null,
                                        binding.tagRecycler
                                )
                        );
                    } else {
                        VisibilityHelper.toggleViewExpansion(
                                binding.scrollLayout,
                                false,
                                () -> tagAdapter.submitList(removedList),
                                binding.tagRecycler
                        );
                    }

                    //移除ViewModel集合中的数据
                    TagMultiSelectViewModel viewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
                    viewModel.getCheckedTagIdSet().remove(entity.getTagId());
                }
        );
        binding.tagRecycler.setAdapter(tagAdapter);

        //媒体 Recycler
        int spanCount = AppearanceHelper.getScreenHeight(this) >
                AppearanceHelper.getScreenWidth(this) ?
                3 : 7;
        int size = (binding.scrollLayout.getWidth() -
                binding.scrollLayout.getPaddingStart() -
                binding.scrollLayout.getPaddingEnd()) / spanCount;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        binding.mediaRecycler.setLayoutManager(layoutManager);
        mediaAdapter = new AccountMediaAdapter(
                size,
                (pos, mediaView, mediaList) -> {
                    String[] uriStrArray = mediaList.stream()
                            .map(MediaEntity::getFileUri)
                            .map(Uri::toString)
                            .toArray(String[]::new);

                    //实例化 Intent 并放入数据
                    Intent skip2FullScreen = new Intent(this, FullScreenMediaActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(KeyStrings.FILE_URIS.v(), uriStrArray);
                    bundle.putInt(KeyStrings.VIEW_HOLDER_POSITION.v(), pos);
                    skip2FullScreen.putExtras(bundle);

                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            mediaView,
                            TransitionName.FULLSCREEN_MEDIA.getS()
                    );

                    startActivity(skip2FullScreen, options.toBundle());
                }
        );
        binding.mediaRecycler.setAdapter(mediaAdapter);
        selectionTracker = new SelectionTracker.Builder<>(
                TagStrings.MEDIA_SELECTION.t(),
                binding.mediaRecycler,
                new MediaIdKeyProvider(mediaAdapter),
                new MediaLookup(binding.mediaRecycler),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
                SelectionPredicates.createSelectAnything()      //允许多选
        ).build();
        mediaAdapter.setSelectionTracker(selectionTracker);
        selectionTracker.addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onSelectionChanged() {  //追踪选择状态
                super.onSelectionChanged();
                if (selectionTracker.hasSelection()) {
                    new Handler(Looper.getMainLooper()).post(   //必须主线程更新 UI
                            () -> setSelectMode(true)
                    );

                    int size = selectionTracker.getSelection().size();
                    Log.d(LogTags.ACCOUNT_INPUT.n(), "已选择：" + size);
                } else {
                    new Handler(Looper.getMainLooper()).post(   //必须主线程更新 UI
                            () -> setSelectMode(false)
                    );
                    Log.d(LogTags.ACCOUNT_INPUT.n(), "选择已清除");
                }
            }
        });

        //工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (initBundle != null) {
            binding.toolbar.setTitle(R.string.modify_running_account);

            //读取初始数据
            long accountId = initBundle.getLong(KeyStrings.RUNNING_ID.v());
            BookkeepingDb db = BookkeepingDb.getInstance(this);
            disposable.add(db.accountDao().getAccountWithDetailSingleById(accountId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            modelOptional -> {
                                if (modelOptional.isEmpty()) return;

                                //获取数据
                                AccountUnionModel model = modelOptional.get();
                                AccountEntity account = model.getAccount();
                                AccountTransferEntity transfer = model.getTransfer();
                                List<TagEntity> tagList = model.getTagList();
                                List<MediaEntity> mediaList = model.getMediaList();

                                //显示文本框数据
                                binding.amountInput.setText(String.valueOf(account.getAmount()));   //金额
                                binding.remarkInput.setText(account.getRemark());                   //备注
                                type = AccountType.values()[account.getType()];
                                binding.typeInput.setText(type.getTitle());                         //种类
                                binding.datetimeInput.setText(account.getDateTime().format(
                                        CustomDateTimeFormatter.DATE_TIME
                                ));                                                                 //日期和时间
                                if (type == AccountType.TRANSFER) {
                                    binding.exportAccountLayout.setVisibility(View.VISIBLE);
                                    binding.importAccountLayout.setVisibility(View.VISIBLE);

                                    binding.exportAccountInput.setText(transfer.getExportAccount());    //转出账户
                                    binding.importAccountInput.setText(transfer.getImportAccount());    //转入账户
                                }

                                //显示标签
                                if (!tagList.isEmpty()) {
                                    tagAdapter.submitList(tagList);
                                    binding.tagRecycler.setVisibility(View.VISIBLE);
                                } else {
                                    binding.tagRecycler.setVisibility(View.GONE);
                                }
                                List<Long> tagIdList = tagList.stream()
                                        .map(TagEntity::getTagId)
                                        .collect(Collectors.toList());
                                TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
                                tagMultiSelectViewModel.getCheckedTagIdSet().clear();
                                tagMultiSelectViewModel.getCheckedTagIdSet().addAll(tagIdList);

                                //显示媒体
                                if (!mediaList.isEmpty()) {
                                    mediaAdapter.submitList(mediaList);
                                }
                            }
                    )
            );
        }

        //金额
        binding.amountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.amountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.amountInput.getText());
                if (input.trim().isEmpty()) {
                    binding.amountLayout.setError("金额不能为空");
                } else if (Double.parseDouble(input) == 0.0) {
                    binding.amountLayout.setError("金额不能为0");
                }
            }
        });

        //种类
        NoFilteringArrayAdapter<String> typeAdapter = new NoFilteringArrayAdapter<>(
                this,
                Arrays.stream(AccountType.values())
                        .map(AccountType::getTitle)
                        .toArray(String[]::new)
        );
        binding.typeInput.setText(type.getTitle());
        binding.typeInput.setAdapter(typeAdapter);
        binding.typeInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    boolean transferVisible = position == AccountType.TRANSFER.ordinal();
                    VisibilityHelper.toggleViewExpansion(
                            binding.scrollLayout,
                            transferVisible,
                            null,
                            binding.exportAccountLayout,
                            binding.importAccountLayout
                    );

                    type = AccountType.values()[position];
                }
        );

        //日期和时间
        binding.datetimeInput.setText(LocalDateTime.now().format(CustomDateTimeFormatter.DATE_TIME));
        binding.datetimeInput.setOnClickListener(v -> selectDateTime());
        binding.datetimeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectDateTime();
                binding.datetimeLayout.setError(null);
            }
        });

        //转出和转入账户的自动填充适配器
        BookkeepingDb db = BookkeepingDb.getInstance(this);
        disposable.add(db.accountDao().getTransferAccountsSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        transferAccountList -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    this,
                                    R.layout.exposed_dropdown_popup_item,
                                    transferAccountList
                            );

                            binding.exportAccountInput.setAdapter(adapter);
                            binding.importAccountInput.setAdapter(adapter);
                        },
                        e -> ExceptionHelper.showExceptionDialog(this, e)
                )
        );

        //转出账户
        binding.exportAccountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.exportAccountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.exportAccountInput.getText()).trim();
                if (type == AccountType.TRANSFER && input.isEmpty()) {
                    binding.exportAccountLayout.setError("转出账户不能为空");
                }
            }
        });

        //转入账户
        binding.importAccountInput.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.importAccountLayout.setError(null);
            } else {
                String input = String.valueOf(binding.importAccountInput.getText()).trim();
                if (type == AccountType.TRANSFER && input.isEmpty()) {
                    binding.importAccountLayout.setError("转出账户不能为空");
                }
            }
        });

        //标签选择按钮
        binding.tagSelectBtn.setOnClickListener(view -> {
            TagSelectBottomSheet bottomSheet = new TagSelectBottomSheet();
            Bundle bundle = new Bundle();
            bundle.putInt(KeyStrings.TAG_SCOPE.v(), (int) Math.pow(2, type.ordinal())); //传递标签作用域标识符
            bottomSheet.setArguments(bundle);
            bottomSheet.show(getSupportFragmentManager(), TagStrings.TAG_SELECT_BOTTOM.t());
        });

        //标签说明按钮
        binding.tagExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "使用标签标记，便于分类，同时还能支持预算自动管理等自动化功能";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //媒体添加按钮
        binding.mediaAddBtn.setOnClickListener(view -> {
            MediaAddBottomSheet bottomSheet = new MediaAddBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), TagStrings.MEDIA_ADD_BOTTOM.t());
        });

        //媒体删除按钮
        binding.mediaDeleteBtn.setOnClickListener(view -> {
            //获取需要被删除的媒体
            List<MediaEntity> mediaListToBeDeleted = new ArrayList<>();
            for (long id : selectionTracker.getSelection()) {
                MediaEntity media = mediaAdapter.getItemById(id);
                if (media != null) {
                    mediaListToBeDeleted.add(media);
                }
            }

            //退出多选
            selectionTracker.clearSelection();

            //更新适配器列表
            List<MediaEntity> mediaList = new ArrayList<>(mediaAdapter.getCurrentList());
            mediaList.removeAll(mediaListToBeDeleted);
            mediaAdapter.submitList(mediaList);
        });

        //媒体说明按钮
        binding.mediaExplainBtn.setOnClickListener(view -> {
            final String EXPLANATION = "利用媒体文件保存更多流水信息，便于日后快速了解详情";
            TipPreference.showTipWithoutKey(view, Gravity.START, EXPLANATION);
        });

        //完成按钮
        binding.confirmButton.setOnClickListener(v -> {
            String err = verifyInput();
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                return;
            }

            //显示进度条对话框
            ProgressDialogBuilder dialogBuilder = new ProgressDialogBuilder(
                    this,
                    "保存媒体文件",
                    "正在移动媒体文件……"
            );
            AlertDialog dialog = dialogBuilder
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                        disposable.clear();
                        Toast.makeText(this, "已取消媒体文件保存", Toast.LENGTH_SHORT).show();
                    })
                    .create();

            //如果有新媒体，则显示对话框
            List<MediaEntity> currentMediaList = mediaAdapter.getCurrentList();
            Set<MediaEntity> newMediaSet = currentMediaList.stream()    //获取新添加的媒体
                    .filter(mediaEntity -> mediaEntity.getMediaId() == 0)
                    .collect(Collectors.toSet());
            if (!newMediaSet.isEmpty()) {
                dialog.show();
            }

            //创建移动任务，并逐个返回移动成功的 File 型 Uri
            Observable<MediaEntity> moveTask = Observable.create(emitter -> {
                File targetDir = DirectoryPaths.MEDIA.getDir(this);
                long accountId = initBundle == null ? 0 : initBundle.getLong(KeyStrings.RUNNING_ID.v());

                for (MediaEntity originMedia : currentMediaList) {
                    if (newMediaSet.contains(originMedia)) {
                        File originFile = new File(Objects.requireNonNull(originMedia.getFileUri().getPath()));
                        File movedFile = FileHelper.moveFile(originFile, targetDir);
                        emitter.onNext(new MediaEntity(Uri.fromFile(movedFile), accountId));    //不论是否成功都返回
                    } else {
                        emitter.onNext(originMedia);
                    }
                }

                emitter.onComplete();
            });

            //多线程执行任务并调用段落添加/更新方法
            List<MediaEntity> resultList = new ArrayList<>();   //移动结果列表（可能包含 null）
            disposable.add(moveTask
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            mediaEntity -> {
                                resultList.add(mediaEntity);
                                dialogBuilder.updateProgress(resultList.size(), currentMediaList.size(), "正在移动媒体文件……");
                            },
                            e -> {
                                ExceptionHelper.showExceptionDialog(this, e);
                                dialog.dismiss();
                            },
                            () -> {
                                dialog.dismiss();

                                //排除移动失败的文件
                                List<MediaEntity> succeedMediaList = resultList.stream()
                                        .filter(mediaEntity -> mediaEntity.getFileUri() != null)
                                        .collect(Collectors.toList());

                                //调用数据写入方法
                                saveData(succeedMediaList);
                            }
                    )
            );
        });
    }

    /**
     * 观察 ViewModel 的 LiveData
     */
    private void observeLiveData() {
        //媒体添加选项
        MediaAddOptionViewModel mediaAddOptionViewModel = new ViewModelProvider(this).get(MediaAddOptionViewModel.class);
        mediaAddOptionViewModel.getClickEvent().observe(this, integer -> {
            MediaAddOption option = MediaAddOption.values()[integer];

            if (option == MediaAddOption.TAKE_PICTURE) {
                if (PermissionHelper.isRuntimePermissionGranted(
                        Manifest.permission.CAMERA,
                        this
                )) {
                    launchSystemCamera();
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA);
                }
            } else if (option == MediaAddOption.OPEN_ALBUM) {
                albumLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );
            }
        });

        //标签选择
        TagMultiSelectViewModel tagMultiSelectViewModel = new ViewModelProvider(this).get(TagMultiSelectViewModel.class);
        tagMultiSelectViewModel.getNeedExecute().observe(this, b -> {
            if (b) {
                BookkeepingDb db = BookkeepingDb.getInstance(this);
                disposable.add(db.tagDao().getTagSingleById(tagMultiSelectViewModel.getCheckedTagIdSet())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                tagList -> {
                                    if (!tagList.isEmpty()) {
                                        tagAdapter.submitList(
                                                tagList,
                                                () -> VisibilityHelper.toggleViewExpansion(
                                                        binding.scrollLayout,
                                                        true,
                                                        null,
                                                        binding.tagRecycler
                                                )
                                        );
                                    } else {
                                        VisibilityHelper.toggleViewExpansion(
                                                binding.scrollLayout,
                                                false,
                                                () -> tagAdapter.submitList(tagList),
                                                binding.tagRecycler
                                        );
                                    }
                                },
                                e -> ExceptionHelper.showExceptionDialog(this, e)
                        )
                );
            }
        });
    }

    /**
     * 初始化启动器
     */
    private void initLaunchers() {
        //从相册选择图片启动器
        albumLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(),
                this::onAlbumPictureUrisReceived
        );

        //系统相机拍照启动器
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        MediaAddOptionViewModel viewModel = new ViewModelProvider(this)
                                .get(MediaAddOptionViewModel.class);
                        onCameraPictureUriReceived(viewModel.getCameraFileUri());
                    } else {
                        Toast.makeText(this, "拍照已取消", Toast.LENGTH_SHORT).show();

                        //删除刚刚创建的照片文件
                        MediaAddOptionViewModel viewModel = new ViewModelProvider(this)
                                .get(MediaAddOptionViewModel.class);
                        FileHelper.deleteFile(viewModel.getCameraFileUri(), this);
                    }
                }
        );

        //权限申请启动器
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        launchSystemCamera();
                    } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        //弹出解释对话框
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("申请权限")
                                .setMessage("使用相机拍照需要先授予摄像头权限")
                                .setNegativeButton("取消", null)
                                .setPositiveButton(
                                        "确定",
                                        (dialogInterface, i) ->
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                )
                                .show();
                    } else {
                        Toast.makeText(this, "请授予相机权限后再拍照", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * 初始化返回拦截逻辑
     */
    private void initOnBackPressedHandlers() {
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                backHelper.dispatchBackPressed();
            }
        };
        backHelper = new BackPressedCallbackHelper(backPressedCallback);
        getOnBackPressedDispatcher().addCallback(backPressedCallback);

        //媒体多选处理器
        selectionBackHandler = new BackPressedCallbackHelper.BackHandler() {
            @Override
            public boolean handleBack() {
                selectionTracker.clearSelection();

                backHelper.unregisterHandler(this);
                return true;
            }

            @Override
            public int getPriority() {
                return 3;
            }
        };
    }

    /**
     * 启动系统相机进行拍照
     */
    private void launchSystemCamera() {
        try {
            //在缓存目录下创建一个临时文件
            File photoFile = File.createTempFile(
                    "IMG_",
                    ".jpg",
                    DirectoryPaths.MEDIA_TEMP.getDir(this)
            );
            MediaAddOptionViewModel viewModel = new ViewModelProvider(this)
                    .get(MediaAddOptionViewModel.class);
            viewModel.setCameraFileUri(Uri.fromFile(photoFile));    //保存 File 类型的 Uri

            //通过 FileProvider 获取 Content URI
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );

            //启动相机
            SlyCoffer.lockLifecycleObserver();
            takePictureLauncher.launch(contentUri);
        } catch (IOException e) {
            Toast.makeText(this, "无法创建相片文件", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "请授予相机权限后再拍照", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 相机拍照成功的回调
     *
     * @param tempPictureUri 系统相机拍照后的临时图片 Uri
     */
    private void onCameraPictureUriReceived(Uri tempPictureUri) {
        //获取现有的列表
        List<MediaEntity> mediaList = new ArrayList<>(mediaAdapter.getCurrentList());

        //更新列表
        long accountId = initBundle == null ? 0 : initBundle.getLong(KeyStrings.RUNNING_ID.v());
        mediaList.add(new MediaEntity(tempPictureUri, accountId));
        mediaAdapter.submitList(mediaList);
    }

    /**
     * 相册图片的 Uri 接收回调
     *
     * @param uriList 用户选择的相册图片 Uri
     */
    private void onAlbumPictureUrisReceived(@NonNull List<Uri> uriList) {
        //判断是否为空
        if (uriList.isEmpty()) {
            Toast.makeText(this, "未选择媒体文件", Toast.LENGTH_SHORT).show();
            return;
        }

        //显示进度条对话框
        ProgressDialogBuilder builder = new ProgressDialogBuilder(
                this,
                "导入媒体",
                "正在复制媒体文件"
        );
        AlertDialog progressDialog = builder.
                setNegativeButton("取消", (dialogInterface, i) -> {
                    Toast.makeText(this, "已取消媒体导入", Toast.LENGTH_SHORT).show();
                    disposable.clear();
                })
                .show();

        //创建复制任务
        List<MediaEntity> mediaList = new ArrayList<>();
        Observable<Integer> task = Observable.create(emitter -> {
            File mediaDir = DirectoryPaths.MEDIA_TEMP.getDir(this);
            byte[] sharedBuffer = new byte[1024 * 32];  //共享32KB缓存

            //复制文件并保存引用
            for (Uri uri : uriList) {
                File resultFile = null;
                try {
                    resultFile = FileHelper.copyFile(this, uri, mediaDir, sharedBuffer);
                } catch (IOException e) {
                    emitter.onError(e);
                }
                if (resultFile == null) {
                    continue;
                }

                //保存到列表中
                Uri successfulUri = Uri.fromFile(resultFile);
                long accountId = initBundle == null ? 0 : initBundle.getLong(KeyStrings.RUNNING_ID.v());
                MediaEntity media = new MediaEntity(successfulUri, accountId);
                mediaList.add(media);

                //更新进度
                emitter.onNext(mediaList.size());
            }

            //完成任务
            emitter.onComplete();
        });

        //执行复制操作
        disposable.add(task
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        progress -> {
                            builder.setIndeterminate(false);
                            builder.updateProgress(progress, uriList.size(), "正在导入媒体……");
                        },
                        e -> {
                            ExceptionHelper.showExceptionDialog(this, e);
                            progressDialog.dismiss();
                        },
                        () -> {
                            Toast.makeText(
                                    this,
                                    "已导入" + mediaList.size() + "个媒体文件",
                                    Toast.LENGTH_SHORT
                            ).show();

                            //媒体文件显示在列表中
                            mediaList.addAll(0, mediaAdapter.getCurrentList());
                            mediaAdapter.submitList(mediaList);

                            progressDialog.dismiss();
                        }
                )
        );
    }

    /**
     * 校验输入内容合法性
     *
     * @return 错误提示，若无错误返回 null
     */
    private String verifyInput() {
        String err = null;
        String amount = String.valueOf(binding.amountInput.getText());
        String dateTimeStr = String.valueOf(binding.datetimeInput.getText());
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();

        if (amount.isEmpty()) {
            err = "金额不能为空";
            binding.amountLayout.setError(err);
        } else if (Double.parseDouble(amount) == 0.0) {
            err = "金额不能为0";
            binding.amountLayout.setError(err);
        } else if (dateTimeStr.isEmpty()) {
            err = "日期和时间不能为空";
            binding.datetimeLayout.setError(err);
        } else if (type == AccountType.TRANSFER && exportAccount.isEmpty()) {
            err = "转出账户不能为空";
            binding.exportAccountLayout.setError(err);
        } else if (type == AccountType.TRANSFER && importAccount.isEmpty()) {
            err = "转入账户不能为空";
            binding.importAccountLayout.setError(err);
        }

        return err;
    }

    /**
     * 将数据保存到数据库
     */
    private void saveData(List<MediaEntity> copiedMediaUriList) {
        //获取输入的数据
        double amount;
        try {
            amount = Double.parseDouble(String.valueOf(binding.amountInput.getText()));
        } catch (NumberFormatException e) {
            amount = 0;
        }
        String remark = String.valueOf(binding.remarkInput.getText());
        LocalDateTime dateTime = LocalDateTime.parse(
                String.valueOf(binding.datetimeInput.getText()),
                CustomDateTimeFormatter.DATE_TIME
        );
        int typeOrdinal = this.type.ordinal();
        String exportAccount = String.valueOf(binding.exportAccountInput.getText()).trim();
        String importAccount = String.valueOf(binding.importAccountInput.getText()).trim();

        //生成标签 ID 列表
        List<Long> tagIdList = tagAdapter.getCurrentList().stream()
                .map(TagEntity::getTagId)
                .collect(Collectors.toList());

        AccountEntity account = new AccountEntity(amount, remark, typeOrdinal, dateTime, AutoBookkeepingType.NONE.ordinal());
        AccountTransferEntity transfer = new AccountTransferEntity(exportAccount, importAccount);
        if (initBundle == null) {
            disposable.add(AccountService.addNewAccount(account, transfer, copiedMediaUriList, tagIdList, this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            accountId -> {
                                Toast.makeText(this, "流水记录添加成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> {
                                for (MediaEntity mediaEntity : copiedMediaUriList) {
                                    //删除新添加的媒体文件
                                    if (mediaEntity.getMediaId() == 0) {
                                        FileHelper.deleteFile(mediaEntity.getFileUri(), this);
                                    }
                                }
                                ExceptionHelper.showExceptionDialog(this, e);
                            }
                    )
            );
        } else {
            long accountId = initBundle.getLong(KeyStrings.RUNNING_ID.v());
            account.setAccountId(accountId);
            disposable.add(AccountService.modifyAccount(account, transfer, copiedMediaUriList, tagIdList, this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                                Toast.makeText(this, "流水记录修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> {
                                for (MediaEntity mediaEntity : copiedMediaUriList) {
                                    //删除新添加的媒体文件
                                    if (mediaEntity.getMediaId() == 0) {
                                        FileHelper.deleteFile(mediaEntity.getFileUri(), this);
                                    }
                                }
                                ExceptionHelper.showExceptionDialog(this, e);
                            }
                    )
            );
        }
    }

    /**
     * 显示时间选择对话框
     */
    private void selectDateTime() {
        //解析已输入的时间
        String datetimeStr = String.valueOf(binding.datetimeInput.getText());
        LocalDateTime inputDatetime = datetimeStr.isEmpty() ?
                LocalDateTime.now() :
                LocalDateTime.parse(datetimeStr, CustomDateTimeFormatter.DATE_TIME);
        LocalDate date = inputDatetime.toLocalDate();

        DateTimePickerHelper.selectDate(
                date,
                getSupportFragmentManager(),
                dateTimeMillis -> {
                    //组合日期和时间
                    LocalDate selectedDate = DateTimePickerHelper.getLocalDateFromTimeMilli(dateTimeMillis);
                    DateTimePickerHelper.selectTime(
                            inputDatetime,
                            getSupportFragmentManager(),
                            timePicker -> {
                                int hour = timePicker.getHour();
                                int minute = timePicker.getMinute();

                                LocalTime selectedTime = LocalTime.of(hour, minute);
                                LocalDateTime finalDatetime = selectedDate.atTime(selectedTime);
                                //修改文本框的日期和时间
                                binding.datetimeInput.setText(CustomDateTimeFormatter.DATE_TIME.format(finalDatetime));
                            }
                    );
                }
        );
    }

    /**
     * 设置媒体多选模式是否启用
     *
     * @param isSelectMode 是否在媒体多选模式
     */
    private void setSelectMode(boolean isSelectMode) {
        if (isSelectMode && binding.mediaDeleteBtn.getVisibility() == View.VISIBLE ||
                !isSelectMode && binding.mediaDeleteBtn.getVisibility() == View.GONE) {
            return;
        }

        //处理返回拦截
        if (isSelectMode) {
            backHelper.registerHandler(selectionBackHandler);
        } else {
            backHelper.unregisterHandler(selectionBackHandler);
        }

        //定义过渡动画
        TransitionSet set = new TransitionSet()
                .addTransition(new Fade())
                .addTarget(binding.mediaDeleteBtn)
                .addTarget(binding.mediaAddBtn)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setDuration(250);

        //通知布局即将发生变化
        TransitionManager.beginDelayedTransition(binding.mediaBtnLayout, set);

        //切换视图可见性
        if (isSelectMode) {
            binding.mediaDeleteBtn.setVisibility(View.VISIBLE);
            binding.mediaAddBtn.setVisibility(View.GONE);
        } else {
            binding.mediaDeleteBtn.setVisibility(View.GONE);
            binding.mediaAddBtn.setVisibility(View.VISIBLE);
        }

        if (binding.mediaRecycler.getAdapter() instanceof AccountMediaAdapter) {
            ((AccountMediaAdapter) binding.mediaRecycler.getAdapter()).setSelectMode(isSelectMode); //切换适配器选择模式
        }
    }
}