package com.example.animelib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static final String THEME_PREFERENCE = "theme_preference";
    public static final String THEME_MODE = "theme_mode";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

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
    
    /**
     * Применяет тему к конкретной активности без перезагрузки
     * @param activity Активность для применения темы
     * @param themeMode Режим темы
     */
    public static void applyThemeToActivity(Activity activity, int themeMode) {
        // Сохраняем тему синхронно для будущих активностей
        saveThemePreference(activity, themeMode);
        
        // Обновляем глобальный системный режим по умолчанию для всего приложения
        applyTheme(themeMode);
        
        // Принудительно обновляем текущую активность БЕЗ пересоздания
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
    
    /**
     * Преобразует режим темы в AppCompat режим
     */
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

    /**
     * Возвращает цвет шапки/системного статус-бара для сайта.
     */
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
        return preferences.getInt(THEME_MODE, THEME_LIGHT); // по умолчанию светлая тема
    }
}