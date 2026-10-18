package com.example.animelib.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.ui.DefaultTimeBar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom TimeBar that extends Media3 DefaultTimeBar to provide true rounded
 * corners (pill shape) for the played, buffered, and unplayed tracks,
 * as well as support for segmented playback timelines.
 */
public class RoundedTimeBar extends DefaultTimeBar {
    private static final String TAG = "RoundedTimeBar";

    public static class TimecodeData {
        private int from;
        private int to;
        private String name;

        public TimecodeData() {}
        public TimecodeData(int from, int to, String name) {
            this.from = from;
            this.to = to;
            this.name = name;
        }

        public int getFrom() { return from; }
        public void setFrom(int from) { this.from = from; }
        public int getTo() { return to; }
        public void setTo(int to) { this.to = to; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private static class Interval {
        long fromMs;
        long toMs;

        Interval(long fromMs, long toMs) {
            this.fromMs = fromMs;
            this.toMs = toMs;
        }
    }

    private Rect progressBar;
    private Rect bufferedBar;
    private Rect scrubberBar;
    private Paint playedPaint;
    private Paint bufferedPaint;
    private Paint unplayedPaint;
    private Paint adMarkerPaint;
    private Paint playedAdMarkerPaint;

    private final RectF rectF = new RectF();
    private float customCornerRadius = -1f;

    private final List<TimecodeData> timecodes = new ArrayList<>();
    private long durationMs = 0;

    public RoundedTimeBar(Context context) {
        this(context, null);
    }

    public RoundedTimeBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public RoundedTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs) {
        this(context, attrs, defStyleAttr, timebarAttrs, 0);
    }

