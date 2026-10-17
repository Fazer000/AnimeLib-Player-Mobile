package com.example.animelib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.animelib.util.DialogUtils;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.animelib.adapters.PlaylistsAdapter;
import com.example.animelib.adapters.VideoFilesAdapter;
import com.example.animelib.adapters.VideoFoldersAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.settings.CreateIssueBottomSheet;
import com.example.animelib.settings.FaqBottomSheet;
import com.example.animelib.settings.ResizeModeBottomSheet;
import com.example.animelib.settings.SkipDurationBottomSheet;
import com.example.animelib.settings.SubtitlesBottomSheet;
import com.example.animelib.settings.SurroundSoundBottomSheet;
import com.example.animelib.settings.ThemeSelectionBottomSheet;
import com.example.animelib.settings.VideoFiltersBottomSheet;
import com.example.animelib.UpdateActivity;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.example.animelib.domain.VideoListFilter;
import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;
import com.example.animelib.repository.LocalVideoRepository;
import com.example.animelib.repository.VideoRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private ImageButton btnSort;
    private ImageButton btnCreatePlaylist;
    private ImageButton btnSearchToggle;
    private ImageButton btnCloseSearch;
    private CardView layoutExpandedSearch, cardFolderCarousel;
    private View tabExplorer, tabPlaylists, tabSettings, vTabIndicator, layoutControlButtonsContainer;
    private TextView tvTabExplorerText, tvTabPlaylistsText, tvTabSettingsText;
    private ImageView ivTabExplorerIcon, ivTabPlaylistsIcon, ivTabSettingsIcon;
    private View layoutSettingsContainer;
    private MaterialSwitch switchTestMode, switchSurroundSound, switchSubtitles, switchAmbientLight, switchAutoPlay;
    private LinearLayout settingThemeCard, settingUpdateCard, settingResizeModeCard, settingSubtitlesCard, settingAmbientLightCard, settingAutoPlayCard, settingSurroundSoundCard, settingVideoFiltersCard, settingSkipDurationCard, settingTestModeCard, settingFaqCard, settingBugReportCard;
    private TextView tvCurrentTheme, tvCurrentResizeMode, tvCurrentSubtitleTag, tvCurrentSurroundSound, tvCurrentVideoFilters, tvCurrentSkipDuration;
    private boolean isPlaylistsTabSelected = false;
    private CardView layoutPlaylistCreationBar;
    private TextView tvPlaylistSelectionCount;
    private MaterialButton btnCancelPlaylist, btnDonePlaylist;
    private boolean isPlaylistCreationMode = false;
    private EditText etSearchVideos;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            int imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            boolean isImeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            View bottomContainer = findViewById(R.id.layoutBottomFloatingContainer);
            if (isImeVisible && layoutExpandedSearch != null && layoutExpandedSearch.getVisibility() == View.VISIBLE) {
                float density = getResources().getDisplayMetrics().density;
                if (bottomContainer != null) {
                    bottomContainer.setTranslationY(-imeHeight + (16f * density));
                }
            } else {
                if (bottomContainer != null) {
                    bottomContainer.setTranslationY(0f);
                }
            }
            return windowInsets;
        });

        selectExplorerTab();
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
        btnSort = findViewById(R.id.btnSort);
        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);
        btnSearchToggle = findViewById(R.id.btnSearchToggle);
        layoutExpandedSearch = findViewById(R.id.layoutExpandedSearch);
        cardFolderCarousel = findViewById(R.id.cardFolderCarousel);
        layoutControlButtonsContainer = findViewById(R.id.layoutControlButtonsContainer);
        tabExplorer = findViewById(R.id.tabExplorer);
        tabPlaylists = findViewById(R.id.tabPlaylists);
        tabSettings = findViewById(R.id.tabSettings);
        tvTabExplorerText = findViewById(R.id.tvTabExplorerText);
        tvTabPlaylistsText = findViewById(R.id.tvTabPlaylistsText);
        tvTabSettingsText = findViewById(R.id.tvTabSettingsText);
        ivTabExplorerIcon = findViewById(R.id.ivTabExplorerIcon);
        ivTabPlaylistsIcon = findViewById(R.id.ivTabPlaylistsIcon);
        ivTabSettingsIcon = findViewById(R.id.ivTabSettingsIcon);
        vTabIndicator = findViewById(R.id.vTabIndicator);
        if (vTabIndicator != null && tabExplorer != null) {
            vTabIndicator.post(() -> {
                ViewGroup.LayoutParams params = vTabIndicator.getLayoutParams();
                params.width = tabExplorer.getWidth();
                vTabIndicator.setLayoutParams(params);
            });
        }
        layoutSettingsContainer = findViewById(R.id.layoutSettingsContainer);
        switchTestMode = findViewById(R.id.switchTestMode);
        switchSurroundSound = findViewById(R.id.switchSurroundSound);
        settingThemeCard = findViewById(R.id.settingThemeCard);
        settingUpdateCard = findViewById(R.id.settingUpdateCard);
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);

        layoutPlaylistCreationBar = findViewById(R.id.layoutPlaylistCreationBar);
        tvPlaylistSelectionCount = findViewById(R.id.tvPlaylistSelectionCount);
        btnCancelPlaylist = findViewById(R.id.btnCancelPlaylist);
        btnDonePlaylist = findViewById(R.id.btnDonePlaylist);
        etSearchVideos = findViewById(R.id.etSearchVideos);

        btnCloseSearch = findViewById(R.id.btnCloseSearch);
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

        if (tabExplorer != null && tabPlaylists != null && tabSettings != null) {
            tabExplorer.setOnClickListener(v -> selectExplorerTab());
            tabPlaylists.setOnClickListener(v -> selectPlaylistsTab());
            tabSettings.setOnClickListener(v -> selectSettingsTab());
        }

        initSettingsViews();
    }

    private void initSettingsViews() {
        layoutSettingsContainer = findViewById(R.id.layoutSettingsContainer);

        // Group 1: Resize Mode
        settingResizeModeCard = findViewById(R.id.settingResizeModeCard);
        tvCurrentResizeMode = findViewById(R.id.tvCurrentResizeMode);
        if (settingResizeModeCard != null) {
            settingResizeModeCard.setOnClickListener(v -> {
                ResizeModeBottomSheet dialog = new ResizeModeBottomSheet(this, databaseManager.loadResizeModeSetting(), mode -> {
                    databaseManager.saveResizeModeSetting(mode);
                    if (tvCurrentResizeMode != null) {
                        tvCurrentResizeMode.setText(getResizeModeName(mode));
                    }
                });
                dialog.show();
            });
        }

        // Group 2: Subtitles
        settingSubtitlesCard = findViewById(R.id.settingSubtitlesCard);
        tvCurrentSubtitleTag = findViewById(R.id.tvCurrentSubtitleTag);
        switchSubtitles = findViewById(R.id.switchSubtitles);
        if (settingSubtitlesCard != null) {
            settingSubtitlesCard.setOnClickListener(v -> {
                SubtitlesBottomSheet dialog = new SubtitlesBottomSheet(
                        this,
                        databaseManager.loadSubtitlesEnabledSetting(),
                        databaseManager.loadSubtitleFormatSetting(),
                        new ArrayList<>(),
                        enabled -> {
                            databaseManager.saveSubtitlesEnabledSetting(enabled);
                            if (switchSubtitles != null) switchSubtitles.setChecked(enabled);
                        },
                        format -> {
                            databaseManager.saveSubtitleFormatSetting(format);
                            if (tvCurrentSubtitleTag != null) tvCurrentSubtitleTag.setText(format.toUpperCase(Locale.US));
                        }
                );
                dialog.setStyleSettings(
                        databaseManager.loadSubtitleTextSizeSetting(),
                        databaseManager.loadSubtitleTextColorSetting(),
                        databaseManager.loadSubtitleBackgroundColorSetting(),
                        databaseManager.loadSubtitleEdgeTypeSetting(),
                        databaseManager.loadSubtitleEdgeColorSetting(),
                        (size, col, bg, edge, edgeCol) -> {
                            databaseManager.saveSubtitleStyleSettings(size, col, bg, edge, edgeCol);
                        }
                );
                dialog.show();
            });
        }
        if (switchSubtitles != null) {
            switchSubtitles.setChecked(databaseManager.loadSubtitlesEnabledSetting());
            switchSubtitles.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveSubtitlesEnabledSetting(isChecked);
            });
        }

        // Ambient Light
        settingAmbientLightCard = findViewById(R.id.settingAmbientLightCard);
        switchAmbientLight = findViewById(R.id.switchAmbientLight);
        if (switchAmbientLight != null) {
            switchAmbientLight.setChecked(databaseManager.loadAmbientLightSetting());
            switchAmbientLight.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveAmbientLightSetting(isChecked);
            });
        }

        // AutoPlay
        settingAutoPlayCard = findViewById(R.id.settingAutoPlayCard);
        switchAutoPlay = findViewById(R.id.switchAutoPlay);
        if (switchAutoPlay != null) {
            switchAutoPlay.setChecked(databaseManager.loadAutoPlaySetting());
            switchAutoPlay.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveAutoPlaySetting(isChecked);
            });
        }

        // Surround Sound
        settingSurroundSoundCard = findViewById(R.id.settingSurroundSoundCard);
        tvCurrentSurroundSound = findViewById(R.id.tvCurrentSurroundSound);
        switchSurroundSound = findViewById(R.id.switchSurroundSound);
        if (settingSurroundSoundCard != null) {
            settingSurroundSoundCard.setOnClickListener(v -> {
                SurroundSoundBottomSheet dialog = new SurroundSoundBottomSheet(
                        this,
                        databaseManager.loadSurroundSoundSetting(),
                        databaseManager.loadSurround3DMode(),
                        databaseManager.loadSurroundSpatialWidth(),
                        databaseManager.loadSurroundDialogueBoost(),
                        databaseManager.loadSurroundBassBoost(),
                        databaseManager.loadSurroundTrebleBoost(),
                        (enabled, mode, spatial, dialogue, bass, treble) -> {
                            databaseManager.saveSurroundSoundSetting(enabled);
                            databaseManager.saveSurround3DSettings(mode, spatial, dialogue, bass, treble);
                            if (switchSurroundSound != null) switchSurroundSound.setChecked(enabled);
                            if (tvCurrentSurroundSound != null) tvCurrentSurroundSound.setText(getSurroundModeName(mode));
                        }
                );
                dialog.show();
            });
        }
        if (switchSurroundSound != null) {
            switchSurroundSound.setChecked(databaseManager.loadSurroundSoundSetting());
            switchSurroundSound.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveSurroundSoundSetting(isChecked);
            });
        }

        // Video Filters
        settingVideoFiltersCard = findViewById(R.id.settingVideoFiltersCard);
        tvCurrentVideoFilters = findViewById(R.id.tvCurrentVideoFilters);
        if (settingVideoFiltersCard != null) {
            settingVideoFiltersCard.setOnClickListener(v -> {
                float[] filters = databaseManager.loadVideoFilters();
                VideoFiltersBottomSheet dialog = new VideoFiltersBottomSheet(
                        this,
                        filters[0], filters[1], filters[2], filters[3], filters[4],
                        (b, c, s, g, h) -> {
                            databaseManager.saveVideoFilters(b, c, s, g, h);
                            if (tvCurrentVideoFilters != null) {
                                boolean isDef = (b == 0f && c == 100f && s == 100f && g == 1.0f && h == 0f);
                                tvCurrentVideoFilters.setText(isDef ? "По умолчанию" : "Настроено");
                            }
                        }
                );
                dialog.show();
            });
        }

        // Skip Duration
        settingSkipDurationCard = findViewById(R.id.settingSkipDurationCard);
        tvCurrentSkipDuration = findViewById(R.id.tvCurrentSkipDuration);
        if (settingSkipDurationCard != null) {
            settingSkipDurationCard.setOnClickListener(v -> {
                SkipDurationBottomSheet dialog = new SkipDurationBottomSheet(
                        this,
                        databaseManager.loadLongSkipDurationSetting(),
                        duration -> {
                            databaseManager.saveLongSkipDurationSetting(duration);
                            if (tvCurrentSkipDuration != null) {
                                int min = duration / 60;
                                int sec = duration % 60;
                                tvCurrentSkipDuration.setText(String.format(Locale.US, "%d сек (%d:%02d)", duration, min, sec));
                            }
                        }
                );
                dialog.show();
            });
        }

        // Test Mode
        settingTestModeCard = findViewById(R.id.settingTestModeCard);
        switchTestMode = findViewById(R.id.switchTestMode);
        if (switchTestMode != null) {
            switchTestMode.setChecked(databaseManager.loadTestModeSetting());
            switchTestMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                databaseManager.saveTestModeSetting(isChecked);
                loadVideosFromStorage();
            });
        }

        // Group 3: Theme
        settingThemeCard = findViewById(R.id.settingThemeCard);
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        if (settingThemeCard != null) {
            settingThemeCard.setOnClickListener(v -> {
                ThemeSelectionBottomSheet dialog = new ThemeSelectionBottomSheet(
                        this,
                        databaseManager.loadThemeSetting(),
                        theme -> {
                            databaseManager.saveThemeSetting(theme);
                            ThemeUtils.applyTheme(theme);
                            updateThemeText();
                            recreate();
                        }
                );
                dialog.show();
            });
        }

        // FAQ & Help
        settingFaqCard = findViewById(R.id.settingFaqCard);
        if (settingFaqCard != null) {
            settingFaqCard.setOnClickListener(v -> {
                FaqBottomSheet dialog = new FaqBottomSheet(this);
                dialog.show();
            });
        }

        // Bug Report
        settingBugReportCard = findViewById(R.id.settingBugReportCard);
        if (settingBugReportCard != null) {
            settingBugReportCard.setOnClickListener(v -> {
                CreateIssueBottomSheet dialog = new CreateIssueBottomSheet(this);
                dialog.show();
            });
        }

        // Check for Updates
        settingUpdateCard = findViewById(R.id.settingUpdateCard);
        if (settingUpdateCard != null) {
            settingUpdateCard.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, UpdateActivity.class));
                } catch (Exception e) {
                    CustomToast.showError(this, "Не удалось открыть страницу обновлений");
                }
            });
        }

        refreshSettingsUi();
    }

    private void refreshSettingsUi() {
        if (tvCurrentResizeMode != null) {
            int mode = databaseManager.loadResizeModeSetting();
            tvCurrentResizeMode.setText(getResizeModeName(mode));
        }
        if (switchSubtitles != null) {
            switchSubtitles.setChecked(databaseManager.loadSubtitlesEnabledSetting());
        }
        if (tvCurrentSubtitleTag != null) {
            String format = databaseManager.loadSubtitleFormatSetting();
            tvCurrentSubtitleTag.setText(format.toUpperCase(Locale.US));
        }
        if (switchAmbientLight != null) {
            switchAmbientLight.setChecked(databaseManager.loadAmbientLightSetting());
        }
        if (switchAutoPlay != null) {
            switchAutoPlay.setChecked(databaseManager.loadAutoPlaySetting());
        }
        if (switchSurroundSound != null) {
            switchSurroundSound.setChecked(databaseManager.loadSurroundSoundSetting());
        }
        if (tvCurrentSurroundSound != null) {
            tvCurrentSurroundSound.setText(getSurroundModeName(databaseManager.loadSurround3DMode()));
        }
        if (tvCurrentVideoFilters != null) {
            float[] f = databaseManager.loadVideoFilters();
            boolean isDefault = (f[0] == 0f && f[1] == 100f && f[2] == 100f && f[3] == 1.0f && f[4] == 0f);
            tvCurrentVideoFilters.setText(isDefault ? "По умолчанию" : "Настроено");
        }
        if (tvCurrentSkipDuration != null) {
            int duration = databaseManager.loadLongSkipDurationSetting();
            int min = duration / 60;
            int sec = duration % 60;
            tvCurrentSkipDuration.setText(String.format(Locale.US, "%d сек (%d:%02d)", duration, min, sec));
        }
        if (switchTestMode != null) {
            switchTestMode.setChecked(databaseManager.loadTestModeSetting());
        }
        updateThemeText();
    }

    private String getResizeModeName(int mode) {
        switch (mode) {
            case 3: return "По размеру экрана"; // ZOOM
            case 4: return "Растянуть"; // FILL
            default: return "Вписать (Оригинал)"; // FIT
        }
    }

    private String getSurroundModeName(int mode) {
        switch (mode) {
            case 0: return "🎬 Кинотеатр 3D";
            case 1: return "🎧 Концерт 3D";
            case 2: return "🗣 Голос 3D";
            case 3: return "🔊 Классический 5.1";
            case 4: return "⚡ Экстрим 3D";
            default: return "🎬 Кинотеатр 3D";
        }
    }

    private void updateThemeText() {
        if (tvCurrentTheme != null) {
            int theme = ThemeUtils.getSavedThemePreference(this);
            tvCurrentTheme.setText(ThemeUtils.getThemeName(theme));
        }
    }

    private void selectExplorerTab() {
        isPlaylistsTabSelected = false;
        if (cardFolderCarousel != null) cardFolderCarousel.setVisibility(View.VISIBLE);
        if (layoutControlButtonsContainer != null) layoutControlButtonsContainer.setVisibility(View.VISIBLE);
        rvFolders.setVisibility(View.VISIBLE);
        rvVideos.setVisibility(View.VISIBLE);
        if (layoutSettingsContainer != null) layoutSettingsContainer.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (rvVideos.getAdapter() != videoFilesAdapter) {
            rvVideos.setAdapter(videoFilesAdapter);
        }
        filterAndDisplayVideos();
        updateTabSelection(0);
    }

    private void selectPlaylistsTab() {
        isPlaylistsTabSelected = true;
        if (cardFolderCarousel != null) cardFolderCarousel.setVisibility(View.GONE);
        if (layoutControlButtonsContainer != null) layoutControlButtonsContainer.setVisibility(View.GONE);
        if (layoutExpandedSearch != null) layoutExpandedSearch.setVisibility(View.GONE);
        if (btnSearchToggle != null) btnSearchToggle.setVisibility(View.VISIBLE);
        rvFolders.setVisibility(View.GONE);
        rvVideos.setVisibility(View.VISIBLE);
        if (layoutSettingsContainer != null) layoutSettingsContainer.setVisibility(View.GONE);
        loadAndDisplayPlaylists();
        updateTabSelection(1);
    }

    private void selectSettingsTab() {
        isPlaylistsTabSelected = false;
        if (cardFolderCarousel != null) cardFolderCarousel.setVisibility(View.GONE);
        if (layoutControlButtonsContainer != null) layoutControlButtonsContainer.setVisibility(View.GONE);
        if (layoutExpandedSearch != null) layoutExpandedSearch.setVisibility(View.GONE);
        if (btnSearchToggle != null) btnSearchToggle.setVisibility(View.VISIBLE);
        rvFolders.setVisibility(View.GONE);
        rvVideos.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (layoutSettingsContainer != null) {
            layoutSettingsContainer.setVisibility(View.VISIBLE);
            refreshSettingsUi();
        }
        updateTabSelection(2);
    }

    private void updateTabSelection(int targetIndex) {
        View[] tabs = {tabExplorer, tabPlaylists, tabSettings};
        TextView[] texts = {tvTabExplorerText, tvTabPlaylistsText, tvTabSettingsText};
        ImageView[] icons = {ivTabExplorerIcon, ivTabPlaylistsIcon, ivTabSettingsIcon};

        for (int i = 0; i < 3; i++) {
            if (tabs[i] != null && tabs[i].getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabs[i].getLayoutParams();
                lp.weight = (i == targetIndex) ? 1.5f : 0.75f;
                tabs[i].setLayoutParams(lp);
            }
            if (i == targetIndex) {
                if (texts[i] != null) {
                    texts[i].setVisibility(View.VISIBLE);
                    texts[i].setTextColor(ContextCompat.getColor(this, R.color.white_color));
                }
                if (icons[i] != null) {
                    icons[i].setColorFilter(ContextCompat.getColor(this, R.color.white_color));
                }
            } else {
                if (texts[i] != null) {
                    texts[i].setVisibility(View.GONE);
                }
                if (icons[i] != null) {
                    icons[i].setColorFilter(ContextCompat.getColor(this, R.color.player_item_inactive_text));
                }
            }
        }

        if (vTabIndicator != null && tabs[targetIndex] != null) {
            View target = tabs[targetIndex];
            vTabIndicator.post(() -> {
                if (vTabIndicator != null && target != null) {
                    vTabIndicator.animate().translationX(target.getLeft()).setDuration(200).start();
                }
            });
        }
    }

    private void setupRecyclerViews() {
        rvFolders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoFoldersAdapter = new VideoFoldersAdapter(foldersList, folder -> {
            selectedFolder = folder.getName();
            filterAndDisplayVideos();
        });
        rvFolders.setAdapter(videoFoldersAdapter);

        rvVideos.setLayoutManager(new LinearLayoutManager(this));
        videoFilesAdapter = new VideoFilesAdapter(displayedVideos, (video, position) -> {
            if (isPlaylistCreationMode) {
                videoFilesAdapter.toggleSelection(video);
                updatePlaylistSelectionBar();
            } else {
                openVideoInPlayer(video);
            }
        });
        rvVideos.setAdapter(videoFilesAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(rvVideos, (v, windowInsets) -> {
            int statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int defaultTopPadding = (int) (16 * getResources().getDisplayMetrics().density);
            int bottomPadding = (int) (260 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), statusBarHeight + defaultTopPadding, v.getPaddingRight(), bottomPadding);
            return windowInsets;
        });

        if (layoutSettingsContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutSettingsContainer, (v, windowInsets) -> {
                int statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                int defaultTopPadding = (int) (16 * getResources().getDisplayMetrics().density);
                int bottomPadding = (int) (260 * getResources().getDisplayMetrics().density);
                v.setPadding(v.getPaddingLeft(), statusBarHeight + defaultTopPadding, v.getPaddingRight(), bottomPadding);
                return windowInsets;
            });
        }
    }

    private void setupSearchAndListeners() {
        btnSort.setOnClickListener(v -> showSortDialog());
        if (btnCreatePlaylist != null) {
            btnCreatePlaylist.setOnClickListener(v -> startPlaylistCreationMode());
        }
        if (btnSearchToggle != null) {
            btnSearchToggle.setOnClickListener(v -> {
                btnSearchToggle.setVisibility(View.GONE);
                layoutExpandedSearch.setVisibility(View.VISIBLE);
                etSearchVideos.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearchVideos, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> {
                etSearchVideos.setText("");
                layoutExpandedSearch.setVisibility(View.GONE);
                btnSearchToggle.setVisibility(View.VISIBLE);
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearchVideos.getWindowToken(), 0);
                }
            });
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

        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> {
                etSearchVideos.setText("");
                layoutExpandedSearch.setVisibility(View.GONE);
                btnSearchToggle.setVisibility(View.VISIBLE);
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearchVideos.getWindowToken(), 0);
                }
            });
        }

        btnEmptyAction.setOnClickListener(v -> {
            if (!hasStoragePermission()) {
                requestStoragePermission();
            } else {
                loadVideosFromStorage();
            }
        });
    }

    private void showPlaylistMenuDialog() {
        String[] options = new String[]{
                "Создать новый плейлист",
                "Мои плейлисты"
        };
        DialogUtils.showListDialog(this, "Управление плейлистами", options, which -> {
            if (which == 0) {
                startPlaylistCreationMode();
            } else if (which == 1) {
                showSavedPlaylistsDialog();
            }
        });
    }

    private void startPlaylistCreationMode() {
        isPlaylistCreationMode = true;
        if (layoutPlaylistCreationBar != null) {
            layoutPlaylistCreationBar.setVisibility(View.VISIBLE);
        }
        if (btnSort != null) btnSort.setVisibility(View.GONE);
        if (btnCreatePlaylist != null) btnCreatePlaylist.setVisibility(View.GONE);
        if (videoFilesAdapter != null) {
            videoFilesAdapter.setSelectionMode(true);
        }
        updatePlaylistSelectionBar();
        com.example.animelib.util.CustomToast.showInfo(this, "Выберите видео по порядку для создания плейлиста");
    }

    private void exitPlaylistCreationMode() {
        isPlaylistCreationMode = false;
        if (layoutPlaylistCreationBar != null) {
            layoutPlaylistCreationBar.setVisibility(View.GONE);
        }
        if (btnSort != null) btnSort.setVisibility(View.VISIBLE);
        if (btnCreatePlaylist != null) btnCreatePlaylist.setVisibility(View.VISIBLE);
        if (videoFilesAdapter != null) {
            videoFilesAdapter.setSelectionMode(false);
        }
    }

    private void loadAndDisplayPlaylists() {
        List<com.example.animelib.data.entity.PlaylistEntity> playlists = databaseManager.getAllPlaylists();
        if (playlists.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvVideos.setVisibility(View.GONE);
            tvEmptyTitle.setText("Нет сохраненных плейлистов");
            tvEmptySubtitle.setText("Создайте свой первый плейлист, нажав кнопку плейлиста внизу");
            btnEmptyAction.setText("Создать плейлист");
            btnEmptyAction.setOnClickListener(v -> startPlaylistCreationMode());
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        rvVideos.setVisibility(View.VISIBLE);

        com.example.animelib.adapters.PlaylistsAdapter adapter = new com.example.animelib.adapters.PlaylistsAdapter(playlists, new com.example.animelib.adapters.PlaylistsAdapter.OnPlaylistListener() {
            @Override
            public void onPlaylistClick(com.example.animelib.data.entity.PlaylistEntity playlist) {
                loadAndPlaySavedPlaylist(playlist);
            }

            @Override
            public void onPlaylistDelete(com.example.animelib.data.entity.PlaylistEntity playlist) {
                DialogUtils.showCustomDialog(MainActivity.this,
                        "Удалить плейлист",
                        "Удалить плейлист «" + playlist.getName() + "»?",
                        "Удалить",
                        "Отмена",
                        new DialogUtils.DialogListener() {
                            @Override
                            public void onPositiveClick() {
                                databaseManager.deletePlaylist(playlist.getId());
                                com.example.animelib.util.CustomToast.showSuccess(MainActivity.this, "Плейлист удален");
                                loadAndDisplayPlaylists();
                            }

                            @Override
                            public void onNegativeClick() {
                                // Do nothing
                            }
                        });
            }
        });
        rvVideos.setAdapter(adapter);
    }

    private void updatePlaylistSelectionBar() {
        if (videoFilesAdapter == null || tvPlaylistSelectionCount == null) return;
        int count = videoFilesAdapter.getSelectedVideos().size();
        tvPlaylistSelectionCount.setText("Выбрано видео: " + count);
        if (btnDonePlaylist != null) {
            btnDonePlaylist.setEnabled(count > 0);
            btnDonePlaylist.setAlpha(count > 0 ? 1.0f : 0.5f);
        }
    }

    private void promptForPlaylistNameAndSave() {
        if (videoFilesAdapter == null) return;
        List<VideoItem> selected = videoFilesAdapter.getSelectedVideos();
        if (selected.isEmpty()) {
            com.example.animelib.util.CustomToast.showWarning(this, "Выберите хотя бы одно видео");
            return;
        }

        String defaultName = "Плейлист " + new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        String msg = "Укажите название для плейлиста (" + selected.size() + " видео по порядку):";

        DialogUtils.showInputDialog(this, "Создание плейлиста", msg, defaultName, "Название плейлиста", "Создать", "Отмена",
                new DialogUtils.InputDialogListener() {
                    @Override
                    public void onInputConfirmed(String input) {
                        String name = input.isEmpty() ? "Мой плейлист" : input;
                        final String playlistName = name;
                        final List<VideoItem> selectedCopy = new ArrayList<>(selected);

                        long playlistId = databaseManager.createPlaylist(playlistName, selectedCopy);
                        if (playlistId != -1) {
                            com.example.animelib.util.CustomToast.showSuccess(MainActivity.this, "Плейлист «" + playlistName + "» успешно создан!");
                            exitPlaylistCreationMode();

                            DialogUtils.showConfirmDialog(MainActivity.this,
                                    "Плейлист создан",
                                    "Воспроизвести созданный плейлист сейчас?",
                                    "Да",
                                    "Позже",
                                    false,
                                    new DialogUtils.DialogListener() {
                                        @Override
                                        public void onPositiveClick() {
                                            playCustomPlaylist(selectedCopy, playlistName);
                                        }

                                        @Override
                                        public void onNegativeClick() {}
                                    });
                        } else {
                            com.example.animelib.util.CustomToast.showWarning(MainActivity.this, "Не удалось создать плейлист");
                        }
                    }

                    @Override
                    public void onCancelled() {}
                });
    }

    private void showSavedPlaylistsDialog() {
        List<com.example.animelib.data.entity.PlaylistEntity> playlists = databaseManager.getAllPlaylists();
        if (playlists.isEmpty()) {
            com.example.animelib.util.CustomToast.showInfo(this, "У вас пока нет сохраненных плейлистов");
            return;
        }

        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).getName() + " (" + databaseManager.getVideosForPlaylist(playlists.get(i).getId()).size() + " видео)";
        }

        DialogUtils.showListWithNeutralDialog(this, "Мои плейлисты", names, "Удалить плейлист", new DialogUtils.NeutralListDialogListener() {
            @Override
            public void onItemSelected(int which) {
                com.example.animelib.data.entity.PlaylistEntity selectedPlaylist = playlists.get(which);
                loadAndPlaySavedPlaylist(selectedPlaylist);
            }

            @Override
            public void onNeutralClicked() {
                showDeletePlaylistChooser(playlists);
            }
        });
    }

    private void showDeletePlaylistChooser(List<com.example.animelib.data.entity.PlaylistEntity> playlists) {
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).getName();
        }
        DialogUtils.showListDialog(this, "Выберите плейлист для удаления", names, which -> {
            com.example.animelib.data.entity.PlaylistEntity toDelete = playlists.get(which);
            databaseManager.deletePlaylist(toDelete.getId());
            com.example.animelib.util.CustomToast.showSuccess(MainActivity.this, "Плейлист удален");
        });
    }

    private void loadAndPlaySavedPlaylist(com.example.animelib.data.entity.PlaylistEntity playlistEntity) {
        List<com.example.animelib.data.entity.PlaylistVideoEntity> videoEntities = databaseManager.getVideosForPlaylist(playlistEntity.getId());
        if (videoEntities.isEmpty()) {
            com.example.animelib.util.CustomToast.showWarning(this, "В этом плейлисте нет видео");
            return;
        }

        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        for (com.example.animelib.data.entity.PlaylistVideoEntity ve : videoEntities) {
            paths.add(ve.getVideoPath());
            titles.add(ve.getVideoTitle());
        }

        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("EXTRA_LOCAL_FILE_PATH", paths.get(0));
        intent.putExtra("EXTRA_VIDEO_TITLE", titles.get(0));
        intent.putExtra("EXTRA_FOLDER_NAME", playlistEntity.getName());
        intent.putExtra("EXTRA_PLAYLIST_NAME", playlistEntity.getName());
        intent.putExtra("EXTRA_IS_PLAYLIST", true);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_PATHS", paths);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_TITLES", titles);
        intent.putExtra("EXTRA_PLAYLIST_INDEX", 0);
        startActivity(intent);
    }

    private void playCustomPlaylist(List<VideoItem> selected, String playlistName) {
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        for (VideoItem item : selected) {
            String path = item.getPath() != null && !item.getPath().isEmpty() ? item.getPath() : item.getUriString();
            paths.add(path);
            titles.add(item.getTitle());
        }

        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("EXTRA_LOCAL_FILE_PATH", paths.get(0));
        intent.putExtra("EXTRA_VIDEO_TITLE", titles.get(0));
        intent.putExtra("EXTRA_FOLDER_NAME", playlistName);
        intent.putExtra("EXTRA_PLAYLIST_NAME", playlistName);
        intent.putExtra("EXTRA_IS_PLAYLIST", true);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_PATHS", paths);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_TITLES", titles);
        intent.putExtra("EXTRA_PLAYLIST_INDEX", 0);
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
                if (!isPlaylistsTabSelected) {
                    filterAndDisplayVideos();
                } else {
                    loadAndDisplayPlaylists();
                }
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
        if (isPlaylistsTabSelected) return;
        List<VideoItem> filtered = videoListFilter.filterAndSort(
                allVideos,
                selectedFolder,
                currentSearchQuery,
                currentSortOrder
        );

        displayedVideos.clear();
        displayedVideos.addAll(filtered);
        if (rvVideos.getAdapter() != videoFilesAdapter) {
            rvVideos.setAdapter(videoFilesAdapter);
        }
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
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvVideos.setVisibility(View.VISIBLE);
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

        DialogUtils.showSingleChoiceDialog(this, "Сортировка видео", options, checkedItem, which -> {
            switch (which) {
                case 0: currentSortOrder = VideoListFilter.SortOrder.DATE_DESC; break;
                case 1: currentSortOrder = VideoListFilter.SortOrder.DATE_ASC; break;
                case 2: currentSortOrder = VideoListFilter.SortOrder.NAME_ASC; break;
                case 3: currentSortOrder = VideoListFilter.SortOrder.SIZE_DESC; break;
            }
            filterAndDisplayVideos();
        });
    }

    private void showTestModeMenu() {
        boolean isTestModeOn = databaseManager.loadTestModeSetting();
        String toggleText = isTestModeOn ? "Скрыть тестовые видео в списке" : "Показывать тестовые видео в списке";

        String[] options = new String[]{
                "Запустить тест плеера (демо-видео)",
                toggleText
        };

        DialogUtils.showListDialog(this, "Тест-режим плеера", options, which -> {
            if (which == 0) {
                launchTestPlayerWithDemoPlaylist();
            } else if (which == 1) {
                databaseManager.saveTestModeSetting(!isTestModeOn);
                loadVideosFromStorage();
                com.example.animelib.util.CustomToast.showInfo(MainActivity.this,
                        !isTestModeOn ? "Тестовые видео добавлены в список" : "Тестовые видео скрыты");
            }
        });
    }

    private void launchTestPlayerWithDemoPlaylist() {
        List<VideoItem> testItems = videoRepository.getTestVideos();
        if (testItems.isEmpty()) return;
        VideoItem first = testItems.get(0);
        openVideoInPlayer(first, testItems);
    }

    private void openVideoInPlayer(VideoItem video) {
        openVideoInPlayer(video, displayedVideos);
    }

    private void openVideoInPlayer(VideoItem video, List<VideoItem> playlist) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        String targetPath = video.getPath() != null && !video.getPath().isEmpty()
                ? video.getPath() : video.getUriString();

        intent.putExtra("EXTRA_LOCAL_FILE_PATH", targetPath);
        intent.putExtra("EXTRA_VIDEO_TITLE", video.getTitle());
        intent.putExtra("EXTRA_FOLDER_NAME", video.getFolderName());

        ArrayList<String> playlistPaths = new ArrayList<>();
        ArrayList<String> playlistTitles = new ArrayList<>();
        int currentIndex = 0;

        List<VideoItem> targetPlaylist = (playlist != null && !playlist.isEmpty()) ? playlist : displayedVideos;

        for (int i = 0; i < targetPlaylist.size(); i++) {
            VideoItem item = targetPlaylist.get(i);
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
