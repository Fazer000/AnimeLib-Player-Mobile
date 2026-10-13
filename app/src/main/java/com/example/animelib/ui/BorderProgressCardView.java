package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;

public class BorderProgressCardView extends MaterialCardView {

    private int progress = 0; // 0 to 100
    private int baseStrokeColor = 0;
    private int progressStrokeColor = 0;
    private float strokeWidthPx = 4f;

    private final Paint baseStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path borderPath = new Path();
    private final Path progressSegmentPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();
    private final RectF rectF = new RectF();

    public BorderProgressCardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BorderProgressCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BorderProgressCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setStrokeWidth(0); // Disable MaterialCardView default stroke

        float density = context.getResources().getDisplayMetrics().density;
        strokeWidthPx = 2.0f * density;

        baseStrokePaint.setStyle(Paint.Style.STROKE);
        baseStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        progressStrokePaint.setStyle(Paint.Style.STROKE);
        progressStrokePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgressValues(int progress, int baseStrokeColor, int progressStrokeColor, float strokeWidthDp) {
        this.progress = Math.max(0, Math.min(100, progress));
        this.baseStrokeColor = baseStrokeColor;
        this.progressStrokeColor = progressStrokeColor;
        if (strokeWidthDp > 0) {
            float density = getContext().getResources().getDisplayMetrics().density;
            this.strokeWidthPx = strokeWidthDp * density;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() <= 0 || getHeight() <= 0) return;

        float halfStroke = strokeWidthPx / 2f;
        rectF.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);

        float cornerRadiusPx = getRadius();
        if (cornerRadiusPx <= 0) {
            cornerRadiusPx = 10f * getContext().getResources().getDisplayMetrics().density;
        }

        float radius = Math.min(cornerRadiusPx, Math.min(rectF.width(), rectF.height()) / 2f);
        float topCenter = (rectF.left + rectF.right) / 2f;

        borderPath.reset();
        borderPath.moveTo(topCenter, rectF.top);

        // Top edge right side
        borderPath.lineTo(rectF.right - radius, rectF.top);
        // Top-right corner arc
        borderPath.arcTo(rectF.right - 2 * radius, rectF.top, rectF.right, rectF.top + 2 * radius, 270, 90, false);
        // Right edge
        borderPath.lineTo(rectF.right, rectF.bottom - radius);
        // Bottom-right corner arc
        borderPath.arcTo(rectF.right - 2 * radius, rectF.bottom - 2 * radius, rectF.right, rectF.bottom, 0, 90, false);
        // Bottom edge
        borderPath.lineTo(rectF.left + radius, rectF.bottom);
        // Bottom-left corner arc
        borderPath.arcTo(rectF.left, rectF.bottom - 2 * radius, rectF.left + 2 * radius, rectF.bottom, 90, 90, false);
        // Left edge
        borderPath.lineTo(rectF.left, rectF.top + radius);
        // Top-left corner arc
        borderPath.arcTo(rectF.left, rectF.top, rectF.left + 2 * radius, rectF.top + 2 * radius, 180, 90, false);
        // Top edge left side back to topCenter
        borderPath.lineTo(topCenter, rectF.top);

        // 1. Draw base stroke
        if (baseStrokeColor != 0) {
            baseStrokePaint.setColor(baseStrokeColor);
            baseStrokePaint.setStrokeWidth(strokeWidthPx);
            canvas.drawPath(borderPath, baseStrokePaint);
        }

        // 2. Draw progress stroke
        if (progress > 0 && progressStrokeColor != 0) {
            pathMeasure.setPath(borderPath, false);
            float totalLength = pathMeasure.getLength();
            float drawLength = totalLength * (progress / 100f);

            progressSegmentPath.reset();
            pathMeasure.getSegment(0f, drawLength, progressSegmentPath, true);

            progressStrokePaint.setColor(progressStrokeColor);
            progressStrokePaint.setStrokeWidth(strokeWidthPx);
            canvas.drawPath(progressSegmentPath, progressStrokePaint);
        }
    }
}
