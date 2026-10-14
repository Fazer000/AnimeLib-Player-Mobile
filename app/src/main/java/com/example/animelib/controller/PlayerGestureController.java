package com.example.animelib.controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.ui.PlayerView;

import com.example.animelib.R;

import java.util.Locale;

/**
 * Контроллер жестов воспроизведения видео (яркость, громкость, перемотка свайпом, 2x скорость, двойной тап).
 */
public class PlayerGestureController {

    public interface GestureCallback {
        void onSingleTap();
        void onSeekCommit(long targetPositionMs);
        long getCurrentPosition();
        long getDuration();
        void onHoldSpeed(boolean isHolding);
        boolean isLocked();
        int getSkipDurationSeconds();
    }

    private final Activity activity;
    private final PlayerView playerView;
    private final GestureCallback callback;

    private final AudioManager audioManager;
    private final int maxVolume;

    // UI Overlays
    private final TextView seekPreviewText;
    private final TextView holdSpeedToast;
    private final LinearLayout skipIndicatorLeft;
    private final TextView skipTextLeft;
    private final LinearLayout skipIndicatorRight;
    private final TextView skipTextRight;
    private final LinearLayout gestureIndicatorPlaque;
    private final ImageView gestureIndicatorIcon;
    private final ProgressBar gestureIndicatorProgress;
    private final TextView gestureIndicatorText;

    // Gesture State
    private float touchStartX, touchStartY;
    private boolean isTouching = false;
    private boolean isHorizontalSeeking = false;
    private boolean isVerticalBrightness = false;
    private boolean isVerticalVolume = false;
    private boolean isHoldingSpeed = false;
    private long seekTargetPosition = 0;

    private final Handler holdSpeedHandler = new Handler(Looper.getMainLooper());
    private final Handler hidePlaqueHandler = new Handler(Looper.getMainLooper());

    public PlayerGestureController(@NonNull Activity activity,
                                   @NonNull PlayerView playerView,
                                   @Nullable TextView seekPreviewText,
                                   @Nullable TextView holdSpeedToast,
                                   @Nullable LinearLayout skipIndicatorLeft,
                                   @Nullable TextView skipTextLeft,
                                   @Nullable LinearLayout skipIndicatorRight,
                                   @Nullable TextView skipTextRight,
                                   @Nullable LinearLayout gestureIndicatorPlaque,
                                   @Nullable ImageView gestureIndicatorIcon,
                                   @Nullable ProgressBar gestureIndicatorProgress,
                                   @Nullable TextView gestureIndicatorText,
                                   @NonNull GestureCallback callback) {
        this.activity = activity;
        this.playerView = playerView;
        this.seekPreviewText = seekPreviewText;
        this.holdSpeedToast = holdSpeedToast;
        this.skipIndicatorLeft = skipIndicatorLeft;
        this.skipTextLeft = skipTextLeft;
        this.skipIndicatorRight = skipIndicatorRight;
        this.skipTextRight = skipTextRight;
        this.gestureIndicatorPlaque = gestureIndicatorPlaque;
        this.gestureIndicatorIcon = gestureIndicatorIcon;
        this.gestureIndicatorProgress = gestureIndicatorProgress;
        this.gestureIndicatorText = gestureIndicatorText;
        this.callback = callback;

        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.maxVolume = audioManager != null ? audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 15;

        setupGestureListeners();
    }

    public void release() {
        holdSpeedHandler.removeCallbacksAndMessages(null);
        hidePlaqueHandler.removeCallbacksAndMessages(null);
    }