    public RoundedTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttr, timebarAttrs, defStyleRes);
        init();
    }

    private void init() {
        try {
            Class<?> clazz = DefaultTimeBar.class;

            Field fProgressBar = clazz.getDeclaredField("progressBar");
            fProgressBar.setAccessible(true);
            progressBar = (Rect) fProgressBar.get(this);

            Field fBufferedBar = clazz.getDeclaredField("bufferedBar");
            fBufferedBar.setAccessible(true);
            bufferedBar = (Rect) fBufferedBar.get(this);

            Field fScrubberBar = clazz.getDeclaredField("scrubberBar");
            fScrubberBar.setAccessible(true);
            scrubberBar = (Rect) fScrubberBar.get(this);

            Field fPlayedPaint = clazz.getDeclaredField("playedPaint");
            fPlayedPaint.setAccessible(true);
            playedPaint = (Paint) fPlayedPaint.get(this);

            Field fBufferedPaint = clazz.getDeclaredField("bufferedPaint");
            fBufferedPaint.setAccessible(true);
            bufferedPaint = (Paint) fBufferedPaint.get(this);

            Field fUnplayedPaint = clazz.getDeclaredField("unplayedPaint");
            fUnplayedPaint.setAccessible(true);
            unplayedPaint = (Paint) fUnplayedPaint.get(this);

            Field fAdMarkerPaint = clazz.getDeclaredField("adMarkerPaint");
            fAdMarkerPaint.setAccessible(true);
            adMarkerPaint = (Paint) fAdMarkerPaint.get(this);

            Field fPlayedAdMarkerPaint = clazz.getDeclaredField("playedAdMarkerPaint");
            fPlayedAdMarkerPaint.setAccessible(true);
            playedAdMarkerPaint = (Paint) fPlayedAdMarkerPaint.get(this);

            // Ensure paints have anti-aliasing for smooth rounded corners
            if (playedPaint != null) playedPaint.setAntiAlias(true);
            if (bufferedPaint != null) bufferedPaint.setAntiAlias(true);
            if (unplayedPaint != null) unplayedPaint.setAntiAlias(true);

            // Fallback colors if defined as transparent in XML
            if (playedPaint != null && (playedPaint.getColor() == Color.TRANSPARENT || playedPaint.getColor() == 0)) {
                playedPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), com.example.animelib.R.color.purple_primary));
            }
            if (bufferedPaint != null && (bufferedPaint.getColor() == Color.TRANSPARENT || bufferedPaint.getColor() == 0)) {
                bufferedPaint.setColor(Color.parseColor("#80FFFFFF"));
            }
            if (unplayedPaint != null && (unplayedPaint.getColor() == Color.TRANSPARENT || unplayedPaint.getColor() == 0)) {
                unplayedPaint.setColor(Color.parseColor("#4DFFFFFF"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind DefaultTimeBar internal fields", e);
        }
    }

    public void setCornerRadius(float radius) {
        this.customCornerRadius = radius;
        invalidate();
    }

    public void setTimecodes(List<TimecodeData> timecodes, long durationMs) {
        this.timecodes.clear();
        if (timecodes != null) {
            this.timecodes.addAll(timecodes);
            Collections.sort(this.timecodes, (t1, t2) -> Integer.compare(t1.getFrom(), t2.getFrom()));
        }
        if (durationMs > 0) {
            this.durationMs = durationMs;
        }
        invalidate();
    }

    @Override
    public void setDuration(long duration) {
        super.setDuration(duration);
        this.durationMs = duration;
    }

    private List<Interval> buildIntervals() {
        List<Interval> intervals = new ArrayList<>();
        if (durationMs <= 0 || timecodes.isEmpty()) return intervals;

        long currentPointer = 0;
        for (TimecodeData tc : timecodes) {
            long from = Math.max(0, tc.getFrom() * 1000L);
            long to = Math.min(durationMs, tc.getTo() * 1000L);

            if (to <= from || from >= durationMs) continue;

            if (from > currentPointer) {
                intervals.add(new Interval(currentPointer, from));
            }
            intervals.add(new Interval(from, to));
            currentPointer = Math.max(currentPointer, to);
        }

        if (currentPointer < durationMs) {
            intervals.add(new Interval(currentPointer, durationMs));
        }

        return intervals;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (progressBar == null || progressBar.width() <= 0) {
            super.onDraw(canvas);
            return;
        }

        int height = progressBar.height();
        int centerY = progressBar.centerY();
        float top = centerY - height / 2f;
        float bottom = centerY + height / 2f;
        float cornerRadius = customCornerRadius >= 0 ? customCornerRadius : height / 2f;

        List<Interval> intervals = buildIntervals();

        if (intervals.isEmpty()) {
            // Standard continuous track with full rounded ends (pill)
            float barLeft = progressBar.left;
            float barRight = progressBar.right;

            // 1. Draw unplayed background track (full bar with rounded corners)
            if (unplayedPaint != null && unplayedPaint.getColor() != Color.TRANSPARENT) {
                rectF.set(barLeft, top, barRight, bottom);
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, unplayedPaint);
            }

            // 2. Draw buffered track (with rounded corners)
            if (bufferedBar != null && bufferedPaint != null && bufferedPaint.getColor() != Color.TRANSPARENT) {
                float buffRight = Math.min(bufferedBar.right, barRight);
                if (buffRight > barLeft) {
                    rectF.set(barLeft, top, buffRight, bottom);
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bufferedPaint);
                }
            }

            // 3. Draw played track (with rounded corners)
            if (scrubberBar != null && playedPaint != null && playedPaint.getColor() != Color.TRANSPARENT) {
                float playRight = Math.min(scrubberBar.right, barRight);
                if (playRight > barLeft) {
                    rectF.set(barLeft, top, playRight, bottom);
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, playedPaint);
                }
            }
        } else {
            // Segmented track
            float density = getResources().getDisplayMetrics().density;
            float gapWidth = 3f * density;
            float totalWidth = progressBar.width();
            float barLeft = progressBar.left;
            int count = intervals.size();

            float currentPlayX = (scrubberBar != null) ? scrubberBar.right : barLeft;
            float currentBuffX = (bufferedBar != null) ? bufferedBar.right : barLeft;

            for (int i = 0; i < count; i++) {
                Interval interval = intervals.get(i);
                if (interval.toMs <= interval.fromMs) continue;

                float rawStartX = barLeft + ((float) interval.fromMs / durationMs) * totalWidth;
                float rawEndX = barLeft + ((float) interval.toMs / durationMs) * totalWidth;

                float segLeft = (i == 0) ? rawStartX : (rawStartX + gapWidth / 2f);
                float segRight = (i == count - 1) ? rawEndX : (rawEndX - gapWidth / 2f);

                if (segRight <= segLeft) continue;

                // 1. Unplayed segment background with rounded corners
                if (unplayedPaint != null && unplayedPaint.getColor() != Color.TRANSPARENT) {
                    rectF.set(segLeft, top, segRight, bottom);
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, unplayedPaint);
                }

                // 2. Buffered portion inside segment with rounded corners
                if (bufferedPaint != null && currentBuffX > segLeft) {
                    float bRight = Math.min(segRight, currentBuffX);
                    if (bRight > segLeft) {
                        rectF.set(segLeft, top, bRight, bottom);
                        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bufferedPaint);
                    }
                }

                // 3. Played portion inside segment with rounded corners
                if (playedPaint != null && currentPlayX > segLeft) {
                    float pRight = Math.min(segRight, currentPlayX);
                    if (pRight > segLeft) {
                        rectF.set(segLeft, top, pRight, bottom);
                        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, playedPaint);
                    }
                }
            }
        }

        // Draw scrubber/playhead knob using super.onDraw by making track paints temporarily transparent
        int origUnplayed = unplayedPaint != null ? unplayedPaint.getColor() : 0;
        int origBuffered = bufferedPaint != null ? bufferedPaint.getColor() : 0;
        int origPlayed = playedPaint != null ? playedPaint.getColor() : 0;

        if (unplayedPaint != null) unplayedPaint.setColor(Color.TRANSPARENT);
        if (bufferedPaint != null) bufferedPaint.setColor(Color.TRANSPARENT);
        if (playedPaint != null) playedPaint.setColor(Color.TRANSPARENT);

        super.onDraw(canvas);

        if (unplayedPaint != null) unplayedPaint.setColor(origUnplayed);
        if (bufferedPaint != null) bufferedPaint.setColor(origBuffered);
        if (playedPaint != null) playedPaint.setColor(origPlayed);
    }
}
