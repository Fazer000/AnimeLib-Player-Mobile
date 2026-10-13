package com.example.animelib.managers;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

public class VideoFiltersManager {
    private static final String TAG = "VideoFiltersManager";

    private final Context context;
    private final View targetView;

    // Filter properties
    private float brightness = 0f;    // -100 to +100 (default 0)
    private float contrast = 100f;    // 50 to 150 (default 100)
    private float saturation = 100f;  // 0 to 200 (default 100)
    private float gamma = 1.0f;       // 0.5 to 2.0 (default 1.0)
    private float hue = 0f;           // -180 to +180 (default 0)

    private final Paint filterPaint = new Paint();

    public VideoFiltersManager(Context context, View targetView) {
        this.context = context;
        this.targetView = targetView;
    }

    public void setFilters(float brightness, float contrast, float saturation, float gamma, float hue) {
        this.brightness = brightness;
        this.contrast = contrast;
        this.saturation = saturation;
        this.gamma = gamma;
        this.hue = hue;
        applyFilters();
    }

    public void applyFilters() {
        if (targetView == null) return;

        // Ensure root player view (and its controller UI) never holds a layer filter
        if (targetView instanceof androidx.media3.ui.PlayerView) {
            targetView.setLayerType(View.LAYER_TYPE_NONE, null);
        }

        View actualTarget = getActualTargetView();
        if (actualTarget == null) return;

        boolean isDefault = (brightness == 0f && contrast == 100f && saturation == 100f && gamma == 1.0f && hue == 0f);

        if (isDefault) {
            actualTarget.setLayerType(View.LAYER_TYPE_NONE, null);
            return;
        }

        // Combined ColorMatrix calculation
        ColorMatrix cm = new ColorMatrix();

        // 1. Saturation (scale 0.0 to 2.0)
        float satScale = saturation / 100f;
        ColorMatrix satCm = new ColorMatrix();
        satCm.setSaturation(satScale);
        cm.postConcat(satCm);

        // 2. Brightness, Contrast & Gamma Gain
        float contrastScale = (contrast / 100f) * (float) Math.pow(1.0f / Math.max(0.1f, gamma), 0.5);
        float brightnessOffset = (brightness / 100f) * 255f + (1f - contrastScale) * 128f;

        float[] cmArray = cm.getArray();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cmArray[i * 5 + j] *= contrastScale;
            }
            cmArray[i * 5 + 4] += brightnessOffset;
        }

        // 3. Hue rotation
        if (hue != 0f) {
            ColorMatrix hueCm = new ColorMatrix();
            adjustHue(hueCm, hue);
            cm.postConcat(hueCm);
        }

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
        filterPaint.setColorFilter(filter);

        actualTarget.setLayerType(View.LAYER_TYPE_HARDWARE, filterPaint);
    }

    private View getActualTargetView() {
        if (targetView == null) return null;
        if (targetView instanceof androidx.media3.ui.PlayerView) {
            androidx.media3.ui.PlayerView pv = (androidx.media3.ui.PlayerView) targetView;
            View contentFrame = pv.findViewById(androidx.media3.ui.R.id.exo_content_frame);
            if (contentFrame != null) {
                return contentFrame;
            }
        }
        return targetView;
    }

    private static void adjustHue(ColorMatrix cm, float value) {
        float rad = (value / 180f) * (float) Math.PI;
        if (rad == 0) return;

        float cosVal = (float) Math.cos(rad);
        float sinVal = (float) Math.sin(rad);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;

        float[] mat = new float[] {
            lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
            lumG + cosVal * (-lumG) + sinVal * (-lumG),
            lumB + cosVal * (-lumB) + sinVal * (1 - lumB),
            0, 0,

            lumR + cosVal * (-lumR) + sinVal * 0.143f,
            lumG + cosVal * (1 - lumG) + sinVal * 0.140f,
            lumB + cosVal * (-lumB) + sinVal * (-0.283f),
            0, 0,

            lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
            lumG + cosVal * (-lumG) + sinVal * lumG,
            lumB + cosVal * (1 - lumB) + sinVal * lumB,
            0, 0,

            0, 0, 0, 1, 0
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    public boolean isModified() {
        return (brightness != 0f || contrast != 100f || saturation != 100f || gamma != 1.0f || hue != 0f);
    }

    public float getBrightness() { return brightness; }
    public float getContrast() { return contrast; }
    public float getSaturation() { return saturation; }
    public float getGamma() { return gamma; }
    public float getHue() { return hue; }
}
