package com.example.animelib.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.animelib.R;
import com.example.animelib.UpdateActivity;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.settings.ResizeModeBottomSheet;
import com.example.animelib.settings.SkipDurationBottomSheet;
import com.example.animelib.settings.SubtitlesBottomSheet;
import com.example.animelib.settings.SurroundSoundBottomSheet;
import com.example.animelib.settings.ThemeSelectionBottomSheet;
import com.example.animelib.settings.VideoFiltersBottomSheet;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.ThemeUtils;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private DatabaseManager databaseManager;

    private View settingResizeModeCard, settingSubtitlesCard, settingAmbientLightCard, settingAutoPlayCard;
    private View settingSurroundSoundCard, settingVideoFiltersCard, settingSkipDurationCard, settingTestModeCard;
    private View settingThemeCard, settingLanguageCard, settingUpdateCard;

    private TextView tvCurrentResizeMode, tvCurrentSurroundSound, tvCurrentVideoFilters, tvCurrentSkipDuration;
    private TextView tvCurrentTheme, tvCurrentLanguage;

    private MaterialSwitch switchSubtitles, switchAmbientLight, switchAutoPlay, switchSurroundSound, switchTestMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Apply status bar top insets and nav bar bottom insets to settings view
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            updateWindowInsets(statusBarInsets.top, navBarInsets.bottom);
            return insets;
        });

        databaseManager = DatabaseManager.getInstance(requireContext());

        // Find views
        settingResizeModeCard = view.findViewById(R.id.settingResizeModeCard);
        settingSubtitlesCard = view.findViewById(R.id.settingSubtitlesCard);
        settingAmbientLightCard = view.findViewById(R.id.settingAmbientLightCard);
        settingAutoPlayCard = view.findViewById(R.id.settingAutoPlayCard);
        settingSurroundSoundCard = view.findViewById(R.id.settingSurroundSoundCard);
        settingVideoFiltersCard = view.findViewById(R.id.settingVideoFiltersCard);
        settingSkipDurationCard = view.findViewById(R.id.settingSkipDurationCard);
        settingTestModeCard = view.findViewById(R.id.settingTestModeCard);
        settingThemeCard = view.findViewById(R.id.settingThemeCard);
        settingLanguageCard = view.findViewById(R.id.settingLanguageCard);
        settingUpdateCard = view.findViewById(R.id.settingUpdateCard);

        tvCurrentResizeMode = view.findViewById(R.id.tvCurrentResizeMode);
        tvCurrentSurroundSound = view.findViewById(R.id.tvCurrentSurroundSound);
        tvCurrentVideoFilters = view.findViewById(R.id.tvCurrentVideoFilters);
        tvCurrentSkipDuration = view.findViewById(R.id.tvCurrentSkipDuration);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);

        switchSubtitles = view.findViewById(R.id.switchSubtitles);
        switchAmbientLight = view.findViewById(R.id.switchAmbientLight);
        switchAutoPlay = view.findViewById(R.id.switchAutoPlay);
        switchSurroundSound = view.findViewById(R.id.switchSurroundSound);
        switchTestMode = view.findViewById(R.id.switchTestMode);

        setupListeners();
        refreshUi();
    }

    public void updateWindowInsets(int topInset, int bottomInset) {
        if (getView() == null) return;
        float density = getResources().getDisplayMetrics().density;
        int defaultHorizontal = (int) (16 * density);
        int defaultTop = topInset + (int) (16 * density);
        int defaultBottom = bottomInset + (int) (120 * density);
        getView().setPadding(defaultHorizontal, defaultTop, defaultHorizontal, defaultBottom);
        if (getView() instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) getView()).setClipToPadding(false);
            ((androidx.core.widget.NestedScrollView) getView()).setClipChildren(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    public void refreshUi() {
        if (getContext() == null || databaseManager == null) return;

        // Resize Mode
        if (tvCurrentResizeMode != null) {
            int mode = databaseManager.loadResizeModeSetting();
            tvCurrentResizeMode.setText(getResizeModeName(mode));
        }

        // Subtitles
        if (switchSubtitles != null) {
            switchSubtitles.setChecked(databaseManager.loadSubtitlesEnabledSetting());
        }

        // Ambient Light
        if (switchAmbientLight != null) {
            switchAmbientLight.setChecked(databaseManager.loadAmbientLightSetting());
        }

        // AutoPlay
        if (switchAutoPlay != null) {
            switchAutoPlay.setChecked(databaseManager.loadAutoPlaySetting());
        }

        // Surround Sound
        if (switchSurroundSound != null) {
            switchSurroundSound.setChecked(databaseManager.loadSurroundSoundSetting());
        }
        if (tvCurrentSurroundSound != null) {
            int mode = databaseManager.loadSurround3DMode();
            tvCurrentSurroundSound.setText(getSurroundModeName(mode));
        }

        // Video Filters
        if (tvCurrentVideoFilters != null) {
            float[] filters = databaseManager.loadVideoFilters();
            boolean isDef = (filters[0] == 0f && filters[1] == 100f && filters[2] == 100f && filters[3] == 1.0f && filters[4] == 0f);
            tvCurrentVideoFilters.setText(isDef ? "По умолчанию" : "Настроено");
        }

        // Skip Duration
        if (tvCurrentSkipDuration != null) {
            int duration = databaseManager.loadLongSkipDurationSetting();
            int min = duration / 60;
            int sec = duration % 60;
            tvCurrentSkipDuration.setText(String.format(Locale.US, "%d сек (%d:%02d)", duration, min, sec));
        }

        // Test Mode
        if (switchTestMode != null) {
            switchTestMode.setChecked(databaseManager.loadTestModeSetting());
        }

        // Theme
        if (tvCurrentTheme != null) {
            int theme = ThemeUtils.getSavedThemePreference(requireContext());
            tvCurrentTheme.setText(ThemeUtils.getThemeName(theme));
        }
    }

    private void setupListeners() {
        // Resize Mode
        if (settingResizeModeCard != null) {
            settingResizeModeCard.setOnClickListener(v -> {
                ResizeModeBottomSheet dialog = new ResizeModeBottomSheet(
                        requireContext(),
                        databaseManager.loadResizeModeSetting(),
                        mode -> {
                            databaseManager.saveResizeModeSetting(mode);
                            if (tvCurrentResizeMode != null) tvCurrentResizeMode.setText(getResizeModeName(mode));
                        }
                );
                dialog.show();
            });
        }

        // Subtitles
        if (settingSubtitlesCard != null) {
            settingSubtitlesCard.setOnClickListener(v -> {
                SubtitlesBottomSheet dialog = new SubtitlesBottomSheet(
                        requireContext(),
                        databaseManager.loadSubtitlesEnabledSetting(),
                        "auto",
                        null,
                        enabled -> {
                            databaseManager.saveSubtitlesEnabledSetting(enabled);
                            if (switchSubtitles != null) switchSubtitles.setChecked(enabled);
                        },
                        format -> {}
                );
                dialog.show();
            });
        }

        if (switchSubtitles != null) {
            switchSubtitles.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveSubtitlesEnabledSetting(isChecked);
            });
        }

        // Ambient Light
        if (switchAmbientLight != null) {
            switchAmbientLight.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveAmbientLightSetting(isChecked);
            });
        }

        // AutoPlay
        if (switchAutoPlay != null) {
            switchAutoPlay.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveAutoPlaySetting(isChecked);
            });
        }

        // Surround Sound
        if (settingSurroundSoundCard != null) {
            settingSurroundSoundCard.setOnClickListener(v -> {
                SurroundSoundBottomSheet dialog = new SurroundSoundBottomSheet(
                        requireContext(),
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
            switchSurroundSound.setOnCheckedChangeListener((btn, isChecked) -> {
                databaseManager.saveSurroundSoundSetting(isChecked);
            });
        }

        // Video Filters
        if (settingVideoFiltersCard != null) {
            settingVideoFiltersCard.setOnClickListener(v -> {
                float[] filters = databaseManager.loadVideoFilters();
                VideoFiltersBottomSheet dialog = new VideoFiltersBottomSheet(
                        requireContext(),
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
        if (settingSkipDurationCard != null) {
            settingSkipDurationCard.setOnClickListener(v -> {
                SkipDurationBottomSheet dialog = new SkipDurationBottomSheet(
                        requireContext(),
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
        if (switchTestMode != null) {
            switchTestMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                databaseManager.saveTestModeSetting(isChecked);
            });
        }

        // Theme
        if (settingThemeCard != null) {
            settingThemeCard.setOnClickListener(v -> {
                int currentTheme = ThemeUtils.getSavedThemePreference(requireContext());
                ThemeSelectionBottomSheet dialog = new ThemeSelectionBottomSheet(
                        requireContext(),
                        currentTheme,
                        selectedTheme -> {
                            ThemeUtils.applyThemeToActivity(requireActivity(), selectedTheme);
                            refreshUi();
                        }
                );
                dialog.show();
            });
        }

        // Language
        if (settingLanguageCard != null) {
            settingLanguageCard.setOnClickListener(v -> {
                CustomToast.showInfo(requireContext(), "Текущий язык: Русский");
            });
        }

        // Updates
        if (settingUpdateCard != null) {
            settingUpdateCard.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UpdateActivity.class);
                startActivity(intent);
            });
        }
    }

    private String getResizeModeName(int mode) {
        switch (mode) {
            case 0: return "Заполнение (Fit)";
            case 1: return "Кадрирование (Crop)";
            case 2: return "Растянуть (Stretch)";
            default: return "По умолчанию";
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
}
