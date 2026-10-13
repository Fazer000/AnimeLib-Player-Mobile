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

        // Hide video server option (not applicable for local files)
        View videoServerOption = view.findViewById(R.id.videoServerOption);
        if (videoServerOption != null) videoServerOption.setVisibility(View.GONE);

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

        // Bug Report (GitHub)
        View bugReportOption = view.findViewById(R.id.bugReportOption);
        if (bugReportOption != null) {
            bugReportOption.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Fazer000/AnimeLib-Player-Mobile/issues"));
                    getContext().startActivity(intent);
                } catch (Exception e) {
                    CustomToast.showWarning(getContext(), "Не удалось открыть браузер");
                }
            });
        }

        // Check for Updates via GitHub
        TextView appVersionText = view.findViewById(R.id.appVersionText);
        if (appVersionText != null) appVersionText.setText("Версия 1.0.0 (Fazer000)");
        View checkForUpdatesOption = view.findViewById(R.id.checkForUpdatesOption);
        View updateProgressBar = view.findViewById(R.id.updateProgressBar);
        View ivUpdateArrow = view.findViewById(R.id.ivUpdateArrow);
        if (checkForUpdatesOption != null) {
            checkForUpdatesOption.setOnClickListener(v -> {
                if (updateProgressBar != null) updateProgressBar.setVisibility(View.VISIBLE);
                if (ivUpdateArrow != null) ivUpdateArrow.setVisibility(View.GONE);

                new Thread(() -> {
                    boolean hasUpdate = false;
                    String latestTag = "v1.0.0";
                    String releaseUrl = "https://github.com/Fazer000/AnimeLib-Player-Mobile/releases";
                    try {
                        java.net.URL url = new java.net.URL("https://api.github.com/repos/Fazer000/AnimeLib-Player-Mobile/releases/latest");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "AnimeLib-Player-Android");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);

                        if (conn.getResponseCode() == 200) {
                            java.io.InputStream is = conn.getInputStream();
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            reader.close();

                            org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                            if (json.has("tag_name")) {
                                latestTag = json.getString("tag_name");
                                if (json.has("html_url")) {
                                    releaseUrl = json.getString("html_url");
                                }
                                if (!"v1.0.0".equalsIgnoreCase(latestTag) && !"1.0.0".equalsIgnoreCase(latestTag)) {
                                    hasUpdate = true;
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    final boolean finalHasUpdate = hasUpdate;
                    final String finalLatestTag = latestTag;
                    final String finalReleaseUrl = releaseUrl;

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (updateProgressBar != null) updateProgressBar.setVisibility(View.GONE);
                        if (ivUpdateArrow != null) ivUpdateArrow.setVisibility(View.VISIBLE);

                        if (finalHasUpdate) {
                            CustomToast.showInfo(getContext(), "Доступно обновление: " + finalLatestTag);
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalReleaseUrl));
                                getContext().startActivity(intent);
                            } catch (Exception ignored) {}
                        } else {
                            CustomToast.showInfo(getContext(), "У вас актуальная версия!");
                        }
                    });
                }).start();
            });
        }

        setCancelable(true);
        setCanceledOnTouchOutside(true);
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
