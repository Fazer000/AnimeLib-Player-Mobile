package com.example.animelib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static final String THEME_PREFERENCE = "theme_preference";
    public static final String THEME_MODE = "theme_mode";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    public static final String ACCENT_PREFERENCE = "accent_color_preference";
    public static final String ACCENT_PURPLE = "purple";
    public static final String ACCENT_CYAN = "cyan";
    public static final String ACCENT_COBALT = "cobalt";
    public static final String ACCENT_EMERALD = "emerald";
    public static final String ACCENT_CRIMSON = "crimson";
    public static final String ACCENT_AMBER = "amber";
    public static final String ACCENT_INDIGO = "indigo";
    public static final String ACCENT_SAKURA = "sakura";

    public static void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static String getThemeName(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return "Светлая";
            case THEME_DARK:
                return "Темная";
            case THEME_SYSTEM:
            default:
                return "Системная";
        }
    }

    public static void applyThemeToActivity(Activity activity, int themeMode) {
        saveThemePreference(activity, themeMode);
        applyTheme(themeMode);
        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.AppCompatDelegate delegate = 
                ((androidx.appcompat.app.AppCompatActivity) activity).getDelegate();
            int targetMode = getAppCompatNightMode(themeMode);
            if (delegate.getLocalNightMode() != targetMode) {
                delegate.setLocalNightMode(targetMode);
                delegate.applyDayNight();
            }
        }
    }

    private static int getAppCompatNightMode(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static void initTheme(Context context) {
        if (context == null) return;
        int themeMode = getSavedThemePreference(context);
        applyTheme(themeMode);
    }

    public static boolean isDarkTheme(Context context) {
        if (context == null) return false;
        int savedMode = getSavedThemePreference(context);
        if (savedMode == THEME_DARK) {
            return true;
        } else if (savedMode == THEME_LIGHT) {
            return false;
        }
        int nightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getSiteHeaderColor(Context context, String url) {
        if (context == null) return 0xFFEDE7F6;
        if (isDarkTheme(context)) {
            return androidx.core.content.ContextCompat.getColor(context, com.example.animelib.R.color.dt_header_color);
        }
        return androidx.core.content.ContextCompat.getColor(context, com.example.animelib.R.color.lt_header_color);
    }

    public static void saveThemePreference(Context context, int themeMode) {
        if (context == null) return;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(THEME_MODE, themeMode).apply();

        try {
            com.example.animelib.data.DatabaseManager.getInstance(context).saveThemeSettingInternal(themeMode);
        } catch (Exception ignored) {}
    }

    public static int getSavedThemePreference(Context context) {
        if (context == null) return THEME_LIGHT;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.contains(THEME_MODE)) {
            try {
                int dbTheme = com.example.animelib.data.DatabaseManager.getInstance(context).loadThemeSettingFromDb();
                preferences.edit().putInt(THEME_MODE, dbTheme).apply();
                return dbTheme;
            } catch (Exception ignored) {}
        }
        return preferences.getInt(THEME_MODE, THEME_LIGHT);
    }

    // Accent Color Management
    public static String getSavedAccentColor(Context context) {
        if (context == null) return ACCENT_PURPLE;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.contains(ACCENT_PREFERENCE)) {
            try {
                String dbAccent = com.example.animelib.data.DatabaseManager.getInstance(context).loadAccentColorSettingFromDb();
                preferences.edit().putString(ACCENT_PREFERENCE, dbAccent).apply();
                return dbAccent;
            } catch (Exception ignored) {}
        }
        return preferences.getString(ACCENT_PREFERENCE, ACCENT_PURPLE);
    }

    public static void saveAccentColor(Context context, String accentKey) {
        if (context == null || accentKey == null) return;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(ACCENT_PREFERENCE, accentKey).apply();

        try {
            com.example.animelib.data.DatabaseManager.getInstance(context).saveAccentColorSetting(accentKey);
        } catch (Exception ignored) {}
    }

    public static int getAccentColorInt(Context context) {
        String key = getSavedAccentColor(context);
        return Color.parseColor(getAccentColorHex(key));
    }

    public static String getAccentColorHex(String accentKey) {
        if (accentKey == null) return "#8B5CF6";
        switch (accentKey) {
            case ACCENT_CYAN:
                return "#00E5FF";
            case ACCENT_COBALT:
                return "#3B82F6";
            case ACCENT_EMERALD:
                return "#10B981";
            case ACCENT_CRIMSON:
                return "#FF2E63";
            case ACCENT_AMBER:
                return "#F59E0B";
            case ACCENT_INDIGO:
                return "#6366F1";
            case ACCENT_SAKURA:
                return "#EC4899";
            case ACCENT_PURPLE:
            default:
                return "#8B5CF6";
        }
    }

    public static String getAccentName(String accentKey) {
        if (accentKey == null) return "Фиолетовый";
        switch (accentKey) {
            case ACCENT_CYAN:
                return "Кибер Неон";
            case ACCENT_COBALT:
                return "Кобальт";
            case ACCENT_EMERALD:
                return "Изумруд";
            case ACCENT_CRIMSON:
                return "Кино Рубин";
            case ACCENT_AMBER:
                return "Янтарь";
            case ACCENT_INDIGO:
                return "Индиго";
            case ACCENT_SAKURA:
                return "Сакура";
            case ACCENT_PURPLE:
            default:
                return "Фиолетовый";
        }
    }

    public static void applyAccentToSwitch(android.widget.CompoundButton switchView, Context context) {
        if (switchView == null || context == null) return;
        int accent = getAccentColorInt(context);
        int uncheckedTrack = androidx.core.content.ContextCompat.getColor(context, com.example.animelib.R.color.switch_track_unchecked);
        int uncheckedThumb = androidx.core.content.ContextCompat.getColor(context, com.example.animelib.R.color.switch_thumb_unchecked);

        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] { -android.R.attr.state_checked }
        };

        int[] thumbColors = new int[] { accent, uncheckedThumb };
        int[] trackColors = new int[] { (0x66 << 24) | (accent & 0x00FFFFFF), uncheckedTrack };

        if (switchView instanceof com.google.android.material.materialswitch.MaterialSwitch) {
            com.google.android.material.materialswitch.MaterialSwitch ms = (com.google.android.material.materialswitch.MaterialSwitch) switchView;
            ms.setThumbTintList(new android.content.res.ColorStateList(states, thumbColors));
            ms.setTrackTintList(new android.content.res.ColorStateList(states, trackColors));
            ms.setTrackDecorationTintList(new android.content.res.ColorStateList(states, thumbColors));
        } else {
            switchView.setButtonTintList(new android.content.res.ColorStateList(states, thumbColors));
        }
    }

    public static void applyAccentToButton(View button, Context context) {
        if (button == null || context == null) return;
        int accent = getAccentColorInt(context);
        android.content.res.ColorStateList tintList = android.content.res.ColorStateList.valueOf(accent);
        if (button instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) button).setBackgroundTintList(tintList);
        } else if (button instanceof android.widget.Button) {
            button.setBackgroundTintList(tintList);
        } else {
            button.setBackgroundTintList(tintList);
        }
    }

    public static void applyAccentToProgressBar(android.widget.ProgressBar pb, Context context) {
        if (pb == null || context == null) return;
        int accent = getAccentColorInt(context);
        android.content.res.ColorStateList tintList = android.content.res.ColorStateList.valueOf(accent);
        pb.setIndeterminateTintList(tintList);
        pb.setProgressTintList(tintList);
        pb.setSecondaryProgressTintList(android.content.res.ColorStateList.valueOf((0x33 << 24) | (accent & 0x00FFFFFF)));
    }
}