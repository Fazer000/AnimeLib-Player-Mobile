package com.example.animelib.controller;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Контроллер системного интерфейса плеера (Immersive Fullscreen, Window Insets, StatusBar/NavigationBar).
 */
public class PlayerSystemUiController {

    private final Activity activity;
    private final WindowInsetsControllerCompat insetsController;

    public PlayerSystemUiController(@NonNull Activity activity) {
        this.activity = activity;
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        this.insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    /**
     * Скрывает системные панели (статус-бар и навигацию) для полноэкранного просмотра.
     */
    public void hideSystemUI() {
        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
        }

        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            activity.getWindow().setAttributes(lp);
        }
    }

    /**
     * Показывает системные панели.
     */
    public void showSystemUI() {
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }
}
