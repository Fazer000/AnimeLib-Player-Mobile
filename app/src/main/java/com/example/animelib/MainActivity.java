package com.example.animelib;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.adapters.VideoFilesAdapter;
import com.example.animelib.adapters.VideoFoldersAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC
    }

    private DatabaseManager databaseManager;
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
    private SortOrder currentSortOrder = SortOrder.DATE_DESC;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        databaseManager = DatabaseManager.getInstance(this);
        // Apply saved theme
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
        // Update watch progress if videos were watched
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
        // Folders list
        rvFolders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoFoldersAdapter = new VideoFoldersAdapter(foldersList, folder -> {
            selectedFolder = folder.getName();
            filterAndDisplayVideos();
        });
        rvFolders.setAdapter(videoFoldersAdapter);

        // Videos list
        rvVideos.setLayoutManager(new LinearLayoutManager(this));
        videoFilesAdapter = new VideoFilesAdapter(displayedVideos, (video, position) -> {
            openVideoInPlayer(video);
        });
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

        btnClearSearch.setOnClickListener(v -> {
            etSearchVideos.setText("");
        });

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
            List<VideoItem> scanned = scanDeviceVideos();

            // Load progress from database for each video
            for (VideoItem item : scanned) {
                if (item.getPath() != null) {
                    VideoPlaybackHistoryEntity history = databaseManager.getVideoHistory(item.getPath());
                    if (history != null) {
                        item.setLastPositionMs(history.getLastPositionMs());
                    }
                }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                allVideos.clear();
                allVideos.addAll(scanned);

                buildFolderList();
                filterAndDisplayVideos();
            });
        });
    }

    private void refreshWatchProgressOnly() {
        scanExecutor.execute(() -> {
            boolean changed = false;
            for (VideoItem item : allVideos) {
                if (item.getPath() != null) {
                    VideoPlaybackHistoryEntity history = databaseManager.getVideoHistory(item.getPath());
                    if (history != null && history.getLastPositionMs() != item.getLastPositionMs()) {
                        item.setLastPositionMs(history.getLastPositionMs());
                        changed = true;
                    }
                }
            }
            if (changed) {
                runOnUiThread(() -> {
                    if (videoFilesAdapter != null) {
                        videoFilesAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private List<VideoItem> scanDeviceVideos() {
        List<VideoItem> list = new ArrayList<>();
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? MediaStore.Video.Media.BUCKET_DISPLAY_NAME : MediaStore.Video.Media.DATA
        };

        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = getContentResolver().query(collection, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
                int bucketColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                        : dataColumn;

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(dataColumn);
                    long duration = cursor.getLong(durationColumn);
                    long size = cursor.getLong(sizeColumn);
                    long dateModified = cursor.getLong(dateModifiedColumn);

                    String folderName;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        folderName = cursor.getString(bucketColumn);
                    } else {
                        folderName = extractFolderNameFromPath(path);
                    }

                    if (folderName == null || folderName.isEmpty()) {
                        folderName = extractFolderNameFromPath(path);
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    if (name == null && path != null) {
                        name = new File(path).getName();
                    }

                    list.add(new VideoItem(id, name, path, contentUri, duration, size, dateModified, folderName));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning MediaStore videos", e);
        }

        // If MediaStore returned nothing (e.g. fresh emulator or external dir), also check standard directories
        if (list.isEmpty()) {
            scanDirectoryFallback(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), list);
            scanDirectoryFallback(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), list);
            scanDirectoryFallback(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), list);
        }

        return list;
    }

    private void scanDirectoryFallback(File dir, List<VideoItem> list) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectoryFallback(f, list);
            } else if (isVideoFile(f.getName())) {
                String folderName = dir.getName();
                Uri uri = Uri.fromFile(f);
                list.add(new VideoItem(f.hashCode(), f.getName(), f.getAbsolutePath(), uri,
                        0, f.length(), f.lastModified() / 1000, folderName));
            }
        }
    }

    private boolean isVideoFile(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm")
                || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".flv")
                || lower.endsWith(".ts") || lower.endsWith(".m4v") || lower.endsWith(".3gp");
    }

    private String extractFolderNameFromPath(String path) {
        if (path == null) return "Видео";
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null) {
                return parent.getName();
            }
        } catch (Exception ignored) {}
        return "Видео";
    }

    private void buildFolderList() {
        Map<String, Integer> folderCounts = new HashMap<>();
        for (VideoItem v : allVideos) {
            String folder = v.getFolderName();
            folderCounts.put(folder, folderCounts.getOrDefault(folder, 0) + 1);
        }

        foldersList.clear();
        foldersList.add(new VideoFolder("Все", allVideos.size()));

        List<String> sortedNames = new ArrayList<>(folderCounts.keySet());
        Collections.sort(sortedNames);
        for (String name : sortedNames) {
            foldersList.add(new VideoFolder(name, folderCounts.get(name)));
        }

        videoFoldersAdapter.notifyDataSetChanged();
    }

    private void filterAndDisplayVideos() {
        List<VideoItem> filtered = new ArrayList<>();

        for (VideoItem v : allVideos) {
            boolean matchesFolder = "Все".equalsIgnoreCase(selectedFolder)
                    || selectedFolder.equalsIgnoreCase(v.getFolderName());
            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || (v.getTitle() != null && v.getTitle().toLowerCase(Locale.ROOT).contains(currentSearchQuery.toLowerCase(Locale.ROOT)));

            if (matchesFolder && matchesSearch) {
                filtered.add(v);
            }
        }

        sortVideos(filtered);

        displayedVideos.clear();
        displayedVideos.addAll(filtered);
        videoFilesAdapter.notifyDataSetChanged();

        updateCountersAndEmptyState();
    }

    private void sortVideos(List<VideoItem> list) {
        switch (currentSortOrder) {
            case DATE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.getDateModified(), a.getDateModified()));
                break;
            case DATE_ASC:
                Collections.sort(list, (a, b) -> Long.compare(a.getDateModified(), b.getDateModified()));
                break;
            case NAME_ASC:
                Collections.sort(list, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                        a.getTitle() != null ? a.getTitle() : "",
                        b.getTitle() != null ? b.getTitle() : ""));
                break;
            case SIZE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.getSizeBytes(), a.getSizeBytes()));
                break;
        }
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
        if (currentSortOrder == SortOrder.DATE_ASC) checkedItem = 1;
        else if (currentSortOrder == SortOrder.NAME_ASC) checkedItem = 2;
        else if (currentSortOrder == SortOrder.SIZE_DESC) checkedItem = 3;

        new AlertDialog.Builder(this)
                .setTitle("Сортировка видео")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0: currentSortOrder = SortOrder.DATE_DESC; break;
                        case 1: currentSortOrder = SortOrder.DATE_ASC; break;
                        case 2: currentSortOrder = SortOrder.NAME_ASC; break;
                        case 3: currentSortOrder = SortOrder.SIZE_DESC; break;
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

        // Pass current playlist paths & titles for next/prev navigation
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