    private float initialBrightness = 0.5f;
    private int initialVolume = 0;

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureListeners() {
        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                if (!callback.isLocked()) {
                    callback.onSingleTap();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (callback.isLocked()) return false;
                int width = playerView.getWidth();
                int skipSec = callback.getSkipDurationSeconds();
                long currentPos = callback.getCurrentPosition();
                long duration = callback.getDuration();

                if (e.getX() < width / 3.0f) {
                    // Double tap left: rewind
                    long newPos = Math.max(0, currentPos - (skipSec * 1000L));
                    callback.onSeekCommit(newPos);
                    showDoubleTapSkipIndicator(false, skipSec);
                    return true;
                } else if (e.getX() > (width * 2.0f / 3.0f)) {
                    // Double tap right: forward
                    long newPos = Math.min(duration, currentPos + (skipSec * 1000L));
                    callback.onSeekCommit(newPos);
                    showDoubleTapSkipIndicator(true, skipSec);
                    return true;
                }
                return false;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if (callback.isLocked()) return false;

            gestureDetector.onTouchEvent(event);

            int width = playerView.getWidth();
            int height = playerView.getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    isTouching = true;
                    isHorizontalSeeking = false;
                    isVerticalBrightness = false;
                    isVerticalVolume = false;

                    float curB = activity.getWindow().getAttributes().screenBrightness;
                    initialBrightness = curB < 0 ? 0.5f : curB;
                    initialVolume = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : 0;

                    // Hold for 2x speed
                    holdSpeedHandler.postDelayed(() -> {
                        if (isTouching && !isHorizontalSeeking && !isVerticalBrightness && !isVerticalVolume) {
                            isHoldingSpeed = true;
                            callback.onHoldSpeed(true);
                            if (holdSpeedToast != null) {
                                holdSpeedToast.setText("2.0x");
                                holdSpeedToast.setVisibility(View.VISIBLE);
                            }
                        }
                    }, 500);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isHoldingSpeed) {
                        return true;
                    }

                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;

                    if (!isHorizontalSeeking && !isVerticalBrightness && !isVerticalVolume) {
                        if (Math.abs(dx) > 30 && Math.abs(dx) > Math.abs(dy)) {
                            isHorizontalSeeking = true;
                            holdSpeedHandler.removeCallbacksAndMessages(null);
                            seekTargetPosition = callback.getCurrentPosition();
                        } else if (Math.abs(dy) > 30) {
                            holdSpeedHandler.removeCallbacksAndMessages(null);
                            if (touchStartX < width / 2.0f) {
                                isVerticalBrightness = true;
                            } else {
                                isVerticalVolume = true;
                            }
                        }
                    }

                    if (isHorizontalSeeking) {
                        long duration = callback.getDuration();
                        if (duration > 0) {
                            long deltaMs = (long) (dx / width * 90000L); // 90 sec range
                            long previewPos = Math.max(0, Math.min(duration, seekTargetPosition + deltaMs));
                            showSeekPreview(previewPos, duration, deltaMs);
                        }
                    } else if (isVerticalBrightness) {
                        float deltaRatio = -dy / height;
                        float targetBrightness = Math.max(0.01f, Math.min(1.0f, initialBrightness + deltaRatio));
                        setBrightness(targetBrightness);
                    } else if (isVerticalVolume) {
                        float deltaRatio = -dy / height;
                        int targetVolume = Math.max(0, Math.min(maxVolume, Math.round(initialVolume + deltaRatio * maxVolume)));
                        setVolume(targetVolume);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holdSpeedHandler.removeCallbacksAndMessages(null);
                    if (isHoldingSpeed) {
                        isHoldingSpeed = false;
                        callback.onHoldSpeed(false);
                        if (holdSpeedToast != null) {
                            holdSpeedToast.setVisibility(View.GONE);
                        }
                    }

                    if (isHorizontalSeeking) {
                        float finalDx = event.getX() - touchStartX;
                        long deltaMs = (long) (finalDx / width * 90000L);
                        long target = Math.max(0, Math.min(callback.getDuration(), seekTargetPosition + deltaMs));
                        callback.onSeekCommit(target);
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

    private void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = brightness;
        activity.getWindow().setAttributes(lp);

        int percent = Math.round(brightness * 100);
        showGesturePlaque(R.drawable.ic_brightness, percent);
    }

    private void setVolume(int volume) {
        if (audioManager == null || maxVolume <= 0) return;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

        int percent = (volume * 100) / maxVolume;
        showGesturePlaque(R.drawable.ic_volume, percent);
    }

    private void showGesturePlaque(int iconRes, int percent) {
        if (gestureIndicatorPlaque == null) return;
        gestureIndicatorPlaque.setVisibility(View.VISIBLE);
        if (gestureIndicatorIcon != null) gestureIndicatorIcon.setImageResource(iconRes);
        if (gestureIndicatorProgress != null) gestureIndicatorProgress.setProgress(percent);
        if (gestureIndicatorText != null) gestureIndicatorText.setText(percent + "%");
    }

    public void showDoubleTapSkipIndicator(boolean isForward, int seconds) {
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
}
