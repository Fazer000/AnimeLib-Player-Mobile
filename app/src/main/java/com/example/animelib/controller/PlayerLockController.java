package com.example.animelib.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.animelib.util.CustomToast;

/**
 * Контроллер блокировки экрана во время воспроизведения видео (Lock Screen).
 */
public class PlayerLockController {

    public interface OnLockStateChangeListener {
        void onLockStateChanged(boolean isLocked);
    }

    private final Context context;
    private final FrameLayout layoutLockedOverlay;
    private final LinearLayout lockedStatusBadge;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isScreenLocked = false;
    private OnLockStateChangeListener lockStateChangeListener;

    public PlayerLockController(@NonNull Context context,
                                @Nullable FrameLayout layoutLockedOverlay,
                                @Nullable LinearLayout lockedStatusBadge) {
        this.context = context;
        this.layoutLockedOverlay = layoutLockedOverlay;
        this.lockedStatusBadge = lockedStatusBadge;

        setupLockOverlayGestures();
    }

    public void setOnLockStateChangeListener(OnLockStateChangeListener listener) {
        this.lockStateChangeListener = listener;
    }

    public boolean isLocked() {
        return isScreenLocked;
    }

    public void lock() {
        isScreenLocked = true;
        if (layoutLockedOverlay != null) {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
        }
        if (lockedStatusBadge != null) {
            lockedStatusBadge.setVisibility(View.VISIBLE);
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(() -> lockedStatusBadge.setVisibility(View.GONE), 2500);
        }
        CustomToast.showInfo(context, "Экран заблокирован");

        if (lockStateChangeListener != null) {
            lockStateChangeListener.onLockStateChanged(true);
        }
    }

    public void unlock() {
        isScreenLocked = false;
        if (layoutLockedOverlay != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
        }
        CustomToast.showSuccess(context, "Экран разблокирован");

        if (lockStateChangeListener != null) {
            lockStateChangeListener.onLockStateChanged(false);
        }
    }

    public void release() {
        handler.removeCallbacksAndMessages(null);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupLockOverlayGestures() {
        if (layoutLockedOverlay == null) return;

        View swipeUnlockPill = layoutLockedOverlay.findViewById(com.example.animelib.R.id.swipeUnlockPill);

        layoutLockedOverlay.setOnTouchListener(new View.OnTouchListener() {
            private float downY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getRawY();
                        if (lockedStatusBadge != null) {
                            lockedStatusBadge.setVisibility(View.VISIBLE);
                            handler.removeCallbacksAndMessages(null);
                            handler.postDelayed(() -> lockedStatusBadge.setVisibility(View.GONE), 2000);
                        }
                        if (swipeUnlockPill != null) {
                            swipeUnlockPill.animate().cancel();
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (swipeUnlockPill != null) {
                            float dy = downY - event.getRawY();
                            if (dy > 0) {
                                swipeUnlockPill.setTranslationY(-dy);
                            } else {
                                swipeUnlockPill.setTranslationY(0f);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float deltaY = downY - event.getRawY();
                        if (deltaY > 120) {
                            unlock();
                            if (swipeUnlockPill != null) {
                                swipeUnlockPill.setTranslationY(0f);
                            }
                        } else if (swipeUnlockPill != null) {
                            swipeUnlockPill.animate().translationY(0f).setDuration(220).start();
                        }
                        return true;
                }
                return true;
            }
        });
    }
}
