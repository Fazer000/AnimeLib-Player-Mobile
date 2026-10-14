package com.example.animelib;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.animelib.controller.PlayerGestureController;
import com.example.animelib.controller.PlayerLockController;
import com.example.animelib.controller.PlayerPlaylistManager;
import com.example.animelib.controller.PlayerSystemUiController;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.managers.SurroundSoundManager;
import com.example.animelib.managers.VideoFiltersManager;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.ui.AmbientVignetteOverlayView;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.SurroundRenderersFactory;
import com.example.animelib.util.ThemeUtils;

import java.io.File;

@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity implements PlayerGestureController.GestureCallback {

    private static final String TAG = "VideoPlayerActivity";

    // Subsystem Controllers
    private PlayerSystemUiController systemUiController;
    private PlayerLockController lockController;
    private PlayerPlaylistManager playlistManager;
    private PlayerGestureController gestureController;

    // Playback & UI
    private PlayerView playerView;
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

    // Feature Managers
    private AmbientLightManager ambientLightManager;
    private SurroundSoundManager surroundSoundManager;
    private VideoFiltersManager videoFiltersManager;

    // Playback Preferences & State
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private float currentPlaybackSpeed = 1.0f;
    private int skipDurationSeconds = 85;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            saveCurrentPosition();
            progressHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        databaseManager = DatabaseManager.getInstance(this);
        ThemeUtils.applyTheme(databaseManager.loadThemeSetting());

        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnimeLIB_VideoPlayer);
        setContentView(R.layout.activity_video_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        systemUiController = new PlayerSystemUiController(this);
        systemUiController.hideSystemUI();

        playlistManager = new PlayerPlaylistManager();
        playlistManager.parseIntent(getIntent());

        skipDurationSeconds = databaseManager.loadLongSkipDurationSetting();

        initViews();
        setupExoPlayer();
        setupControllers();
        setupClickListeners();

        View mainVideoLayout = findViewById(R.id.mainVideoLayout);
        if (mainVideoLayout != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainVideoLayout, (v, insets) -> {
                int topInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top;
                if (topInset > 0) {
                    statusBarHeight = topInset;
                }
                updateLayoutForOrientation(getResources().getConfiguration().orientation);
                return insets;
            });
        }
        updateLayoutForOrientation(getResources().getConfiguration().orientation);

        playCurrentVideo(true);
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
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
        loadingOverlay = findViewById(R.id.loadingOverlay);
        bufferingIndicator = findViewById(R.id.playerBufferingIndicator);

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
        AmbientVignetteOverlayView ambientVignette = findViewById(R.id.ambientVignetteOverlay);

        if (ambientContainer != null) {
            ambientLightManager = new AmbientLightManager(this, playerView, ambientContainer,
                    ambientPlayerView, ambientImageView, ambientVignette);
            ambientLightManager.setEnabled(true);
            databaseManager.saveAmbientLightSetting(true);
        }
    }

    private void setupControllers() {
        FrameLayout layoutLockedOverlay = findViewById(R.id.layoutLockedOverlay);
        LinearLayout lockedStatusBadge = findViewById(R.id.lockedStatusBadge);

        lockController = new PlayerLockController(this, layoutLockedOverlay, lockedStatusBadge);
        lockController.setOnLockStateChangeListener(isLocked -> {
            if (isLocked) {
                playerView.hideController();
            } else {
                playerView.showController();
            }
        });

        gestureController = new PlayerGestureController(
                this,
                playerView,
                findViewById(R.id.seekPreviewText),
                findViewById(R.id.holdSpeedToast),
                findViewById(R.id.skipIndicatorLeft),
                findViewById(R.id.skipTextLeft),
                findViewById(R.id.skipIndicatorRight),
                findViewById(R.id.skipTextRight),
                findViewById(R.id.gestureIndicatorPlaque),
                findViewById(R.id.gestureIndicatorIcon),
                findViewById(R.id.gestureIndicatorProgress),
                findViewById(R.id.gestureIndicatorText),
                this
        );
    }

    private void setupClickListeners() {
        if (btnClose != null) btnClose.setOnClickListener(v -> finish());
        if (btnSettings != null) btnSettings.setOnClickListener(v -> {
            if (playerView != null) playerView.showController();
            showSettingsBottomSheet();
        });
        if (btnPip != null) btnPip.setOnClickListener(v -> enterPipMode());
        if (btnToggleFullscreen != null) btnToggleFullscreen.setOnClickListener(v -> {
            if (playerView != null) playerView.showController();
            toggleFullscreen();
        });
        if (btnLockScreen != null) btnLockScreen.setOnClickListener(v -> lockController.lock());

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (player != null) {
                    player.play();
                    updatePlayPauseButtons(true);
                    if (playerView != null) playerView.showController();
                }
            });
        }
        if (btnPause != null) {
            btnPause.setOnClickListener(v -> {
                if (player != null) {
                    player.pause();
                    updatePlayPauseButtons(false);
                    if (playerView != null) playerView.showController();
                }
            });
        }
        if (btnNext != null) btnNext.setOnClickListener(v -> {
            if (playerView != null) playerView.showController();
            playNextVideo();
        });
        if (btnPrev != null) btnPrev.setOnClickListener(v -> {
            if (playerView != null) playerView.showController();
            playPreviousVideo();
        });

        if (btnSkipForward != null) {
            btnSkipForward.setOnClickListener(v -> {
                if (player != null) {
                    long targetPos = Math.min(player.getDuration(), player.getCurrentPosition() + (skipDurationSeconds * 1000L));
                    player.seekTo(targetPos);
                    gestureController.showDoubleTapSkipIndicator(true, skipDurationSeconds);
                    if (playerView != null) playerView.showController();
                }
            });
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
        playerView.setControllerShowTimeoutMs(3000);
        playerView.setControllerHideOnTouch(true);
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            boolean isVisible = (visibility == View.VISIBLE);
            animateControlsVisibility(isVisible);

            if (systemUiController != null) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    systemUiController.hideSystemUI();
                } else {
                    systemUiController.showSystemUI();
                }
            }
        });

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

    private void playCurrentVideo(boolean shouldResume) {
        String videoPath = playlistManager.getCurrentPath();
        if (videoPath == null || videoPath.isEmpty()) {
            CustomToast.showWarning(this, "Путь к видео не указан");
            finish();
            return;
        }

        if (tvAnimeTitle != null) {
            tvAnimeTitle.setText(playlistManager.getCurrentTitle());
        }

        Uri videoUri;
        if (videoPath.startsWith("content://") || videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            videoUri = Uri.parse(videoPath);
        } else {
            videoUri = Uri.fromFile(new File(videoPath));
        }

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);

        if (ambientLightManager != null) {
            ambientLightManager.setVideoUrl(videoPath);
        }

        long resumePosition = 0;
        if (shouldResume) {
            VideoPlaybackHistoryEntity history = databaseManager.getVideoHistory(videoPath);
            if (history != null && history.getLastPositionMs() > 5000) {
                resumePosition = history.getLastPositionMs();
            }
        }

        player.prepare();
        if (resumePosition > 0) {
            player.seekTo(resumePosition);
        }
        player.play();

        updatePlaylistNavigationButtons();
    }

    private void playVideoAtIndex(int index, boolean shouldResume) {
        if (playlistManager.isEmpty() || index < 0 || index >= playlistManager.getPlaylistPaths().size()) return;
        saveCurrentPosition();
        playlistManager.setCurrentIndex(index);
        playCurrentVideo(shouldResume);
    }

    private void playNextVideo() {
        if (playlistManager.hasNext()) {
            playVideoAtIndex(playlistManager.getNextIndex(), false);
        } else {
            CustomToast.showInfo(this, "Это последнее видео в списке");
        }
    }

    private void playPreviousVideo() {
        if (playlistManager.hasPrevious()) {
            playVideoAtIndex(playlistManager.getPreviousIndex(), false);
        } else {
            CustomToast.showInfo(this, "Это первое видео в списке");
        }
    }

    private void onPlaybackEnded() {
        saveCurrentPosition();
        if (databaseManager.loadAutoPlaySetting() && playlistManager.hasNext()) {
            CustomToast.showInfo(this, "Воспроизведение следующего видео...");
            playNextVideo();
        }
    }

    private void updatePlaylistNavigationButtons() {
        if (btnPrev != null) {
            btnPrev.setAlpha(playlistManager.hasPrevious() ? 1.0f : 0.4f);
            btnPrev.setEnabled(playlistManager.hasPrevious());
        }
        if (btnNext != null) {
            btnNext.setAlpha(playlistManager.hasNext() ? 1.0f : 0.4f);
            btnNext.setEnabled(playlistManager.hasNext());
        }
    }

    private void updatePlayPauseButtons(boolean isPlaying) {
        if (btnPlay == null || btnPause == null) return;

        View showView = isPlaying ? btnPause : btnPlay;
        View hideView = isPlaying ? btnPlay : btnPause;

        if (showView.getVisibility() == View.VISIBLE && showView.getAlpha() == 1.0f && hideView.getVisibility() == View.GONE) {
            return;
        }

        hideView.animate().cancel();
        showView.animate().cancel();

        hideView.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .scaleY(0.5f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    hideView.setVisibility(View.GONE);
                    hideView.setScaleX(1f);
                    hideView.setScaleY(1f);
                    hideView.setAlpha(1f);
                })
                .start();

        showView.setVisibility(View.VISIBLE);
        showView.setAlpha(0f);
        showView.setScaleX(0.5f);
        showView.setScaleY(0.5f);
        showView.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    private void animateControlsVisibility(boolean isVisible) {
        View topBar = findViewById(R.id.topBarContainer);
        View bottomBar = findViewById(R.id.playersControlBar);
        View centerBar = findViewById(R.id.centerControlsContainer);
        View btnLock = findViewById(R.id.btnLockScreen);

        long duration = isVisible ? 220L : 180L;
        Interpolator interpolator = isVisible 
                ? new DecelerateInterpolator(1.2f) 
                : new AccelerateInterpolator(1.2f);

        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        View[] viewsToAnimate = new View[]{topBar, bottomBar, centerBar, isPortrait ? null : btnLock};

        for (View view : viewsToAnimate) {
            if (view == null) continue;

            view.animate().cancel();
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);

            if (isVisible) {
                if (view.getVisibility() != View.VISIBLE) {
                    view.setVisibility(View.VISIBLE);
                    view.setAlpha(0f);
                }
                view.animate()
                        .alpha(1f)
                        .setDuration(duration)
                        .setInterpolator(interpolator)
                        .start();
            } else {
                view.animate()
                        .alpha(0f)
                        .setDuration(duration)
                        .setInterpolator(interpolator)
                        .withEndAction(() -> {
                            view.setVisibility(View.GONE);
                            view.setAlpha(1f);
                        })
                        .start();
            }
        }

        if (btnLock != null && isPortrait) {
            btnLock.animate().cancel();
            btnLock.setVisibility(View.GONE);
        }
    }

    private void saveCurrentPosition() {
        if (player == null) return;
        String path = playlistManager.getCurrentPath();
        if (path == null || path.isEmpty()) return;

        long currentPos = player.getCurrentPosition();
        long duration = player.getDuration();
        if (duration > 0) {
            databaseManager.saveVideoProgress(
                    path,
                    playlistManager.getCurrentTitle(),
                    duration,
                    currentPos,
                    playlistManager.getCurrentFolderName()
            );
        }
    }

    private void toggleFullscreen() {
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
            findViewById(R.id.layoutLockedOverlay).setVisibility(View.GONE);
        } else {
            playerView.showController();
            systemUiController.hideSystemUI();
        }
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

    // --- GestureCallback Implementation ---

    @Override
    public void onSingleTap() {
        if (playerView.isControllerFullyVisible()) {
            playerView.hideController();
        } else {
            playerView.showController();
        }
    }

    @Override
    public void onSeekCommit(long targetPositionMs) {
        if (player != null) {
            player.seekTo(targetPositionMs);
        }
    }

    @Override
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    @Override
    public void onHoldSpeed(boolean isHolding) {
        if (player != null) {
            float speed = isHolding ? 2.0f : currentPlaybackSpeed;
            player.setPlaybackParameters(new PlaybackParameters(speed));
        }
    }

    @Override
    public boolean isLocked() {
        return lockController != null && lockController.isLocked();
    }

    @Override
    public int getSkipDurationSeconds() {
        return skipDurationSeconds;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayoutForOrientation(newConfig.orientation);
        if (ambientLightManager != null) {
            ambientLightManager.onConfigurationChanged();
        }
    }

    private int statusBarHeight = 0;

    private void updateLayoutForOrientation(int orientation) {
        View playerContainer = findViewById(R.id.playerContainer);
        View portraitBottomContent = findViewById(R.id.portraitBottomContent);
        View ambientContainer = findViewById(R.id.ambientContainer);
        View animeInfoContainer = findViewById(R.id.animeInfoContainer);
        View btnLock = findViewById(R.id.btnLockScreen);
        View topBar = findViewById(R.id.topBarContainer);
        View timeAndControls = findViewById(R.id.timeAndControlsContainer);
        View timeBarContainer = findViewById(R.id.timeBarContainer);

        if (playerContainer == null) return;

        boolean isLandscape = (orientation == Configuration.ORIENTATION_LANDSCAPE);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) playerContainer.getLayoutParams();

        if (isLandscape) {
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            params.height = LinearLayout.LayoutParams.MATCH_PARENT;
            params.topMargin = 0;
            params.leftMargin = 0;
            params.rightMargin = 0;
            params.bottomMargin = 0;
            playerContainer.setLayoutParams(params);

            if (playerContainer instanceof com.example.animelib.ui.RoundedCornerFrameLayout) {
                ((com.example.animelib.ui.RoundedCornerFrameLayout) playerContainer).setCornerRadius(0f);
            }

            if (portraitBottomContent != null) {
                portraitBottomContent.setVisibility(View.GONE);
            }

            if (animeInfoContainer != null) {
                animeInfoContainer.setVisibility(View.VISIBLE);
            }

            if (btnLock != null) {
                btnLock.setVisibility(View.VISIBLE);
            }

            if (topBar != null && topBar.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams topBarParams = (android.view.ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
                topBarParams.topMargin = dpToPx(16);
                topBarParams.leftMargin = dpToPx(16);
                topBarParams.rightMargin = dpToPx(16);
                topBar.setLayoutParams(topBarParams);
            }

            if (timeAndControls != null && timeAndControls.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams tcParams = (android.view.ViewGroup.MarginLayoutParams) timeAndControls.getLayoutParams();
                tcParams.leftMargin = dpToPx(16);
                tcParams.rightMargin = dpToPx(16);
                timeAndControls.setLayoutParams(tcParams);
            }

            if (timeBarContainer != null && timeBarContainer.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams tbParams = (android.view.ViewGroup.MarginLayoutParams) timeBarContainer.getLayoutParams();
                tbParams.leftMargin = dpToPx(8);
                tbParams.rightMargin = dpToPx(8);
                tbParams.bottomMargin = dpToPx(16);
                timeBarContainer.setLayoutParams(tbParams);
            }

            if (systemUiController != null) {
                systemUiController.hideSystemUI();
            }
        } else {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int horizontalPadding = dpToPx(12);
            int playerWidth = screenWidth - (horizontalPadding * 2);
            int playerHeight = (int) (playerWidth * (9.0f / 16.0f));

            int topMargin = statusBarHeight > 0 ? statusBarHeight + dpToPx(6) : dpToPx(32);

            params.width = playerWidth;
            params.height = playerHeight;
            params.topMargin = topMargin;
            params.leftMargin = horizontalPadding;
            params.rightMargin = horizontalPadding;
            params.bottomMargin = dpToPx(12);
            playerContainer.setLayoutParams(params);

            if (playerContainer instanceof com.example.animelib.ui.RoundedCornerFrameLayout) {
                ((com.example.animelib.ui.RoundedCornerFrameLayout) playerContainer).setCornerRadius(dpToPx(16));
            }

            if (portraitBottomContent != null) {
                portraitBottomContent.setVisibility(View.VISIBLE);
            }

            // Hide Title and Lock Button in Portrait Mode
            if (animeInfoContainer != null) {
                animeInfoContainer.setVisibility(View.GONE);
            }

            if (btnLock != null) {
                btnLock.setVisibility(View.GONE);
            }

            // Compact control margins in portrait mode
            if (topBar != null && topBar.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams topBarParams = (android.view.ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
                topBarParams.topMargin = dpToPx(6);
                topBarParams.leftMargin = dpToPx(8);
                topBarParams.rightMargin = dpToPx(8);
                topBar.setLayoutParams(topBarParams);
            }

            if (timeAndControls != null && timeAndControls.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams tcParams = (android.view.ViewGroup.MarginLayoutParams) timeAndControls.getLayoutParams();
                tcParams.leftMargin = dpToPx(8);
                tcParams.rightMargin = dpToPx(8);
                timeAndControls.setLayoutParams(tcParams);
            }

            if (timeBarContainer != null && timeBarContainer.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams tbParams = (android.view.ViewGroup.MarginLayoutParams) timeBarContainer.getLayoutParams();
                tbParams.leftMargin = dpToPx(4);
                tbParams.rightMargin = dpToPx(4);
                tbParams.bottomMargin = dpToPx(4);
                timeBarContainer.setLayoutParams(tbParams);
            }

            if (ambientContainer != null) {
                ambientContainer.setVisibility(View.VISIBLE);
            }
            if (ambientLightManager != null) {
                ambientLightManager.setEnabled(true);
            }

            if (systemUiController != null) {
                systemUiController.showSystemUI();
            }
        }

        AmbientVignetteOverlayView ambientVignetteOverlay = findViewById(R.id.ambientVignetteOverlay);

        if (ambientVignetteOverlay != null) {
            ambientVignetteOverlay.setPortraitMode(!isLandscape);
        }

        if (ambientContainer != null) {
            FrameLayout.LayoutParams ambParams = (FrameLayout.LayoutParams) ambientContainer.getLayoutParams();
            if (isLandscape) {
                ambParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                ambParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
                ambParams.topMargin = 0;
                ambParams.leftMargin = 0;
                ambParams.rightMargin = 0;
                ambParams.bottomMargin = 0;
            } else {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int horizontalPadding = dpToPx(12);
                int playerWidth = screenWidth - (horizontalPadding * 2);
                int playerHeight = (int) (playerWidth * (9.0f / 16.0f));
                int topMargin = statusBarHeight > 0 ? statusBarHeight + dpToPx(6) : dpToPx(32);

                ambParams.width = playerWidth;
                ambParams.height = dpToPx(140);
                ambParams.topMargin = topMargin + playerHeight;
                ambParams.leftMargin = horizontalPadding;
                ambParams.rightMargin = horizontalPadding;
                ambParams.bottomMargin = 0;
            }
            ambientContainer.setLayoutParams(ambParams);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && systemUiController != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                systemUiController.hideSystemUI();
            } else {
                systemUiController.showSystemUI();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (systemUiController != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                systemUiController.hideSystemUI();
            } else {
                systemUiController.showSystemUI();
            }
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

        if (gestureController != null) {
            gestureController.release();
        }
        if (lockController != null) {
            lockController.release();
        }
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
