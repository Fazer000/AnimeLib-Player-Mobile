package com.example.animelib.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.R;
import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.adapters.VideoFilesAdapter;
import com.example.animelib.adapters.VideoFoldersAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.domain.VideoListFilter;
import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;
import com.example.animelib.repository.LocalVideoRepository;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.DialogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExplorerFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvVideos, rvFolders;
    private View layoutEmptyState, cardFolderCarousel, layoutControlButtonsContainer, layoutNormalButtons;
    private View layoutPlaylistCreationBar, layoutExpandedSearch;
    private TextView tvEmptyTitle, tvEmptySubtitle, tvPlaylistSelectionCount;
    private EditText etSearchVideos;
    private ImageButton btnSort, btnCreatePlaylist, btnSearchToggle, btnCloseSearch;
    private ImageButton btnCancelPlaylist, btnDonePlaylist;
    private com.google.android.material.button.MaterialButton btnEmptyAction;
    private ProgressBar progressBar;

    private VideoFilesAdapter videoFilesAdapter;
    private VideoFoldersAdapter videoFoldersAdapter;

    private final List<VideoItem> allVideos = new ArrayList<>();
    private final List<VideoItem> displayedVideos = new ArrayList<>();
    private final List<VideoFolder> foldersList = new ArrayList<>();

    private DatabaseManager databaseManager;
    private LocalVideoRepository videoRepository;
    private final VideoListFilter videoFilter = new VideoListFilter();

    private String selectedFolder = "Все";
    private String currentSearchQuery = "";
    private boolean isPlaylistCreationMode = false;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = false;
                    for (Boolean isGranted : result.values()) {
                        if (isGranted) {
                            granted = true;
                            break;
                        }
                    }
                    if (granted) {
                        loadVideosFromStorage();
                    } else {
                        showPermissionDeniedState();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorer, container, false);
    }

    private int currentTopInset = 0;
    private int currentBottomInset = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            updateWindowInsets(statusBarInsets.top, navBarInsets.bottom);
            return insets;
        });

        databaseManager = DatabaseManager.getInstance(requireContext());
        videoRepository = new LocalVideoRepository(requireContext(), databaseManager);

        // Find views
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        rvVideos = view.findViewById(R.id.rvVideos);
        rvFolders = view.findViewById(R.id.rvFolders);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        cardFolderCarousel = view.findViewById(R.id.cardFolderCarousel);
        layoutControlButtonsContainer = view.findViewById(R.id.layoutControlButtonsContainer);
        layoutNormalButtons = view.findViewById(R.id.layoutNormalButtons);
        layoutPlaylistCreationBar = view.findViewById(R.id.layoutPlaylistCreationBar);
        layoutExpandedSearch = view.findViewById(R.id.layoutExpandedSearch);

        View layoutFloatingExplorerControls = view.findViewById(R.id.layoutFloatingExplorerControls);
        if (layoutFloatingExplorerControls != null) {
            layoutFloatingExplorerControls.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (bottom != oldBottom || top != oldTop) {
                    updateWindowInsets(currentTopInset, currentBottomInset);
                }
            });
        }

        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
        tvPlaylistSelectionCount = view.findViewById(R.id.tvPlaylistSelectionCount);
        btnEmptyAction = view.findViewById(R.id.btnEmptyAction);

        etSearchVideos = view.findViewById(R.id.etSearchVideos);
        btnSort = view.findViewById(R.id.btnSort);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);
        btnSearchToggle = view.findViewById(R.id.btnSearchToggle);
        btnCloseSearch = view.findViewById(R.id.btnCloseSearch);
        btnCancelPlaylist = view.findViewById(R.id.btnCancelPlaylist);
        btnDonePlaylist = view.findViewById(R.id.btnDonePlaylist);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerViews();
        setupListeners();

        if (hasStoragePermission()) {
            loadVideosFromStorage();
        } else {
            showPermissionDeniedState();
        }
    }

    public void updateWindowInsets(int topInset, int bottomInset) {
        this.currentTopInset = topInset;
        this.currentBottomInset = bottomInset;
        if (getView() == null) return;

        float density = getResources().getDisplayMetrics().density;
        int topPadding = topInset + (int) (12 * density);

        View layoutFloatingExplorerControls = getView().findViewById(R.id.layoutFloatingExplorerControls);
        int targetBottomMargin = bottomInset + (int) (88 * density);

        if (layoutFloatingExplorerControls != null && layoutFloatingExplorerControls.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFloatingExplorerControls.getLayoutParams();
            if (lp.bottomMargin != targetBottomMargin) {
                lp.bottomMargin = targetBottomMargin;
                layoutFloatingExplorerControls.setLayoutParams(lp);
            }
        }

        if (rvVideos != null) {
            int controlsHeight = layoutFloatingExplorerControls != null ? layoutFloatingExplorerControls.getHeight() : 0;
            int totalPaddingBottom = targetBottomMargin + controlsHeight + (int) (16 * density);
            rvVideos.setPadding(0, topPadding, 0, totalPaddingBottom);
            rvVideos.setClipToPadding(false);
            rvVideos.setClipChildren(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            loadVideosFromStorage();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_VIDEO});
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
        }
    }

    private void setupRecyclerViews() {
        rvFolders.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        videoFoldersAdapter = new VideoFoldersAdapter(foldersList, folder -> {
            selectedFolder = folder.getName();
            filterAndDisplayVideos();
        });
        rvFolders.setAdapter(videoFoldersAdapter);

        rvVideos.setLayoutManager(new LinearLayoutManager(requireContext()));
        videoFilesAdapter = new VideoFilesAdapter(displayedVideos, (video, position) -> {
            if (isPlaylistCreationMode) {
                videoFilesAdapter.toggleSelection(video);
                updatePlaylistSelectionBar();
            } else {
                openVideoInPlayer(video);
            }
        });
        rvVideos.setAdapter(videoFilesAdapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadVideosFromStorage();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    private void setupListeners() {
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortDialog());
        }
        if (btnCreatePlaylist != null) {
            btnCreatePlaylist.setOnClickListener(v -> startPlaylistCreationMode());
        }
        if (btnSearchToggle != null) {
            btnSearchToggle.setOnClickListener(v -> openSearchAnimated());
        }
        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> closeSearchAnimated());
        }
        if (btnCancelPlaylist != null) {
            btnCancelPlaylist.setOnClickListener(v -> exitPlaylistCreationMode());
        }
        if (btnDonePlaylist != null) {
            btnDonePlaylist.setOnClickListener(v -> promptForPlaylistNameAndSave());
        }

        etSearchVideos.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                filterAndDisplayVideos();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnEmptyAction != null) {
            btnEmptyAction.setOnClickListener(v -> {
                if (!hasStoragePermission()) {
                    requestStoragePermission();
                } else {
                    loadVideosFromStorage();
                }
            });
        }
    }

    private void openSearchAnimated() {
        if (layoutExpandedSearch == null || layoutNormalButtons == null) return;

        if (layoutControlButtonsContainer instanceof ViewGroup) {
            TransitionManager.beginDelayedTransition((ViewGroup) layoutControlButtonsContainer, new AutoTransition().setDuration(220));
        }

        layoutNormalButtons.setVisibility(View.GONE);
        layoutExpandedSearch.setVisibility(View.VISIBLE);

        layoutExpandedSearch.post(() -> {
            if (!isAdded() || getView() == null) return;
            int cx = layoutExpandedSearch.getRight() - (int) (28 * getResources().getDisplayMetrics().density);
            int cy = layoutExpandedSearch.getHeight() / 2;
            float finalRadius = (float) Math.hypot(layoutExpandedSearch.getWidth(), layoutExpandedSearch.getHeight());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && cx > 0 && cy > 0) {
                try {
                    Animator anim = ViewAnimationUtils.createCircularReveal(layoutExpandedSearch, cx, cy, 0f, finalRadius);
                    anim.setDuration(280);
                    anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim.start();
                } catch (Exception ignored) {}
            }
        });

        etSearchVideos.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchVideos, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeSearchAnimated() {
        if (layoutExpandedSearch == null || layoutNormalButtons == null) return;

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchVideos.getWindowToken(), 0);
        }

        int cx = layoutExpandedSearch.getRight() - (int) (28 * getResources().getDisplayMetrics().density);
        int cy = layoutExpandedSearch.getHeight() / 2;
        float initialRadius = (float) Math.hypot(layoutExpandedSearch.getWidth(), layoutExpandedSearch.getHeight());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && layoutExpandedSearch.isAttachedToWindow() && cx > 0 && cy > 0) {
            try {
                Animator anim = ViewAnimationUtils.createCircularReveal(layoutExpandedSearch, cx, cy, initialRadius, 0f);
                anim.setDuration(220);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        etSearchVideos.setText("");
                        layoutExpandedSearch.setVisibility(View.GONE);
                        if (layoutControlButtonsContainer instanceof ViewGroup) {
                            TransitionManager.beginDelayedTransition((ViewGroup) layoutControlButtonsContainer, new AutoTransition().setDuration(180));
                        }
                        layoutNormalButtons.setVisibility(View.VISIBLE);
                    }
                });
                anim.start();
                return;
            } catch (Exception ignored) {}
        }

        etSearchVideos.setText("");
        if (layoutControlButtonsContainer instanceof ViewGroup) {
            TransitionManager.beginDelayedTransition((ViewGroup) layoutControlButtonsContainer, new AutoTransition().setDuration(200));
        }
        layoutExpandedSearch.setVisibility(View.GONE);
        layoutNormalButtons.setVisibility(View.VISIBLE);
    }

    public void loadVideosFromStorage() {
        if (getContext() == null || videoRepository == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> scannedVideos = videoRepository.scanVideos();
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                allVideos.clear();
                allVideos.addAll(scannedVideos);

                foldersList.clear();
                foldersList.addAll(videoFilter.buildFolders(allVideos));
                videoFoldersAdapter.notifyDataSetChanged();

                filterAndDisplayVideos();
            });
        });
    }

    private void filterAndDisplayVideos() {
        VideoListFilter.SortOrder sortOrder = VideoListFilter.SortOrder.DATE_DESC;

        List<VideoItem> filtered = videoFilter.filterAndSort(
                allVideos,
                selectedFolder,
                currentSearchQuery,
                sortOrder
        );

        displayedVideos.clear();
        displayedVideos.addAll(filtered);
        videoFilesAdapter.notifyDataSetChanged();

        if (displayedVideos.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvVideos.setVisibility(View.GONE);
            tvEmptyTitle.setText("Видео не найдены");
            tvEmptySubtitle.setText("Проверьте наличие видеофайлов на устройстве или добавьте видео в папку");
            btnEmptyAction.setText("Обновить");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvVideos.setVisibility(View.VISIBLE);
        }
    }

    private void showPermissionDeniedState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvVideos.setVisibility(View.GONE);
        tvEmptyTitle.setText("Доступ к файлам не предоставлен");
        tvEmptySubtitle.setText("Предоставьте разрешение на чтение видеофайлов для сканирования локального хранилища");
        btnEmptyAction.setText("Предоставить доступ");
    }

    private void openVideoInPlayer(VideoItem video) {
        int index = displayedVideos.indexOf(video);
        if (index == -1) index = 0;

        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.putExtra("VIDEO_LIST", new ArrayList<>(displayedVideos));
        intent.putExtra("START_INDEX", index);
        startActivity(intent);
    }

    private void showSortDialog() {
        String[] sortOptions = new String[]{
                "По имени (А-Я)",
                "По дате (сначала новые)",
                "По дате (сначала старые)",
                "По размеру (сначала большие)"
        };
        DialogUtils.showSingleChoiceDialog(requireContext(), "Сортировка видео", sortOptions, 1, which -> {
            filterAndDisplayVideos();
        });
    }

    private void startPlaylistCreationMode() {
        isPlaylistCreationMode = true;
        layoutPlaylistCreationBar.setVisibility(View.VISIBLE);
        btnSort.setVisibility(View.GONE);
        btnCreatePlaylist.setVisibility(View.GONE);
        videoFilesAdapter.setSelectionMode(true);
        updatePlaylistSelectionBar();
        CustomToast.showInfo(requireContext(), "Выберите видео по порядку для создания плейлиста");
    }

    private void exitPlaylistCreationMode() {
        isPlaylistCreationMode = false;
        layoutPlaylistCreationBar.setVisibility(View.GONE);
        btnSort.setVisibility(View.VISIBLE);
        btnCreatePlaylist.setVisibility(View.VISIBLE);
        videoFilesAdapter.setSelectionMode(false);
    }

    private void updatePlaylistSelectionBar() {
        int selectedCount = videoFilesAdapter.getSelectedVideos().size();
        tvPlaylistSelectionCount.setText("Выбрано: " + selectedCount + " видео");
    }

    private void promptForPlaylistNameAndSave() {
        List<VideoItem> selected = videoFilesAdapter.getSelectedVideos();
        if (selected.isEmpty()) {
            CustomToast.showWarning(requireContext(), "Выберите хотя бы одно видео");
            return;
        }

        DialogUtils.showInputDialog(requireContext(), "Название плейлиста", "Введите имя плейлиста", "", "Имя плейлиста", "Создать", "Отмена", new DialogUtils.InputDialogListener() {
            @Override
            public void onInputConfirmed(String input) {
                if (input == null || input.trim().isEmpty()) {
                    CustomToast.showWarning(requireContext(), "Имя плейлиста не может быть пустым");
                    return;
                }

                long id = databaseManager.createPlaylist(input.trim(), selected);
                if (id > 0) {
                    CustomToast.showSuccess(requireContext(), "Плейлист «" + input + "» создан!");
                    exitPlaylistCreationMode();
                } else {
                    CustomToast.showWarning(requireContext(), "Ошибка создания плейлиста");
                }
            }

            @Override
            public void onCancelled() {}
        });
    }
}
