package com.example.animelib.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ItemAnimationUtils {

    /**
     * Анимирует нажатие на элемент списка (эпизоды, озвучки, качества, кнопки переключения):
     * четкий отклик прозрачности и легкая задержка вызова действия для завершения эффекта.
     */
    public static void animateItemClick(View view, Runnable action) {
        if (view == null) {
            if (action != null) action.run();
            return;
        }

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(0.3f);

        view.animate()
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (action != null) {
            view.postDelayed(action, 70);
        }
    }

    /**
     * Плавный переход элемента при выборе активного состояния (плавный вспышка-проявление без скейлинга).
     */
    public static void animateItemStateTransition(View view, boolean isActive) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        if (isActive) {
            view.setAlpha(0.35f);
            view.animate()
                    .alpha(1.0f)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            view.setAlpha(1.0f);
        }
    }

    /**
     * Плавное выделение выбранного айтема (без скейлинга).
     */
    public static void animateItemSelection(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        
        view.setAlpha(0.35f);
        view.animate()
                .alpha(1.0f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Анимирует смену текста/плашки при переключении эпизода или озвучки (без скейлинга)
     */
    public static void animateTextChange(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(0.2f);

        view.animate()
                .alpha(1.0f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}


