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
import android.graphics.Bitmap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.domain.VideoListFilter;
import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;
import com.example.animelib.repository.LocalVideoRepository;
import android.net.Uri;
import com.example.animelib.settings.PlaylistVideosBottomSheet;
import com.example.animelib.settings.VideoInfoBottomSheet;
import com.example.animelib.settings.VideoOptionsBottomSheet;
import com.example.animelib.ui.M3LinearProgressIndicator;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.DialogUtils;
import com.example.animelib.util.VideoThumbnailHelper;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ExplorerFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvVideos, rvFolders;
    private View layoutEmptyState, cardFolderCarousel, layoutControlButtonsContainer, layoutNormalButtons;
    private View layoutTopControls, layoutFloatingExplorerControls;
    private View layoutPlaylistCreationBar, layoutExpandedSearch;
    private TextView tvEmptyTitle, tvEmptySubtitle, tvPlaylistSelectionCount;
    private EditText etSearchVideos;
    private ImageButton btnSort, btnCreatePlaylist, btnSearchToggle, btnCloseSearch;
    private ImageButton btnCancelPlaylist, btnDonePlaylist;
    private com.google.android.material.button.MaterialButton btnEmptyAction;
    private ProgressBar progressBar;

    // Last Played Video block views
    private MaterialCardView cardLastPlayedVideo;
    private ImageView ivLastPlayedThumbnail;
    private TextView tvLastPlayedDuration;
    private TextView tvLastPlayedProgressPercent;
    private TextView tvLastPlayedTitle;
    private TextView tvLastPlayedSubtitle;
    private M3LinearProgressIndicator pbLastPlayedProgress;
    private View btnLastPlayedPlayCard;
    private ImageButton btnLastPlayedPlay;
    private View btnLastPlayedPlaylistCard;
    private ImageButton btnLastPlayedPlaylist;

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
    private int currentNavBarBottomInset = 0;
    private int currentImeBottomInset = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            updateWindowInsets(statusBarInsets.top, navBarInsets.bottom, imeInsets.bottom);
            return insets;
        });

        ViewCompat.setWindowInsetsAnimationCallback(view, new androidx.core.view.WindowInsetsAnimationCompat.Callback(androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP) {
            @NonNull
            @Override
            public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull java.util.List<androidx.core.view.WindowInsetsAnimationCompat> runningAnimations) {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                updateWindowInsets(statusBarInsets.top, navBarInsets.bottom, imeInsets.bottom);
                return insets;
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutExpandedSearch != null && layoutExpandedSearch.getVisibility() == View.VISIBLE) {
                    closeSearchAnimated();
                } else if (isPlaylistCreationMode) {
                    exitPlaylistCreationMode();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        databaseManager = DatabaseManager.getInstance(requireContext());
        videoRepository = new LocalVideoRepository(requireContext(), databaseManager);

        // Find views
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        rvVideos = view.findViewById(R.id.rvVideos);
        rvFolders = view.findViewById(R.id.rvFolders);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        cardFolderCarousel = view.findViewById(R.id.cardFolderCarousel);
        layoutTopControls = view.findViewById(R.id.layoutTopControls);
        layoutFloatingExplorerControls = view.findViewById(R.id.layoutFloatingExplorerControls);
        layoutControlButtonsContainer = view.findViewById(R.id.layoutControlButtonsContainer);
        layoutNormalButtons = view.findViewById(R.id.layoutNormalButtons);
        layoutPlaylistCreationBar = view.findViewById(R.id.layoutPlaylistCreationBar);
        layoutExpandedSearch = view.findViewById(R.id.layoutExpandedSearch);

        if (layoutTopControls != null) {
            layoutTopControls.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int newHeight = bottom - top;
                int oldHeight = oldBottom - oldTop;
                if (newHeight != oldHeight) {
                    applyListPaddings();
                }
            });
        }

        if (layoutFloatingExplorerControls != null) {
            layoutFloatingExplorerControls.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int newHeight = bottom - top;
                int oldHeight = oldBottom - oldTop;
                if (newHeight != oldHeight) {
                    applyListPaddings();
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

        cardLastPlayedVideo = view.findViewById(R.id.cardLastPlayedVideo);
        ivLastPlayedThumbnail = view.findViewById(R.id.ivLastPlayedThumbnail);
        tvLastPlayedDuration = view.findViewById(R.id.tvLastPlayedDuration);
        tvLastPlayedProgressPercent = view.findViewById(R.id.tvLastPlayedProgressPercent);
        tvLastPlayedTitle = view.findViewById(R.id.tvLastPlayedTitle);
        tvLastPlayedSubtitle = view.findViewById(R.id.tvLastPlayedSubtitle);
        pbLastPlayedProgress = view.findViewById(R.id.pbLastPlayedProgress);
        btnLastPlayedPlayCard = view.findViewById(R.id.btnLastPlayedPlayCard);
        btnLastPlayedPlay = view.findViewById(R.id.btnLastPlayedPlay);
        btnLastPlayedPlaylistCard = view.findViewById(R.id.btnLastPlayedPlaylistCard);
        btnLastPlayedPlaylist = view.findViewById(R.id.btnLastPlayedPlaylist);

        setupRecyclerViews();
        setupListeners();
        loadAndDisplayLastPlayedVideo();

        if (hasStoragePermission()) {
            loadVideosFromStorage();
        } else {
            showPermissionDeniedState();
        }
    }

    private void applyListPaddings() {
        if (rvVideos == null || !isAdded()) return;
        float density = getResources().getDisplayMetrics().density;

        int topMargin = currentTopInset + (int) (8 * density);
        int topControlsHeight = (layoutTopControls != null && layoutTopControls.getHeight() > 0)
                ? layoutTopControls.getHeight()
                : (int) (52 * density);
        int totalTopPadding = topMargin + topControlsHeight + (int) (14 * density);

        int targetBottomMargin;
        if (currentImeBottomInset > currentNavBarBottomInset) {
            targetBottomMargin = currentImeBottomInset + (int) (12 * density);
        } else {
            targetBottomMargin = currentNavBarBottomInset + (int) (88 * density);
        }
        int bottomControlsHeight = (layoutFloatingExplorerControls != null && layoutFloatingExplorerControls.getHeight() > 0)
                ? layoutFloatingExplorerControls.getHeight()
                : (int) (64 * density);
        int totalBottomPadding = targetBottomMargin + bottomControlsHeight + (int) (16 * density);

        rvVideos.setPadding(0, totalTopPadding, 0, totalBottomPadding);
        rvVideos.setClipToPadding(false);
        rvVideos.setClipChildren(false);
    }

    public void updateWindowInsets(int topInset, int navBarBottomInset, int imeBottomInset) {
        this.currentTopInset = topInset;
        this.currentNavBarBottomInset = navBarBottomInset;
        this.currentImeBottomInset = imeBottomInset;
        if (getView() == null || !isAdded()) return;

        float density = getResources().getDisplayMetrics().density;

        if (layoutTopControls != null && layoutTopControls.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutTopControls.getLayoutParams();
            int targetTopMargin = topInset + (int) (8 * density);
            if (lp.topMargin != targetTopMargin) {
                lp.topMargin = targetTopMargin;
                layoutTopControls.setLayoutParams(lp);
            }
        }

        int targetBottomMargin;
        if (imeBottomInset > navBarBottomInset) {
            targetBottomMargin = imeBottomInset + (int) (12 * density);
        } else {
            targetBottomMargin = navBarBottomInset + (int) (88 * density);
        }

        if (layoutFloatingExplorerControls != null && layoutFloatingExplorerControls.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFloatingExplorerControls.getLayoutParams();
            if (lp.bottomMargin != targetBottomMargin) {
                lp.bottomMargin = targetBottomMargin;
                layoutFloatingExplorerControls.setLayoutParams(lp);
            }
        }

        applyListPaddings();
    }

    public void updateWindowInsets(int topInset, int bottomInset) {
        updateWindowInsets(topInset, bottomInset, currentImeBottomInset);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            loadVideosFromStorage();
        }
        loadAndDisplayLastPlayedVideo();
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
        videoFilesAdapter = new VideoFilesAdapter(displayedVideos, new VideoFilesAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                if (isPlaylistCreationMode) {
                    videoFilesAdapter.toggleSelection(video);
                    updatePlaylistSelectionBar();
                } else {
                    openVideoInPlayer(video);
                }
            }

            @Override
            public void onVideoMoreClick(VideoItem video, int position) {
                showVideoOptionsBottomSheet(video);
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
        videoFilesAdapter.updateVideos(displayedVideos);

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

    private void showVideoOptionsBottomSheet(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        VideoOptionsBottomSheet bs = new VideoOptionsBottomSheet(requireContext(), video, new VideoOptionsBottomSheet.VideoActionCallback() {
            @Override
            public void onShare(VideoItem video) {
                shareVideo(video);
            }

            @Override
            public void onAddToPlaylist(VideoItem video) {
                showAddToPlaylistDialog(video);
            }

            @Override
            public void onRename(VideoItem video) {
                showRenameVideoDialog(video);
            }

            @Override
            public void onInfo(VideoItem video) {
                showVideoInfoDialog(video);
            }

            @Override
            public void onDelete(VideoItem video) {
                confirmDeleteVideo(video);
            }
        });
        bs.show();
    }

    private void shareVideo(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/*");
            Uri uri = video.getUri();
            if (uri == null && video.getPath() != null) {
                File file = new File(video.getPath());
                uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file
                );
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Поделиться видео"));
        } catch (Exception e) {
            CustomToast.showWarning(requireContext(), "Не удалось поделиться видео");
        }
    }

    private void showAddToPlaylistDialog(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PlaylistEntity> playlists = databaseManager.getAllPlaylists();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                ArrayList<String> options = new ArrayList<>();
                options.add("＋ Новый плейлист");
                for (PlaylistEntity p : playlists) {
                    options.add(p.getName());
                }
                DialogUtils.showListDialog(requireContext(), "Добавить в плейлист", options.toArray(new String[0]), index -> {
                    if (index == 0) {
                        DialogUtils.showInputDialog(requireContext(), "Новый плейлист", "Введите название плейлиста:", "", "Название", "Создать", "Отмена", new DialogUtils.InputDialogListener() {
                            @Override
                            public void onInputConfirmed(String input) {
                                if (input == null || input.trim().isEmpty()) return;
                                ArrayList<VideoItem> single = new ArrayList<>();
                                single.add(video);
                                databaseManager.createPlaylist(input.trim(), single);
                                CustomToast.showSuccess(requireContext(), "Плейлист создан и видео добавлено");
                            }

                            @Override
                            public void onCancelled() {}
                        });
                    } else {
                        PlaylistEntity chosen = playlists.get(index - 1);
                        String path = video.getPath() != null && !video.getPath().isEmpty() ? video.getPath() : video.getUriString();
                        databaseManager.addVideoToPlaylist(chosen.getId(), path, video.getTitle());
                        CustomToast.showSuccess(requireContext(), "Добавлено в " + chosen.getName());
                    }
                });
            });
        });
    }

    private void showRenameVideoDialog(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        String currentName = video.getDisplayTitle();
        DialogUtils.showInputDialog(requireContext(), "Переименовать видео", "Введите новое имя:", currentName, "Имя файла", "Сохранить", "Отмена", new DialogUtils.InputDialogListener() {
            @Override
            public void onInputConfirmed(String input) {
                if (input == null || input.trim().isEmpty() || input.trim().equals(currentName)) return;
                String newName = input.trim();
                String ext = video.getFileExtension();
                String fullNewName = !ext.isEmpty() ? newName + "." + ext.toLowerCase(Locale.ROOT) : newName;

                if (video.getPath() != null) {
                    try {
                        File file = new File(video.getPath());
                        if (file.exists()) {
                            File newFile = new File(file.getParentFile(), fullNewName);
                            file.renameTo(newFile);
                        }
                    } catch (Exception ignored) {}
                }

                video.setTitle(fullNewName);
                databaseManager.updateVideoTitle(video.getPath(), fullNewName);
                videoFilesAdapter.updateVideos(displayedVideos);
                loadAndDisplayLastPlayedVideo();
                CustomToast.showSuccess(requireContext(), "Видео переименовано");
            }

            @Override
            public void onCancelled() {}
        });
    }

    private void showVideoInfoDialog(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        VideoInfoBottomSheet infoBs = new VideoInfoBottomSheet(requireContext(), video);
        infoBs.show();
    }

    private void confirmDeleteVideo(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        DialogUtils.showConfirmDialog(
                requireContext(),
                "Удалить видео?",
                "Вы действительно хотите удалить «" + video.getDisplayTitle() + "»? Это действие нельзя отменить.",
                "Удалить",
                "Отмена",
                true,
                new DialogUtils.DialogListener() {
                    @Override
                    public void onPositiveClick() {
                        deleteVideoFile(video);
                    }

                    @Override
                    public void onNegativeClick() {}
                }
        );
    }

    private void deleteVideoFile(VideoItem video) {
        if (!isAdded() || getContext() == null || video == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (video.getPath() != null) {
                try {
                    File file = new File(video.getPath());
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception ignored) {}
            }
            if (video.getUri() != null && getContext() != null) {
                try {
                    requireContext().getContentResolver().delete(video.getUri(), null, null);
                } catch (Exception ignored) {}
            }
            databaseManager.deleteVideoEverywhere(video.getPath());

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                allVideos.remove(video);
                displayedVideos.remove(video);
                videoFilesAdapter.updateVideos(displayedVideos);
                foldersList.clear();
                foldersList.addAll(videoFilter.buildFolders(allVideos));
                videoFoldersAdapter.notifyDataSetChanged();
                loadAndDisplayLastPlayedVideo();
                CustomToast.showSuccess(requireContext(), "Видео удалено");
            });
        });
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
        if (cardLastPlayedVideo != null) cardLastPlayedVideo.setVisibility(View.GONE);
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
        loadAndDisplayLastPlayedVideo();
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

    private void loadAndDisplayLastPlayedVideo() {
        if (!isAdded() || getContext() == null || databaseManager == null) return;
        if (isPlaylistCreationMode) {
            if (cardLastPlayedVideo != null) cardLastPlayedVideo.setVisibility(View.GONE);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            VideoPlaybackHistoryEntity lastVideo = databaseManager.getLastPlayedVideo();
            if (lastVideo == null || lastVideo.getFilePath() == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (cardLastPlayedVideo != null) cardLastPlayedVideo.setVisibility(View.GONE);
                    });
                }
                return;
            }

            File file = new File(lastVideo.getFilePath());
            if (!file.exists()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (cardLastPlayedVideo != null) cardLastPlayedVideo.setVisibility(View.GONE);
                    });
                }
                return;
            }

            PlaylistEntity playlist = databaseManager.getPlaylistForVideoPath(lastVideo.getFilePath());
            Bitmap thumb = VideoThumbnailHelper.createFrameThumbnail(getContext(), lastVideo.getFilePath(), null, lastVideo.getDurationMs());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || cardLastPlayedVideo == null || isPlaylistCreationMode) return;

                    cardLastPlayedVideo.setVisibility(View.VISIBLE);

                    if (thumb != null) {
                        ivLastPlayedThumbnail.setImageBitmap(thumb);
                    } else {
                        ivLastPlayedThumbnail.setImageResource(R.drawable.player_item_background);
                    }

                    if (lastVideo.getDurationMs() > 0) {
                        long totalSec = lastVideo.getDurationMs() / 1000;
                        long min = totalSec / 60;
                        long sec = totalSec % 60;
                        tvLastPlayedDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
                        tvLastPlayedDuration.setVisibility(View.VISIBLE);
                    } else {
                        tvLastPlayedDuration.setVisibility(View.GONE);
                    }

                    int percent = lastVideo.getProgressPercentage();
                    if (percent > 0) {
                        tvLastPlayedProgressPercent.setText(percent + "%");
                        tvLastPlayedProgressPercent.setVisibility(View.VISIBLE);
                        pbLastPlayedProgress.setVisibility(View.VISIBLE);
                        pbLastPlayedProgress.setProgress(percent, false);
                    } else {
                        tvLastPlayedProgressPercent.setVisibility(View.GONE);
                        pbLastPlayedProgress.setVisibility(View.GONE);
                    }

                    String title = lastVideo.getTitle();
                    if (title == null || title.trim().isEmpty()) {
                        title = file.getName();
                    }
                    if (title != null && title.contains(".")) {
                        int dot = title.lastIndexOf('.');
                        if (dot > 0) {
                            title = title.substring(0, dot);
                        }
                    }
                    tvLastPlayedTitle.setText(title);

                    String subtitle;
                    if (lastVideo.getLastPositionMs() > 0) {
                        long curSec = lastVideo.getLastPositionMs() / 1000;
                        long curMin = curSec / 60;
                        long s = curSec % 60;
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", curMin, s);
                        subtitle = "Продолжить с " + timeStr + (percent > 0 ? " • " + percent + "%" : "");
                    } else {
                        subtitle = "Начать просмотр" + (lastVideo.getFolderName() != null && !lastVideo.getFolderName().isEmpty() ? " • " + lastVideo.getFolderName() : "");
                    }
                    tvLastPlayedSubtitle.setText(subtitle);

                    View.OnClickListener playListener = v -> playLastVideo(lastVideo, playlist);
                    cardLastPlayedVideo.setOnClickListener(playListener);
                    btnLastPlayedPlayCard.setOnClickListener(playListener);
                    btnLastPlayedPlay.setOnClickListener(playListener);

                    if (playlist != null) {
                        btnLastPlayedPlaylistCard.setVisibility(View.VISIBLE);
                        View.OnClickListener playlistClickListener = v -> {
                            PlaylistVideosBottomSheet bs = new PlaylistVideosBottomSheet(requireContext(), playlist);
                            bs.show();
                        };
                        btnLastPlayedPlaylistCard.setOnClickListener(playlistClickListener);
                        btnLastPlayedPlaylist.setOnClickListener(playlistClickListener);
                    } else {
                        btnLastPlayedPlaylistCard.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void playLastVideo(VideoPlaybackHistoryEntity lastVideo, PlaylistEntity playlist) {
        if (!isAdded() || getContext() == null || lastVideo == null) return;

        if (playlist != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                List<PlaylistVideoEntity> videoEntities = databaseManager.getVideosForPlaylist(playlist.getId());
                if (videoEntities != null && !videoEntities.isEmpty() && getActivity() != null) {
                    ArrayList<VideoItem> playlistVideos = new ArrayList<>();
                    int startIndex = 0;
                    long tempId = 1;
                    for (int i = 0; i < videoEntities.size(); i++) {
                        PlaylistVideoEntity entity = videoEntities.get(i);
                        String path = entity.getVideoPath();
                        File file = new File(path);
                        String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
                        if (path.equals(lastVideo.getFilePath())) {
                            startIndex = i;
                        }
                        playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), playlist.getName()));
                    }

                    int finalStartIndex = startIndex;
                    getActivity().runOnUiThread(() -> {
                        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                        intent.putExtra("VIDEO_LIST", playlistVideos);
                        intent.putExtra("START_INDEX", finalStartIndex);
                        intent.putExtra("PLAYLIST_NAME", playlist.getName());
                        intent.putExtra("EXTRA_PLAYLIST_ID", playlist.getId());
                        startActivity(intent);
                    });
                    return;
                }

                // Fallback to single video play if playlist has no videos
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> playSingleOrFromAll(lastVideo));
                }
            });
        } else {
            playSingleOrFromAll(lastVideo);
        }
    }

    private void playSingleOrFromAll(VideoPlaybackHistoryEntity lastVideo) {
        if (!isAdded() || getContext() == null || lastVideo == null) return;

        int foundIndex = -1;
        for (int i = 0; i < displayedVideos.size(); i++) {
            if (lastVideo.getFilePath().equals(displayedVideos.get(i).getPath())) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex != -1) {
            Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra("VIDEO_LIST", new ArrayList<>(displayedVideos));
            intent.putExtra("START_INDEX", foundIndex);
            startActivity(intent);
        } else {
            for (int i = 0; i < allVideos.size(); i++) {
                if (lastVideo.getFilePath().equals(allVideos.get(i).getPath())) {
                    foundIndex = i;
                    break;
                }
            }
            if (foundIndex != -1) {
                Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                intent.putExtra("VIDEO_LIST", new ArrayList<>(allVideos));
                intent.putExtra("START_INDEX", foundIndex);
                startActivity(intent);
            } else {
                File file = new File(lastVideo.getFilePath());
                ArrayList<VideoItem> singleList = new ArrayList<>();
                singleList.add(new VideoItem(1, lastVideo.getTitle() != null ? lastVideo.getTitle() : file.getName(), lastVideo.getFilePath(), null, lastVideo.getDurationMs(), file.length(), file.lastModified(), lastVideo.getFolderName()));
                Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                intent.putExtra("VIDEO_LIST", singleList);
                intent.putExtra("START_INDEX", 0);
                startActivity(intent);
            }
        }
    }
}
