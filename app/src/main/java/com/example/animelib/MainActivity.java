package com.example.animelib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.adapters.VideoFilesAdapter;
import com.example.animelib.adapters.VideoFoldersAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.domain.VideoListFilter;
import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;
import com.example.animelib.repository.LocalVideoRepository;
import com.example.animelib.repository.VideoRepository;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC
    }

    private DatabaseManager databaseManager;
    private VideoRepository videoRepository;
    private final VideoListFilter videoListFilter = new VideoListFilter();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();

    // UI Components
    private TextView tvVideoCount;
    private ImageButton btnSort;
    private ImageButton btnRefresh;
    private EditText etSearchVideos;
    private ImageButton btnClearSearch;
    private RecyclerView rvFolders;
    private RecyclerView rvVideos;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnEmptyAction;

    // Data
    private final List<VideoItem> allVideos = new ArrayList<>();
    private final List<VideoItem> displayedVideos = new ArrayList<>();
    private final List<VideoFolder> foldersList = new ArrayList<>();

    private VideoFilesAdapter videoFilesAdapter;
    private VideoFoldersAdapter videoFoldersAdapter;

    private String selectedFolder = "Все";
    private String currentSearchQuery = "";
    private VideoListFilter.SortOrder currentSortOrder = VideoListFilter.SortOrder.DATE_DESC;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        databaseManager = DatabaseManager.getInstance(this);
        videoRepository = new LocalVideoRepository(this, databaseManager);
        ThemeUtils.applyTheme(databaseManager.loadThemeSetting());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();
        setupSearchAndListeners();
        setupPermissionLauncher();

        checkAndRequestStoragePermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            refreshWatchProgressOnly();
        }
    }

    private void initViews() {
        tvVideoCount = findViewById(R.id.tvVideoCount);
        btnSort = findViewById(R.id.btnSort);
        btnRefresh = findViewById(R.id.btnRefresh);
        etSearchVideos = findViewById(R.id.etSearchVideos);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvFolders = findViewById(R.id.rvFolders);
        rvVideos = findViewById(R.id.rvVideos);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnEmptyAction = findViewById(R.id.btnEmptyAction);

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.purple_primary));
        swipeRefreshLayout.setOnRefreshListener(this::loadVideosFromStorage);
    }

    private void setupRecyclerViews() {
        rvFolders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoFoldersAdapter = new VideoFoldersAdapter(foldersList, folder -> {
            selectedFolder = folder.getName();
            filterAndDisplayVideos();
        });
        rvFolders.setAdapter(videoFoldersAdapter);

        rvVideos.setLayoutManager(new LinearLayoutManager(this));
        videoFilesAdapter = new VideoFilesAdapter(displayedVideos, (video, position) -> openVideoInPlayer(video));
        rvVideos.setAdapter(videoFilesAdapter);
    }

    private void setupSearchAndListeners() {
        btnRefresh.setOnClickListener(v -> loadVideosFromStorage());
        btnSort.setOnClickListener(v -> showSortDialog());

        etSearchVideos.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterAndDisplayVideos();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearchVideos.setText(""));

        btnEmptyAction.setOnClickListener(v -> {
            if (!hasStoragePermission()) {
                requestStoragePermission();
            } else {
                loadVideosFromStorage();
            }
        });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        loadVideosFromStorage();
                    } else {
                        showPermissionDeniedState();
                    }
                }
        );
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void checkAndRequestStoragePermissions() {
        if (hasStoragePermission()) {
            loadVideosFromStorage();
        } else {
            requestStoragePermission();
        }
    }

    private void showPermissionDeniedState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvVideos.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvEmptyTitle.setText("Требуется доступ к видео");
        tvEmptySubtitle.setText("Приложению необходимо разрешение для поиска и воспроизведения локальных видеофайлов");
        btnEmptyAction.setText("Предоставить доступ");
    }

    private void loadVideosFromStorage() {
        if (!hasStoragePermission()) {
            showPermissionDeniedState();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        scanExecutor.execute(() -> {
            List<VideoItem> scanned = videoRepository.scanVideos();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                allVideos.clear();
                allVideos.addAll(scanned);

                updateFolderList();
                filterAndDisplayVideos();
            });
        });
    }

    private void refreshWatchProgressOnly() {
        scanExecutor.execute(() -> {
            boolean changed = videoRepository.refreshWatchProgress(allVideos);
            if (changed) {
                runOnUiThread(() -> {
                    if (videoFilesAdapter != null) {
                        videoFilesAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void updateFolderList() {
        List<VideoFolder> updatedFolders = videoListFilter.buildFolders(allVideos);
        foldersList.clear();
        foldersList.addAll(updatedFolders);
        videoFoldersAdapter.notifyDataSetChanged();
    }

    private void filterAndDisplayVideos() {
        List<VideoItem> filtered = videoListFilter.filterAndSort(
                allVideos,
                selectedFolder,
                currentSearchQuery,
                currentSortOrder
        );

        displayedVideos.clear();
        displayedVideos.addAll(filtered);
        videoFilesAdapter.notifyDataSetChanged();

        updateCountersAndEmptyState();
    }

    private void updateCountersAndEmptyState() {
        if (displayedVideos.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvVideos.setVisibility(View.GONE);
            if (!currentSearchQuery.isEmpty()) {
                tvEmptyTitle.setText("Ничего не найдено");
                tvEmptySubtitle.setText("По запросу «" + currentSearchQuery + "» видео не найдены");
                btnEmptyAction.setText("Очистить поиск");
                btnEmptyAction.setOnClickListener(v -> etSearchVideos.setText(""));
            } else {
                tvEmptyTitle.setText("Видеофайлы не найдены");
                tvEmptySubtitle.setText("Убедитесь, что в выбранной папке есть видеофайлы");
                btnEmptyAction.setText("Обновить список");
                btnEmptyAction.setOnClickListener(v -> loadVideosFromStorage());
            }
            tvVideoCount.setText("0 видео");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvVideos.setVisibility(View.VISIBLE);
            tvVideoCount.setText(String.format(Locale.getDefault(), "Найдено: %d видео", displayedVideos.size()));
        }
    }

    private void showSortDialog() {
        String[] options = new String[]{
                "Сначала новые (по дате)",
                "Сначала старые (по дате)",
                "По названию (А - Я)",
                "По размеру файла"
        };

        int checkedItem = 0;
        if (currentSortOrder == VideoListFilter.SortOrder.DATE_ASC) checkedItem = 1;
        else if (currentSortOrder == VideoListFilter.SortOrder.NAME_ASC) checkedItem = 2;
        else if (currentSortOrder == VideoListFilter.SortOrder.SIZE_DESC) checkedItem = 3;

        new AlertDialog.Builder(this)
                .setTitle("Сортировка видео")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0: currentSortOrder = VideoListFilter.SortOrder.DATE_DESC; break;
                        case 1: currentSortOrder = VideoListFilter.SortOrder.DATE_ASC; break;
                        case 2: currentSortOrder = VideoListFilter.SortOrder.NAME_ASC; break;
                        case 3: currentSortOrder = VideoListFilter.SortOrder.SIZE_DESC; break;
                    }
                    filterAndDisplayVideos();
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openVideoInPlayer(VideoItem video) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        String targetPath = video.getPath() != null && !video.getPath().isEmpty()
                ? video.getPath() : video.getUriString();

        intent.putExtra("EXTRA_LOCAL_FILE_PATH", targetPath);
        intent.putExtra("EXTRA_VIDEO_TITLE", video.getTitle());
        intent.putExtra("EXTRA_FOLDER_NAME", video.getFolderName());

        ArrayList<String> playlistPaths = new ArrayList<>();
        ArrayList<String> playlistTitles = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < displayedVideos.size(); i++) {
            VideoItem item = displayedVideos.get(i);
            String itemPath = item.getPath() != null && !item.getPath().isEmpty() ? item.getPath() : item.getUriString();
            playlistPaths.add(itemPath);
            playlistTitles.add(item.getTitle());
            if (item.getId() == video.getId() || targetPath.equals(itemPath)) {
                currentIndex = i;
            }
        }

        intent.putStringArrayListExtra("EXTRA_PLAYLIST_PATHS", playlistPaths);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_TITLES", playlistTitles);
        intent.putExtra("EXTRA_PLAYLIST_INDEX", currentIndex);

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            scanExecutor.shutdown();
        }
    }
}
