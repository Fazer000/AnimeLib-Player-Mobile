package com.example.animelib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import java.io.File;
import java.util.HashMap;

public class VideoThumbnailHelper {

    public static Bitmap createFrameThumbnail(Context context, String path, Uri uri, long durationMs) {
        Bitmap thumb = null;

        // 1. Try MediaMetadataRetriever at 10% or 10s-30s into the video
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                retriever.setDataSource(path, new HashMap<String, String>());
            } else if (path != null && new File(path).exists()) {
                retriever.setDataSource(path);
            } else if (uri != null && context != null) {
                retriever.setDataSource(context, uri);
            }

            // Extract duration if not provided
            if (durationMs <= 0) {
                String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) {
                    try {
                        durationMs = Long.parseLong(durStr);
                    } catch (Exception ignored) {}
                }
            }

            long targetTimeUs = calculateTargetTimeUs(durationMs, 0.10f);
            thumb = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            // If mostly black, try at 25% or 40% into the video
            if (thumb != null && isMostlyBlack(thumb)) {
                long secondaryTimeUs = calculateTargetTimeUs(durationMs, 0.25f);
                Bitmap secondTry = retriever.getFrameAtTime(secondaryTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (secondTry != null && !isMostlyBlack(secondTry)) {
                    thumb = secondTry;
                } else {
                    long tertiaryTimeUs = calculateTargetTimeUs(durationMs, 0.40f);
                    Bitmap thirdTry = retriever.getFrameAtTime(tertiaryTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (thirdTry != null) {
                        thumb = thirdTry;
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }

        // 2. Fallback to System ContentResolver loadThumbnail or ThumbnailUtils if needed
        if (thumb == null && context != null && uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                thumb = context.getContentResolver().loadThumbnail(uri, new Size(320, 180), null);
            } catch (Throwable ignored) {}
        }

        if (thumb == null && path != null && new File(path).exists()) {
            try {
                thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
            } catch (Throwable ignored) {}
        }

        return thumb;
    }

    private static long calculateTargetTimeUs(long durationMs, float percentage) {
        if (durationMs > 20000) {
            long calcUs = (long) (durationMs * 1000L * percentage);
            return Math.max(5_000_000L, Math.min(60_000_000L, calcUs)); // between 5s and 60s
        } else if (durationMs > 3000) {
            return (long) (durationMs * 1000L * percentage);
        } else {
            return 1_000_000L; // 1 second
        }
    }

    private static boolean isMostlyBlack(Bitmap bitmap) {
        if (bitmap == null) return true;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) return true;

        int totalLuminance = 0;
        int sampleCount = 0;
        int stepX = Math.max(1, width / 6);
        int stepY = Math.max(1, height / 6);

        for (int x = stepX / 2; x < width; x += stepX) {
            for (int y = stepY / 2; y < height; y += stepY) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                int luminance = (r * 299 + g * 587 + b * 114) / 1000;
                totalLuminance += luminance;
                sampleCount++;
            }
        }

        if (sampleCount == 0) return true;
        int avgLuminance = totalLuminance / sampleCount;
        return avgLuminance < 18; // Luminance threshold for black frame
    }
}
