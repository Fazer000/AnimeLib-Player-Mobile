package com.example.animelib.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.models.SubtitleData;
import com.example.animelib.ui.UpdateActivity;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class SettingsBottomSheet extends FlexibleBottomSheetDialog {

    public interface OnSpeedChangedListener {
        void onSpeedChanged(float speed);
    }
    public interface OnAmbientLightToggledListener {
        void onAmbientLightToggled(boolean enabled);
    }
    public interface OnSurroundSoundToggledListener {
        void onSurroundSoundToggled(boolean enabled);
    }
    public interface OnAutoPlayToggledListener {
        void onAutoPlayToggled(boolean enabled);
    }
    public interface OnSkipDurationChangedListener {
        void onSkipDurationChanged(int duration);
    }
    public interface OnThemeChangedListener {
        void onThemeChanged(int theme);
    }
    public interface OnResizeModeChangedListener {
        void onResizeModeChanged(int resizeMode);
    }
    public interface OnVideoFiltersChangedListener {
        void onVideoFiltersChanged(float brightness, float contrast, float saturation, float gamma, float hue);
    }

    private float currentPlaybackSpeed = 1.0f;
    private final OnSpeedChangedListener speedListener;
    private boolean enableAmbientLight = true;
    private final OnAmbientLightToggledListener onAmbientLightToggledListener;
    private boolean enableSurroundSound = true;
    private final OnSurroundSoundToggledListener onSurroundSoundToggledListener;
    private boolean autoPlay = true;
    private final OnAutoPlayToggledListener onAutoPlayToggledListener;
    private int longSkipDuration = 85;
    private final OnSkipDurationChangedListener onSkipDurationChangedListener;
    private int currentTheme = ThemeUtils.THEME_SYSTEM;
    private final OnThemeChangedListener onThemeChangedListener;
    private int currentResizeMode = 0;
    private OnResizeModeChangedListener onResizeModeChangedListener;

    private boolean subtitlesEnabled = true;
    private String subtitleFormat = "ass";
    private List<SubtitleData> availableSubtitles = new ArrayList<>();
    private float subtitleTextSize = 18f;
    private int subtitleTextColor = 0xFFFFFFFF;
    private int subtitleBackgroundColor = 0x00000000;
    private int subtitleEdgeType = 1;
    private int subtitleEdgeColor = 0xFF000000;

    private float filterBrightness = 0f;
    private float filterContrast = 100f;
    private float filterSaturation = 100f;
    private float filterGamma = 1.0f;
    private float filterHue = 0f;
    private OnVideoFiltersChangedListener onVideoFiltersChangedListener;

    public SettingsBottomSheet(@NonNull Context context,
                               float currentSpeed,
                               OnSpeedChangedListener speedListener,
                               boolean enableAmbientLight,
                               OnAmbientLightToggledListener onAmbientLightToggledListener,
                               boolean enableSurroundSound,
                               OnSurroundSoundToggledListener onSurroundSoundToggledListener,
                               boolean autoPlay,
                               OnAutoPlayToggledListener onAutoPlayToggledListener,
                               int longSkipDuration,
                               OnSkipDurationChangedListener onSkipDurationChangedListener,
                               int currentTheme,
                               OnThemeChangedListener onThemeChangedListener,
                               int currentResizeMode,
                               OnResizeModeChangedListener onResizeModeChangedListener) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.currentPlaybackSpeed = currentSpeed;
        this.speedListener = speedListener;
        this.enableAmbientLight = enableAmbientLight;
        this.onAmbientLightToggledListener = onAmbientLightToggledListener;
        this.enableSurroundSound = enableSurroundSound;
        this.onSurroundSoundToggledListener = onSurroundSoundToggledListener;
        this.autoPlay = autoPlay;
        this.onAutoPlayToggledListener = onAutoPlayToggledListener;
        this.longSkipDuration = longSkipDuration;
        this.onSkipDurationChangedListener = onSkipDurationChangedListener;
        this.currentTheme = currentTheme;
        this.onThemeChangedListener = onThemeChangedListener;
        this.currentResizeMode = currentResizeMode;
        this.onResizeModeChangedListener = onResizeModeChangedListener;
    }

    public void setOfflineMode(boolean offlineMode) {
        // Kept for backward compatibility
    }

    public void setVideoFilters(float brightness, float contrast, float saturation, float gamma, float hue, OnVideoFiltersChangedListener listener) {
        this.filterBrightness = brightness;
        this.filterContrast = contrast;
        this.filterSaturation = saturation;
        this.filterGamma = gamma;
        this.filterHue = hue;
        this.onVideoFiltersChangedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_settings, null);
        setContentView(view);

        ImageButton exitBtn = view.findViewById(R.id.bs_exit);
        if (exitBtn != null) {
            exitBtn.setOnClickListener(v -> dismiss());
        }

        // Speed option
        View speedOption = view.findViewById(R.id.speedOption);
        TextView currentSpeedText = view.findViewById(R.id.currentSpeedText);
        if (currentSpeedText != null) currentSpeedText.setText(String.format(java.util.Locale.US, "%.1fx", currentPlaybackSpeed));
        if (speedOption != null) {
            speedOption.setOnClickListener(v -> showSpeedDialog());
        }

        // Resize mode
        View resizeModeOption = view.findViewById(R.id.resizeModeOption);
        TextView currentResizeModeText = view.findViewById(R.id.currentResizeModeText);
        if (currentResizeModeText != null) currentResizeModeText.setText(getResizeModeName(currentResizeMode));
        if (resizeModeOption != null) {
            resizeModeOption.setOnClickListener(v -> showResizeModeDialog());
        }

        // Ambient light
        MaterialSwitch ambientSwitch = view.findViewById(R.id.ambientLightSwitch);
        if (ambientSwitch != null) {
            ambientSwitch.setChecked(enableAmbientLight);
            ambientSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                enableAmbientLight = isChecked;
                if (onAmbientLightToggledListener != null) {
                    onAmbientLightToggledListener.onAmbientLightToggled(isChecked);
                }
            });
        }

        // Surround sound
        View surroundOption = view.findViewById(R.id.surroundSoundOption);
        MaterialSwitch surroundSwitch = view.findViewById(R.id.surroundSoundSwitch);
        if (surroundSwitch != null) {
            surroundSwitch.setChecked(enableSurroundSound);
            surroundSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                enableSurroundSound = isChecked;
                if (onSurroundSoundToggledListener != null) {
                    onSurroundSoundToggledListener.onSurroundSoundToggled(isChecked);
                }
            });
        }
        if (surroundOption != null) {
            surroundOption.setOnClickListener(v -> showSurroundDialog());
        }

        // Video filters
        View videoFiltersOption = view.findViewById(R.id.videoFiltersOption);
        if (videoFiltersOption != null) {
            videoFiltersOption.setOnClickListener(v -> showVideoFiltersDialog());
        }

        // Skip duration
        View skipDurationOption = view.findViewById(R.id.skipDurationOption);
        TextView currentSkipText = view.findViewById(R.id.currentSkipDurationText);
        if (currentSkipText != null) currentSkipText.setText(longSkipDuration + " сек");
        if (skipDurationOption != null) {
            skipDurationOption.setOnClickListener(v -> showSkipDurationDialog());
        }

        // AutoPlay switch
        MaterialSwitch autoPlaySwitch = view.findViewById(R.id.autoPlaySwitch);
        if (autoPlaySwitch != null) {
            autoPlaySwitch.setChecked(autoPlay);
            autoPlaySwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                autoPlay = isChecked;
                if (onAutoPlayToggledListener != null) {
                    onAutoPlayToggledListener.onAutoPlayToggled(isChecked);
                }
            });
        }

        // Subtitles option
        View subtitlesOption = view.findViewById(R.id.subtitlesOption);
        MaterialSwitch subSwitch = view.findViewById(R.id.subtitlesSwitch);
        if (subSwitch != null) {
            subSwitch.setChecked(subtitlesEnabled);
            subSwitch.setOnCheckedChangeListener((btn, isChecked) -> subtitlesEnabled = isChecked);
        }
        if (subtitlesOption != null) {
            subtitlesOption.setOnClickListener(v -> showSubtitlesDialog());
        }

        // Theme option
        View themeOption = view.findViewById(R.id.themeOption);
        TextView currentThemeText = view.findViewById(R.id.currentThemeText);
        if (currentThemeText != null) currentThemeText.setText(ThemeUtils.getThemeName(currentTheme));
        if (themeOption != null) {
            themeOption.setOnClickListener(v -> showThemeDialog());
        }

        // Bug Report (GitHub Issue BS)
        View bugReportOption = view.findViewById(R.id.bugReportOption);
        if (bugReportOption != null) {
            bugReportOption.setOnClickListener(v -> {
                dismiss();
                CreateIssueBottomSheet dialog = new CreateIssueBottomSheet(getContext());
                dialog.setOnBackPressedListener(this::show);
                dialog.show();
            });
        }

        // Check for Updates via GitHub
        TextView appVersionText = view.findViewById(R.id.appVersionText);
        if (appVersionText != null) appVersionText.setText("Версия 1.0.0 (Fazer000)");
        View checkForUpdatesOption = view.findViewById(R.id.checkForUpdatesOption);
        if (checkForUpdatesOption != null) {
            checkForUpdatesOption.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), UpdateActivity.class);
                    getContext().startActivity(intent);
                    dismiss();
                } catch (Exception e) {
                    CustomToast.showError(getContext(), "Не удалось открыть страницу обновлений");
                }
            });
        }

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private boolean isVersionNewer(String latestTag, String currentVersion) {
        if (latestTag == null || latestTag.isEmpty()) return false;
        String l = latestTag.replaceAll("(?i)^v", "").replaceAll("-.*$", "").trim();
        String c = currentVersion.replaceAll("(?i)^v", "").replaceAll("-.*$", "").trim();
        if (l.equalsIgnoreCase(c)) return false;
        String[] lParts = l.split("\\.");
        String[] cParts = c.split("\\.");
        int length = Math.max(lParts.length, cParts.length);
        for (int i = 0; i < length; i++) {
            int lNum = 0;
            int cNum = 0;
            try {
                if (i < lParts.length) lNum = Integer.parseInt(lParts[i].replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
            try {
                if (i < cParts.length) cNum = Integer.parseInt(cParts[i].replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
            if (lNum > cNum) return true;
            if (lNum < cNum) return false;
        }
        return !l.equalsIgnoreCase(c);
    }

    private void showSpeedDialog() {
        dismiss();
        SpeedBottomSheet dialog = new SpeedBottomSheet(getContext(), currentPlaybackSpeed, speed -> {
            currentPlaybackSpeed = speed;
            if (speedListener != null) speedListener.onSpeedChanged(speed);
        });
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showResizeModeDialog() {
        dismiss();
        ResizeModeBottomSheet dialog = new ResizeModeBottomSheet(getContext(), currentResizeMode, mode -> {
            currentResizeMode = mode;
            if (onResizeModeChangedListener != null) onResizeModeChangedListener.onResizeModeChanged(mode);
        });
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showSurroundDialog() {
        dismiss();
        com.example.animelib.data.DatabaseManager db = com.example.animelib.data.DatabaseManager.getInstance(getContext());
        SurroundSoundBottomSheet dialog = new SurroundSoundBottomSheet(
                getContext(),
                enableSurroundSound,
                db.loadSurround3DMode(),
                db.loadSurroundSpatialWidth(),
                db.loadSurroundDialogueBoost(),
                db.loadSurroundBassBoost(),
                db.loadSurroundTrebleBoost(),
                (enabled, mode, spatial, dialogue, bass, treble) -> {
                    enableSurroundSound = enabled;
                    db.saveSurroundSoundSetting(enabled);
                    db.saveSurround3DSettings(mode, spatial, dialogue, bass, treble);
                    if (onSurroundSoundToggledListener != null) {
                        onSurroundSoundToggledListener.onSurroundSoundToggled(enabled);
                    }
                }
        );
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showSkipDurationDialog() {
        dismiss();
        SkipDurationBottomSheet dialog = new SkipDurationBottomSheet(getContext(), longSkipDuration, duration -> {
            longSkipDuration = duration;
            if (onSkipDurationChangedListener != null) onSkipDurationChangedListener.onSkipDurationChanged(duration);
        });
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showVideoFiltersDialog() {
        dismiss();
        VideoFiltersBottomSheet dialog = new VideoFiltersBottomSheet(
                getContext(),
                filterBrightness, filterContrast, filterSaturation, filterGamma, filterHue,
                (b, c, s, g, h) -> {
                    filterBrightness = b;
                    filterContrast = c;
                    filterSaturation = s;
                    filterGamma = g;
                    filterHue = h;
                    if (onVideoFiltersChangedListener != null) {
                        onVideoFiltersChangedListener.onVideoFiltersChanged(b, c, s, g, h);
                    }
                }
        );
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showSubtitlesDialog() {
        dismiss();
        SubtitlesBottomSheet dialog = new SubtitlesBottomSheet(
                getContext(),
                subtitlesEnabled,
                subtitleFormat,
                availableSubtitles,
                enabled -> subtitlesEnabled = enabled,
                format -> subtitleFormat = format
        );
        dialog.setStyleSettings(subtitleTextSize, subtitleTextColor, subtitleBackgroundColor,
                subtitleEdgeType, subtitleEdgeColor, (size, col, bg, edge, edgeCol) -> {
                    subtitleTextSize = size;
                    subtitleTextColor = col;
                    subtitleBackgroundColor = bg;
                    subtitleEdgeType = edge;
                    subtitleEdgeColor = edgeCol;
                });
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void showThemeDialog() {
        dismiss();
        ThemeSelectionBottomSheet dialog = new ThemeSelectionBottomSheet(getContext(), currentTheme, theme -> {
            currentTheme = theme;
            if (onThemeChangedListener != null) onThemeChangedListener.onThemeChanged(theme);
        });
        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private String getResizeModeName(int mode) {
        switch (mode) {
            case 3: return "По размеру экрана"; // ZOOM
            case 4: return "Растянуть"; // FILL
            default: return "Вписать (Оригинал)"; // FIT
        }
    }
}
