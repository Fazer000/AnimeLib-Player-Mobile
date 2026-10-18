package com.example.animelib.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Utility class for DP / PX display conversions.
 */
public class DisplayUtils {

    public static float dpToPx(Context context, float dp) {
        if (context == null) return dp;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    public static int dpToPxInt(Context context, float dp) {
        return Math.round(dpToPx(context, dp));
    }

    public static float pxToDp(Context context, float px) {
        if (context == null) return px;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
