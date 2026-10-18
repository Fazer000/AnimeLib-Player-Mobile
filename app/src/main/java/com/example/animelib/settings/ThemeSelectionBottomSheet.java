package com.example.animelib.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.ThemeUtils;

public class ThemeSelectionBottomSheet extends FlexibleBottomSheetDialog {
    
    public interface OnThemeChangedListener {
        void onThemeChanged(int themeMode);
    }
    
    public interface OnBackPressedListener {
        void onBackPressed();
    }
    
    private OnThemeChangedListener listener;
    private OnBackPressedListener onBackPressedListener;
    private int currentTheme;
    
    private View themeAutoPill;
    private View themeAutoUnselected;
    private View themeLightPill;
    private View themeLightUnselected;
    private View themeDarkPill;
    private View themeDarkUnselected;
    
    public ThemeSelectionBottomSheet(@NonNull Context context, int currentTheme, OnThemeChangedListener listener) {
        super(context, com.example.animelib.R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.currentTheme = currentTheme;
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_theme_selection, null);
        setContentView(view);

        // Находим элементы
        ImageButton closeButton = view.findViewById(R.id.bs_theme_back);
        LinearLayout themeAutoOption = view.findViewById(R.id.themeAutoOption);
        LinearLayout themeLightOption = view.findViewById(R.id.themeLightOption);
        LinearLayout themeDarkOption = view.findViewById(R.id.themeDarkOption);
        
        themeAutoPill = view.findViewById(R.id.themeAutoPill);
        themeAutoUnselected = view.findViewById(R.id.themeAutoUnselected);
        themeLightPill = view.findViewById(R.id.themeLightPill);
        themeLightUnselected = view.findViewById(R.id.themeLightUnselected);
        themeDarkPill = view.findViewById(R.id.themeDarkPill);
        themeDarkUnselected = view.findViewById(R.id.themeDarkUnselected);
        
        // Accent color options
        View accentPurple = view.findViewById(R.id.accentPurpleOption);
        View accentCyan = view.findViewById(R.id.accentCyanOption);
        View accentCobalt = view.findViewById(R.id.accentCobaltOption);
        View accentEmerald = view.findViewById(R.id.accentEmeraldOption);
        View accentCrimson = view.findViewById(R.id.accentCrimsonOption);
        View accentAmber = view.findViewById(R.id.accentAmberOption);
        View accentIndigo = view.findViewById(R.id.accentIndigoOption);
        View accentSakura = view.findViewById(R.id.accentSakuraOption);

        // Accent color tags
        tagPurple = view.findViewById(R.id.tagPurpleCurrent);
        tagCyan = view.findViewById(R.id.tagCyanCurrent);
        tagCobalt = view.findViewById(R.id.tagCobaltCurrent);
        tagEmerald = view.findViewById(R.id.tagEmeraldCurrent);
        tagCrimson = view.findViewById(R.id.tagCrimsonCurrent);
        tagAmber = view.findViewById(R.id.tagAmberCurrent);
        tagIndigo = view.findViewById(R.id.tagIndigoCurrent);
        tagSakura = view.findViewById(R.id.tagSakuraCurrent);

        String savedAccent = ThemeUtils.getSavedAccentColor(getContext());
        updateAccentTags(savedAccent);

        if (accentPurple != null) accentPurple.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_PURPLE, "AnimeLIB Purple"));
        if (accentCyan != null) accentCyan.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_CYAN, "Cyber Cyan"));
        if (accentCobalt != null) accentCobalt.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_COBALT, "Electric Cobalt"));
        if (accentEmerald != null) accentEmerald.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_EMERALD, "Neon Emerald"));
        if (accentCrimson != null) accentCrimson.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_CRIMSON, "Cinema Crimson"));
        if (accentAmber != null) accentAmber.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_AMBER, "Sunset Amber"));
        if (accentIndigo != null) accentIndigo.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_INDIGO, "Cosmic Indigo"));
        if (accentSakura != null) accentSakura.setOnClickListener(v -> selectAccent(ThemeUtils.ACCENT_SAKURA, "Sakura Pink"));

        // Устанавливаем текущую тему
        updateThemeSelection();
        
        // Обработчики кликов
        closeButton.setOnClickListener(v -> {
            if (onBackPressedListener != null) {
                dismiss();
                onBackPressedListener.onBackPressed();
            } else {
                dismiss();
            }
        });
        
        themeAutoOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_SYSTEM));
        themeLightOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_LIGHT));
        themeDarkOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_DARK));
        
        // Закрытие при клике вне диалога
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private View tagPurple, tagCyan, tagCobalt, tagEmerald, tagCrimson, tagAmber, tagIndigo, tagSakura;

    private void updateAccentTags(String accentKey) {
        if (tagPurple != null) tagPurple.setVisibility(ThemeUtils.ACCENT_PURPLE.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagCyan != null) tagCyan.setVisibility(ThemeUtils.ACCENT_CYAN.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagCobalt != null) tagCobalt.setVisibility(ThemeUtils.ACCENT_COBALT.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagEmerald != null) tagEmerald.setVisibility(ThemeUtils.ACCENT_EMERALD.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagCrimson != null) tagCrimson.setVisibility(ThemeUtils.ACCENT_CRIMSON.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagAmber != null) tagAmber.setVisibility(ThemeUtils.ACCENT_AMBER.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagIndigo != null) tagIndigo.setVisibility(ThemeUtils.ACCENT_INDIGO.equals(accentKey) ? View.VISIBLE : View.GONE);
        if (tagSakura != null) tagSakura.setVisibility(ThemeUtils.ACCENT_SAKURA.equals(accentKey) ? View.VISIBLE : View.GONE);
    }

    private android.app.Activity getActivityFromContext(Context context) {
        if (context == null) return null;
        if (context instanceof android.app.Activity) {
            return (android.app.Activity) context;
        }
        if (context instanceof android.content.ContextWrapper) {
            return getActivityFromContext(((android.content.ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    private void selectAccent(String accentKey, String name) {
        ThemeUtils.saveAccentColor(getContext(), accentKey);
        updateAccentTags(accentKey);
        com.example.animelib.util.CustomToast.showSuccess(getContext(), "Выбран цвет: " + name);
        dismiss();
        android.app.Activity activity = getActivityFromContext(getContext());
        if (activity != null) {
            activity.recreate();
        }
    }
    
    private void selectTheme(int themeMode) {
        if (themeMode != currentTheme) {
            currentTheme = themeMode;
            updateThemeSelection();
            
            android.app.Activity activity = getActivityFromContext(getContext());
            if (activity != null) {
                ThemeUtils.applyThemeToActivity(activity, themeMode);
            } else {
                ThemeUtils.applyTheme(themeMode);
            }
            
            // Уведомляем слушателя
            if (listener != null) {
                listener.onThemeChanged(themeMode);
            }
        }
        dismiss();
    }
    
    private void updateThemeSelection() {
        boolean isAuto = currentTheme == ThemeUtils.THEME_SYSTEM;
        boolean isLight = currentTheme == ThemeUtils.THEME_LIGHT;
        boolean isDark = currentTheme == ThemeUtils.THEME_DARK;

        if (themeAutoPill != null) themeAutoPill.setVisibility(isAuto ? View.VISIBLE : View.GONE);
        if (themeAutoUnselected != null) themeAutoUnselected.setVisibility(isAuto ? View.GONE : View.VISIBLE);

        if (themeLightPill != null) themeLightPill.setVisibility(isLight ? View.VISIBLE : View.GONE);
        if (themeLightUnselected != null) themeLightUnselected.setVisibility(isLight ? View.GONE : View.VISIBLE);

        if (themeDarkPill != null) themeDarkPill.setVisibility(isDark ? View.VISIBLE : View.GONE);
        if (themeDarkUnselected != null) themeDarkUnselected.setVisibility(isDark ? View.GONE : View.VISIBLE);
    }
    
    public int getCurrentTheme() {
        return currentTheme;
    }
    
    public String getCurrentThemeText() {
        switch (currentTheme) {
            case ThemeUtils.THEME_SYSTEM:
                return "Авто";
            case ThemeUtils.THEME_LIGHT:
                return "Светлая";
            case ThemeUtils.THEME_DARK:
                return "Темная";
            default:
                return "Авто";
        }
    }
    
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }
    
    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null) {
            onBackPressedListener.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
