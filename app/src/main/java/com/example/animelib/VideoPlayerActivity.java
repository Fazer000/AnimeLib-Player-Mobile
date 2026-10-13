package com.example.animelib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.managers.SurroundSoundManager;
import com.example.animelib.managers.VideoFiltersManager;
import com.example.animelib.settings.ResizeModeBottomSheet;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.settings.SpeedBottomSheet;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.SurroundRenderersFactory;
import com.example.animelib.util.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "VideoPlayerActivity";

    // Playback and UI components
    private PlayerView playerView;
    private FrameLayout playerContainer;
    private ExoPlayer player;
    private DatabaseManager databaseManager;

    private TextView tvAnimeTitle;
    private ImageButton btnClose;
    private ImageButton btnSettings;
    private ImageButton btnPip;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnSkipForward;
    private ImageButton btnToggleFullscreen;
    private ImageButton btnLockScreen;
    private View loadingOverlay;
    private View bufferingIndicator;

    // Gesture and Plaque Overlays
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private LinearLayout skipIndicatorLeft;
    private TextView skipTextLeft;
    private LinearLayout skipIndicatorRight;
    private TextView skipTextRight;
    private LinearLayout gestureIndicatorPlaque;
    private ImageView gestureIndicatorIcon;
    private ProgressBar gestureIndicatorProgress;
    private TextView gestureIndicatorText;

    // Lock Overlay
    private FrameLayout layoutLockedOverlay;
    private LinearLayout lockedStatusBadge;
    private LinearLayout swipeUnlockPill;
    private boolean isScreenLocked = false;

    // Ambient & Filters & Sound
    private AmbientLightManager ambientLightManager;
    private SurroundSoundManager surroundSoundManager;
    private VideoFiltersManager videoFiltersManager;

    // Current State & Playlist
    private String currentVideoPath;
    private String currentVideoTitle;
    private String currentFolderName = "Видео";
    private List<String> playlistPaths = new ArrayList<>();
    private List<String> playlistTitles = new ArrayList<>();
    private int currentPlaylistIndex = 0;

    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private float currentPlaybackSpeed = 1.0f;
    private int skipDurationSeconds = 85;
    private boolean isUserSeeking = false;
    private long seekTargetPosition = 0;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            saveCurrentPosition();
            progressHandler.postDelayed(this, 2000);
        }
    };

    private AudioManager audioManager;
    private int maxVolume;

    // Touch gesture helpers
    private float touchStartX, touchStartY;
    private boolean isTouching = false;
    private boolean isHorizontalSeeking = false;
    private boolean isVerticalBrightness = false;
    private boolean isVerticalVolume = false;
    private boolean isHoldingSpeed = false;
    private long touchDownTime = 0;
    private final Handler holdSpeedHandler = new Handler(Looper.getMainLooper());
    private final Handler hidePlaqueHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        databaseManager = DatabaseManager.getInstance(this);
        ThemeUtils.applyTheme(databaseManager.loadThemeSetting());

        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnimeLIB_VideoPlayer);
        setContentView(R.layout.activity_video_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        skipDurationSeconds = databaseManager.loadLongSkipDurationSetting();

        initViews();
        parseIntentData(getIntent());
        setupExoPlayer();
        setupGestureDetector();
        setupControlsListeners();
        playVideoAtIndex(currentPlaylistIndex, true);
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        playerContainer = findViewById(R.id.playerContainer);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        bufferingIndicator = findViewById(R.id.playerBufferingIndicator);

        // Custom player controller views
        tvAnimeTitle = findViewById(R.id.animeTitle);
        btnClose = findViewById(R.id.ibClosePlayer);
        btnSettings = findViewById(R.id.settingsButton);
        btnPip = findViewById(R.id.pipButton);
        btnPlay = findViewById(R.id.btnPlayerPlay);
        btnPause = findViewById(R.id.btnPlayerPause);
        btnPrev = findViewById(R.id.prevEpisodeButton);
        btnNext = findViewById(R.id.nextEpisodeButton);
        btnSkipForward = findViewById(R.id.skipForwardButton);
        btnToggleFullscreen = findViewById(R.id.menuToggleFullscreen);
        btnLockScreen = findViewById(R.id.btnLockScreen);

        // Gesture Plaques
        seekPreviewText = findViewById(R.id.seekPreviewText);
        holdSpeedToast = findViewById(R.id.holdSpeedToast);
        skipIndicatorLeft = findViewById(R.id.skipIndicatorLeft);
        skipTextLeft = findViewById(R.id.skipTextLeft);
        skipIndicatorRight = findViewById(R.id.skipIndicatorRight);
        skipTextRight = findViewById(R.id.skipTextRight);
        gestureIndicatorPlaque = findViewById(R.id.gestureIndicatorPlaque);
        gestureIndicatorIcon = findViewById(R.id.gestureIndicatorIcon);
        gestureIndicatorProgress = findViewById(R.id.gestureIndicatorProgress);
        gestureIndicatorText = findViewById(R.id.gestureIndicatorText);

        // Lock Overlay
        layoutLockedOverlay = findViewById(R.id.layoutLockedOverlay);
        lockedStatusBadge = findViewById(R.id.lockedStatusBadge);
        swipeUnlockPill = findViewById(R.id.swipeUnlockPill);

        if (btnLockScreen != null) {
            btnLockScreen.setVisibility(View.VISIBLE);
        }

        // Initialize Managers
        surroundSoundManager = new SurroundSoundManager(this);
        surroundSoundManager.setEnabled(databaseManager.loadSurroundSoundSetting());

        videoFiltersManager = new VideoFiltersManager(this, playerView);
        float[] filters = databaseManager.loadVideoFilters();
        videoFiltersManager.setFilters(filters[0], filters[1], filters[2], filters[3], filters[4]);

        View ambientContainer = findViewById(R.id.ambientContainer);
        PlayerView ambientPlayerView = findViewById(R.id.ambientPlayerView);
        ImageView ambientImageView = findViewById(R.id.ambientImageView);
        com.example.animelib.ui.AmbientVignetteOverlayView ambientVignette = findViewById(R.id.ambientVignetteOverlay);

        if (ambientContainer != null && databaseManager.loadAmbientLightSetting()) {
            ambientLightManager = new AmbientLightManager(this, playerView, ambientContainer,
                    ambientPlayerView, ambientImageView, ambientVignette);
            ambientLightManager.setEnabled(true);
        }
    }

    private void parseIntentData(Intent intent) {
        if (intent == null) return;

        Uri dataUri = intent.getData();
        String localPath = intent.getStringExtra("EXTRA_LOCAL_FILE_PATH");
        if (localPath == null) {
            localPath = intent.getStringExtra("EXTRA_VIDEO_PATH");
        }

        if (localPath != null) {
            currentVideoPath = localPath;
        } else if (dataUri != null) {
            currentVideoPath = dataUri.toString();
        }

        currentVideoTitle = intent.getStringExtra("EXTRA_VIDEO_TITLE");
        if (currentVideoTitle == null && currentVideoPath != null) {
            currentVideoTitle = new File(currentVideoPath).getName();
        }

        currentFolderName = intent.getStringExtra("EXTRA_FOLDER_NAME");
        if (currentFolderName == null) currentFolderName = "Видео";

        ArrayList<String> paths = intent.getStringArrayListExtra("EXTRA_PLAYLIST_PATHS");
        ArrayList<String> titles = intent.getStringArrayListExtra("EXTRA_PLAYLIST_TITLES");
        int index = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", 0);

        if (paths != null && !paths.isEmpty()) {
            playlistPaths = paths;
            playlistTitles = titles != null ? titles : new ArrayList<>();
            currentPlaylistIndex = Math.max(0, Math.min(index, playlistPaths.size() - 1));
        } else if (currentVideoPath != null) {
            playlistPaths.clear();
            playlistPaths.add(currentVideoPath);
            playlistTitles.clear();
            playlistTitles.add(currentVideoTitle);
            currentPlaylistIndex = 0;
        }
    }

    private void setupExoPlayer() {
        SurroundRenderersFactory renderersFactory = new SurroundRenderersFactory(
                this,
                surroundSoundManager.getSurroundAudioProcessor()
        );

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 50000, 1500, 2500)
                .setBackBuffer(10000, true)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        player = new ExoPlayer.Builder(this, renderersFactory)
                .setLoadControl(loadControl)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build();

        playerView.setPlayer(player);
        playerView.setResizeMode(currentResizeMode);

        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        surroundSoundManager.attachPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    if (bufferingIndicator != null) bufferingIndicator.setVisibility(View.VISIBLE);
                } else {
                    if (bufferingIndicator != null) bufferingIndicator.setVisibility(View.GONE);
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                }

                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButtons(isPlaying);
                if (isPlaying) {
                    progressHandler.post(progressRunnable);
                } else {
                    saveCurrentPosition();
                    progressHandler.removeCallbacks(progressRunnable);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                CustomToast.showWarning(VideoPlayerActivity.this, "Ошибка воспроизведения: " + error.getMessage());
            }
        });
    }

    private void playVideoAtIndex(int index, boolean shouldResume) {
        if (playlistPaths.isEmpty() || index < 0 || index >= playlistPaths.size()) return;

        saveCurrentPosition();

        currentPlaylistIndex = index;
        currentVideoPath = playlistPaths.get(currentPlaylistIndex);
        currentVideoTitle = playlistTitles.size() > currentPlaylistIndex
                ? playlistTitles.get(currentPlaylistIndex)
                : new File(currentVideoPath).getName();

        if (tvAnimeTitle != null) {
            tvAnimeTitle.setText(currentVideoTitle);
        }

        updatePlaylistButtons();

        Uri uri;
        if (currentVideoPath.startsWith("content://") || currentVideoPath.startsWith("http://") || currentVideoPath.startsWith("https://")) {
            uri = Uri.parse(currentVideoPath);
        } else {
            uri = Uri.fromFile(new File(currentVideoPath));
        }

        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player, mediaItem, uri.toString());
        }

        if (shouldResume) {
            checkAndApplyResumePosition(currentVideoPath);
        }
    }

    private void checkAndApplyResumePosition(String path) {
        VideoPlaybackHistoryEntity history = databaseManager.getVideoHistory(path);
        if (history != null && history.getLastPositionMs() > 2000 && history.getDurationMs() > 0) {
            long resumePos = history.getLastPositionMs();
            if (resumePos < history.getDurationMs() - 5000) {
                player.seekTo(resumePos);
                CustomToast.showInfo(this, "Продолжено с " + formatDuration(resumePos));
            }
        }
    }

    private void onPlaybackEnded() {
        // Reset progress in db
        if (currentVideoPath != null) {
            databaseManager.saveVideoProgress(currentVideoPath, currentVideoTitle, player.getDuration(), 0, currentFolderName);
        }

        if (databaseManager.loadAutoPlaySetting() && currentPlaylistIndex < playlistPaths.size() - 1) {
            CustomToast.showInfo(this, "Следующее видео...");
            playVideoAtIndex(currentPlaylistIndex + 1, false);
        }
    }

    private void saveCurrentPosition() {
        if (player == null || currentVideoPath == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0 && pos > 0) {
            databaseManager.saveVideoProgress(currentVideoPath, currentVideoTitle, dur, pos, currentFolderName);
        }
    }

    private void updatePlayPauseButtons(boolean isPlaying) {
        if (btnPlay != null) btnPlay.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        if (btnPause != null) btnPause.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    private void updatePlaylistButtons() {
        if (btnPrev != null) {
            boolean hasPrev = currentPlaylistIndex > 0;
            btnPrev.setEnabled(hasPrev);
            btnPrev.setAlpha(hasPrev ? 1.0f : 0.4f);
        }
        if (btnNext != null) {
            boolean hasNext = currentPlaylistIndex < playlistPaths.size() - 1;
            btnNext.setEnabled(hasNext);
            btnNext.setAlpha(hasNext ? 1.0f : 0.4f);
        }
    }

    private void setupControlsListeners() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> player.play());
        }

        if (btnPause != null) {
            btnPause.setOnClickListener(v -> player.pause());
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentPlaylistIndex > 0) {
                    playVideoAtIndex(currentPlaylistIndex - 1, true);
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentPlaylistIndex < playlistPaths.size() - 1) {
                    playVideoAtIndex(currentPlaylistIndex + 1, true);
                }
            });
        }

        if (btnSkipForward != null) {
            btnSkipForward.setOnClickListener(v -> {
                if (player != null) {
                    long newPos = Math.min(player.getCurrentPosition() + (skipDurationSeconds * 1000L), player.getDuration());
                    player.seekTo(newPos);
                    showDoubleTapSkipIndicator(true, skipDurationSeconds);
                }
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsBottomSheet());
        }

        if (btnPip != null) {
            btnPip.setOnClickListener(v -> enterPipMode());
        }

        if (btnToggleFullscreen != null) {
            btnToggleFullscreen.setOnClickListener(v -> toggleOrientation());
        }

        if (btnLockScreen != null) {
            btnLockScreen.setOnClickListener(v -> lockScreen());
        }

        setupLockOverlayGestures();
    }

    private void toggleOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational rational = new Rational(16, 9);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(rational)
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                CustomToast.showWarning(this, "Режим PiP недоступен");
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            playerView.hideController();
            if (layoutLockedOverlay != null) layoutLockedOverlay.setVisibility(View.GONE);
        } else {
            playerView.showController();
            hideSystemUI();
        }
    }

    private void lockScreen() {
        isScreenLocked = true;
        playerView.hideController();
        if (layoutLockedOverlay != null) {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
        }
        if (lockedStatusBadge != null) {
            lockedStatusBadge.setVisibility(View.VISIBLE);
            hidePlaqueHandler.removeCallbacksAndMessages(null);
            hidePlaqueHandler.postDelayed(() -> lockedStatusBadge.setVisibility(View.GONE), 2500);
        }
        CustomToast.showInfo(this, "Экран заблокирован");
    }

    private void unlockScreen() {
        isScreenLocked = false;
        if (layoutLockedOverlay != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
        }
        playerView.showController();
        CustomToast.showSuccess(this, "Экран разблокирован");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupLockOverlayGestures() {
        if (layoutLockedOverlay == null) return;

        layoutLockedOverlay.setOnTouchListener(new View.OnTouchListener() {
            private float downY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getY();
                        if (lockedStatusBadge != null) {
                            lockedStatusBadge.setVisibility(View.VISIBLE);
                            hidePlaqueHandler.removeCallbacksAndMessages(null);
                            hidePlaqueHandler.postDelayed(() -> lockedStatusBadge.setVisibility(View.GONE), 2000);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        float deltaY = downY - event.getY();
                        // Swipe up by more than 120px to unlock
                        if (deltaY > 120) {
                            unlockScreen();
                        }
                        return true;
                }
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetector() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!isScreenLocked) {
                    if (playerView.isControllerFullyVisible()) {
                        playerView.hideController();
                    } else {
                        playerView.showController();
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (isScreenLocked || player == null) return false;
                int width = playerView.getWidth();
                if (e.getX() < width / 3.0f) {
                    // Double tap left: rewind
                    long newPos = Math.max(0, player.getCurrentPosition() - (skipDurationSeconds * 1000L));
                    player.seekTo(newPos);
                    showDoubleTapSkipIndicator(false, skipDurationSeconds);
                    return true;
                } else if (e.getX() > (width * 2.0f / 3.0f)) {
                    // Double tap right: forward
                    long newPos = Math.min(player.getDuration(), player.getCurrentPosition() + (skipDurationSeconds * 1000L));
                    player.seekTo(newPos);
                    showDoubleTapSkipIndicator(true, skipDurationSeconds);
                    return true;
                }
                return false;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if (isScreenLocked) return false;

            gestureDetector.onTouchEvent(event);

            int width = playerView.getWidth();
            int height = playerView.getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    touchDownTime = System.currentTimeMillis();
                    isTouching = true;
                    isHorizontalSeeking = false;
                    isVerticalBrightness = false;
                    isVerticalVolume = false;

                    // Hold for 2x speed trigger
                    holdSpeedHandler.postDelayed(() -> {
                        if (isTouching && !isHorizontalSeeking && !isVerticalBrightness && !isVerticalVolume) {
                            isHoldingSpeed = true;
                            if (player != null) {
                                player.setPlaybackParameters(new PlaybackParameters(2.0f));
                            }
                            if (holdSpeedToast != null) {
                                holdSpeedToast.setText("2x Скорость");
                                holdSpeedToast.setVisibility(View.VISIBLE);
                            }
                        }
                    }, 500);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;

                    if (!isHorizontalSeeking && !isVerticalBrightness && !isVerticalVolume) {
                        if (Math.abs(dx) > 30 && Math.abs(dx) > Math.abs(dy)) {
                            isHorizontalSeeking = true;
                            holdSpeedHandler.removeCallbacksAndMessages(null);
                            if (player != null) {
                                seekTargetPosition = player.getCurrentPosition();
                            }
                        } else if (Math.abs(dy) > 30) {
                            holdSpeedHandler.removeCallbacksAndMessages(null);
                            if (touchStartX < width / 2.0f) {
                                isVerticalBrightness = true;
                            } else {
                                isVerticalVolume = true;
                            }
                        }
                    }

                    if (isHorizontalSeeking && player != null) {
                        long duration = player.getDuration();
                        if (duration > 0) {
                            long deltaMs = (long) (dx / width * 90000L); // 90 sec range across screen
                            long previewPos = Math.max(0, Math.min(duration, seekTargetPosition + deltaMs));
                            showSeekPreview(previewPos, duration, deltaMs);
                        }
                    } else if (isVerticalBrightness) {
                        float deltaBrightness = -dy / height;
                        adjustBrightness(deltaBrightness);
                    } else if (isVerticalVolume) {
                        float deltaVol = -dy / height;
                        adjustVolume(deltaVol);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holdSpeedHandler.removeCallbacksAndMessages(null);
                    if (isHoldingSpeed) {
                        isHoldingSpeed = false;
                        if (player != null) {
                            player.setPlaybackParameters(new PlaybackParameters(currentPlaybackSpeed));
                        }
                        if (holdSpeedToast != null) {
                            holdSpeedToast.setVisibility(View.GONE);
                        }
                    }

                    if (isHorizontalSeeking && player != null) {
                        float finalDx = event.getX() - touchStartX;
                        long deltaMs = (long) (finalDx / width * 90000L);
                        long target = Math.max(0, Math.min(player.getDuration(), seekTargetPosition + deltaMs));
                        player.seekTo(target);
                        if (seekPreviewText != null) seekPreviewText.setVisibility(View.GONE);
                    }

                    isTouching = false;
                    isHorizontalSeeking = false;
                    isVerticalBrightness = false;
                    isVerticalVolume = false;

                    hidePlaqueHandler.postDelayed(() -> {
                        if (gestureIndicatorPlaque != null) gestureIndicatorPlaque.setVisibility(View.GONE);
                        if (seekPreviewText != null) seekPreviewText.setVisibility(View.GONE);
                    }, 1000);
                    return true;
            }
            return false;
        });
    }

    private void showSeekPreview(long targetPos, long duration, long deltaMs) {
        if (seekPreviewText == null) return;
        seekPreviewText.setVisibility(View.VISIBLE);
        String prefix = deltaMs >= 0 ? "+" : "";
        seekPreviewText.setText(String.format(Locale.getDefault(), "%s%d с (%s / %s)",
                prefix, deltaMs / 1000, formatDuration(targetPos), formatDuration(duration)));
    }

    private void adjustBrightness(float delta) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness;
        if (current < 0) current = 0.5f;
        float newBrightness = Math.max(0.01f, Math.min(1.0f, current + delta * 0.05f));
        lp.screenBrightness = newBrightness;
        getWindow().setAttributes(lp);

        int percent = Math.round(newBrightness * 100);
        showGesturePlaque(R.drawable.ic_brightness, percent);
    }

    private void adjustVolume(float delta) {
        if (audioManager == null || maxVolume <= 0) return;
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int step = delta > 0 ? 1 : (delta < 0 ? -1 : 0);
        int target = Math.max(0, Math.min(maxVolume, current + step));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);

        int percent = (target * 100) / maxVolume;
        showGesturePlaque(R.drawable.ic_volume, percent);
    }

    private void showGesturePlaque(int iconRes, int percent) {
        if (gestureIndicatorPlaque == null) return;
        gestureIndicatorPlaque.setVisibility(View.VISIBLE);
        if (gestureIndicatorIcon != null) gestureIndicatorIcon.setImageResource(iconRes);
        if (gestureIndicatorProgress != null) gestureIndicatorProgress.setProgress(percent);
        if (gestureIndicatorText != null) gestureIndicatorText.setText(percent + "%");
    }

    private void showDoubleTapSkipIndicator(boolean isForward, int seconds) {
        LinearLayout indicator = isForward ? skipIndicatorRight : skipIndicatorLeft;
        TextView text = isForward ? skipTextRight : skipTextLeft;
        if (indicator == null || text == null) return;

        text.setText((isForward ? "+" : "-") + seconds + " сек");
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(0f);
        indicator.animate().alpha(1f).setDuration(150).withEndAction(() -> {
            indicator.animate().alpha(0f).setDuration(250).setStartDelay(300).withEndAction(() -> {
                indicator.setVisibility(View.GONE);
            }).start();
        }).start();
    }

    private void showSettingsBottomSheet() {
        SettingsBottomSheet bottomSheet = new SettingsBottomSheet(
                this,
                currentPlaybackSpeed,
                speed -> {
                    currentPlaybackSpeed = speed;
                    if (player != null) player.setPlaybackParameters(new PlaybackParameters(speed));
                },
                databaseManager.loadAmbientLightSetting(),
                enabled -> {
                    databaseManager.saveAmbientLightSetting(enabled);
                    if (ambientLightManager != null) ambientLightManager.setEnabled(enabled);
                },
                databaseManager.loadSurroundSoundSetting(),
                enabled -> {
                    databaseManager.saveSurroundSoundSetting(enabled);
                    surroundSoundManager.setEnabled(enabled);
                },
                databaseManager.loadAutoPlaySetting(),
                enabled -> databaseManager.saveAutoPlaySetting(enabled),
                skipDurationSeconds,
                duration -> {
                    skipDurationSeconds = duration;
                    databaseManager.saveLongSkipDurationSetting(duration);
                },
                databaseManager.loadThemeSetting(),
                theme -> {
                    databaseManager.saveThemeSetting(theme);
                    ThemeUtils.applyTheme(theme);
                },
                currentResizeMode,
                mode -> {
                    currentResizeMode = mode;
                    if (playerView != null) playerView.setResizeMode(mode);
                }
        );

        bottomSheet.setOfflineMode(true);
        bottomSheet.show();
    }

    private String formatDuration(long millis) {
        long totalSecs = Math.max(0, millis / 1000);
        long hours = totalSecs / 3600;
        long mins = (totalSecs % 3600) / 60;
        long secs = totalSecs % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentPosition();
        if (player != null && !isInPictureInPictureMode()) {
            player.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveCurrentPosition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCurrentPosition();
        progressHandler.removeCallbacksAndMessages(null);
        holdSpeedHandler.removeCallbacksAndMessages(null);
        hidePlaqueHandler.removeCallbacksAndMessages(null);

        if (ambientLightManager != null) {
            ambientLightManager.release();
        }
        if (surroundSoundManager != null) {
            surroundSoundManager.release();
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
