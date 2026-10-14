package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Overlay View для фоновой подсветки (Ambilight).
 * Создаёт мягкое угасание подсветки исключительно у внешних краев экрана в ландшафтном режиме,
 * и плавный размытый градиент снизу в вертикальном режиме.
 */
public class AmbientVignetteOverlayView extends View {

    private final Paint topEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint bottomEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint leftEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint rightEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint portraitBottomFadePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private int lastWidth = 0;
    private int lastHeight = 0;
    private boolean isPortraitMode = false;

    private static final int[] EDGE_FALLOFF_COLORS = new int[]{
            0x40000000, // 25% soft dark at extreme screen edge
            0x20000000, // 12% dark
            0x0A000000, // ~4% dark
            0x00000000  // Transparent towards center
    };

    private static final float[] EDGE_STOPS = new float[]{
            0.0f, 0.35f, 0.70f, 1.0f
    };

    public AmbientVignetteOverlayView(Context context) {
        this(context, null);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPortraitMode(boolean portraitMode) {
        this.isPortraitMode = portraitMode;
        if (lastWidth > 0 && lastHeight > 0) {
            rebuildShaders(lastWidth, lastHeight);
        }
        invalidate();
    }

    public void setVideoBounds(float left, float top, float right, float bottom) {
        // Safe no-op
    }

    public void clearCustomVideoBounds() {
        // Safe no-op
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;
            rebuildShaders(w, h);
        }
    }

    private void rebuildShaders(int w, int h) {
        if (w <= 0 || h <= 0) return;

        float edgeDepth = dpToPx(16);

        topEdgePaint.setShader(new LinearGradient(
                0, 0, 0, edgeDepth,
                EDGE_FALLOFF_COLORS, EDGE_STOPS, Shader.TileMode.CLAMP
        ));

        bottomEdgePaint.setShader(new LinearGradient(
                0, h, 0, h - edgeDepth,
                EDGE_FALLOFF_COLORS, EDGE_STOPS, Shader.TileMode.CLAMP
        ));

        leftEdgePaint.setShader(new LinearGradient(
                0, 0, edgeDepth, 0,
                EDGE_FALLOFF_COLORS, EDGE_STOPS, Shader.TileMode.CLAMP
        ));

        rightEdgePaint.setShader(new LinearGradient(
                w, 0, w - edgeDepth, 0,
                EDGE_FALLOFF_COLORS, EDGE_STOPS, Shader.TileMode.CLAMP
        ));

        boolean isDarkMode = (getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int startFadeColor = isDarkMode ? 0x00000000 : 0x00FFFFFF;
        int quarterFadeColor = isDarkMode ? 0x33000000 : 0x33FFFFFF;
        int midFadeColor = isDarkMode ? 0x88000000 : 0x88FFFFFF;
        int targetBgColor = isDarkMode ? 0xFF000000 : 0xFFFFFFFF;

        float fadeHeight = dpToPx(44);
        portraitBottomFadePaint.setShader(new LinearGradient(
                0, h - fadeHeight, 0, h,
                new int[]{startFadeColor, quarterFadeColor, midFadeColor, targetBgColor},
                new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                Shader.TileMode.CLAMP
        ));
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float edgeDepth = dpToPx(16);

        // Draw soft vignette gradients along the outer screen edges
        canvas.drawRect(0, 0, w, edgeDepth, topEdgePaint);
        canvas.drawRect(0, h - edgeDepth, w, h, bottomEdgePaint);
        canvas.drawRect(0, 0, edgeDepth, h, leftEdgePaint);
        canvas.drawRect(w - edgeDepth, 0, w, h, rightEdgePaint);

        if (isPortraitMode) {
            float fadeHeight = dpToPx(44);
            canvas.drawRect(0, h - fadeHeight, w, h, portraitBottomFadePaint);
        }
    }
}
