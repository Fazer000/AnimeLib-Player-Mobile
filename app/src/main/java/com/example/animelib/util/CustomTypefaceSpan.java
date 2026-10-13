package com.example.animelib.util;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

/**
 * Custom TypefaceSpan that applies an explicit Typeface object to TextPaint.
 */
public class CustomTypefaceSpan extends MetricAffectingSpan {
    private final Typeface typeface;

    public CustomTypefaceSpan(@NonNull Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        apply(ds, typeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        apply(paint, typeface);
    }

    private static void apply(Paint paint, Typeface tf) {
        if (tf == null) return;
        int oldStyle;
        Typeface old = paint.getTypeface();
        oldStyle = (old == null) ? 0 : old.getStyle();
        int want = oldStyle | tf.getStyle();
        Typeface fake = Typeface.create(tf, want);
        if ((fake.getStyle() & ~tf.getStyle()) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((fake.getStyle() & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        paint.setTypeface(fake);
    }
}
