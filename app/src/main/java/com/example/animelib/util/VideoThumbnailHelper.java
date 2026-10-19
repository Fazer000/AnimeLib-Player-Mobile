package com.example.animelib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;

import java.io.File;
import java.util.HashMap;

/**
 * Высокопроизводительный хелпер для извлечения превью видеофайлов.
 * Предотвращает отображение черных кадров (fade-in/черный интро) с помощью
 * адаптивного анализа ключевых кадров и встроенного LruCache.
 */
public class VideoThumbnailHelper {

    private static final int MEMORY_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8); // 1/8 памяти приложения
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(
            Math.max(1024 * 10, MEMORY_CACHE_SIZE)) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap != null ? (bitmap.getByteCount() / 1024) : 1;
        }
    };

    public static Bitmap getCachedThumbnail(String key) {
        if (key == null) return null;
        return MEMORY_CACHE.get(key);
    }

    public static void putCachedThumbnail(String key, Bitmap bitmap) {
        if (key != null && bitmap != null && !bitmap.isRecycled()) {
            MEMORY_CACHE.put(key, bitmap);
        }
    }

    public static void clearCache() {
        MEMORY_CACHE.evictAll();
    }

    public static Bitmap createFrameThumbnail(Context context, String path, Uri uri, long durationMs) {
        // Нормализация параметров URI и пути
        if (uri == null && path != null && path.startsWith("content://")) {
            try {
                uri = Uri.parse(path);
            } catch (Throwable ignored) {}
        }
        if (path == null && uri != null && "file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        String cacheKey = (uri != null) ? uri.toString() : path;
        if (cacheKey != null) {
            Bitmap cached = MEMORY_CACHE.get(cacheKey);
            if (cached != null && !cached.isRecycled()) {
                return cached;
            }
        }

        Bitmap thumb = null;

        // 1. Попытка получить системный мини-кэш MediaStore/ContentResolver (самый быстрый путь ~1-5мс)
        if (context != null && uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                thumb = context.getContentResolver().loadThumbnail(uri, new Size(320, 180), null);
            } catch (Throwable ignored) {}
        }

        if (thumb == null && path != null && new File(path).exists()) {
            try {
                thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
            } catch (Throwable ignored) {}
        }

        // Если системное превью получено, проверяем: не черный ли это начальный кадр (t=0)
        if (thumb != null) {
            if (!isMostlyBlack(thumb)) {
                if (cacheKey != null) {
                    putCachedThumbnail(cacheKey, thumb);
                }
                return thumb;
            } else {
                // Первый кадр из системы оказался черным — сбрасываем и переходим к умному поиску кадра
                try {
                    thumb.recycle();
                } catch (Throwable ignored) {}
                thumb = null;
            }
        }

        // 2. Умное извлечение кадра через MediaMetadataRetriever (с аппаратным масштабированием и пропуском черных кадров)
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (uri != null && context != null) {
                retriever.setDataSource(context, uri);
            } else if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                retriever.setDataSource(path, new HashMap<>());
            } else if (path != null && new File(path).exists()) {
                retriever.setDataSource(path);
            } else if (path != null) {
                retriever.setDataSource(path);
            } else {
                return null;
            }

            // Определение длительности видео, если не передана
            if (durationMs <= 0) {
                String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) {
                    try {
                        durationMs = Long.parseLong(durStr);
                    } catch (Throwable ignored) {}
                }
            }

            // Набор временных меток в микросекундах для быстрого поиска первого НЕ черного кадра
            long[] candidateTimesUs = getCandidateTimesUs(durationMs);

            Bitmap fallbackCandidate = null;
            for (long timeUs : candidateTimesUs) {
                Bitmap candidate = extractScaledFrame(retriever, timeUs);
                if (candidate != null) {
                    if (!isMostlyBlack(candidate)) {
                        // Найден информативный, не черный кадр! Завершаем поиск сразу
                        thumb = candidate;
                        break;
                    } else {
                        // Сохраняем как крайний вариант на случай, если видео целиком темное
                        if (fallbackCandidate == null) {
                            fallbackCandidate = candidate;
                        } else {
                            try { candidate.recycle(); } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            if (thumb == null) {
                thumb = fallbackCandidate;
            } else if (fallbackCandidate != null && fallbackCandidate != thumb) {
                try { fallbackCandidate.recycle(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }

        if (thumb != null && cacheKey != null) {
            putCachedThumbnail(cacheKey, thumb);
        }

        return thumb;
    }

    /**
     * Быстрое аппаратное извлечение кадра нужного размера (320x180) по ближайшему I-frame (OPTION_CLOSEST_SYNC),
     * что в десятки раз быстрее и не выделяет тяжелых 4K/1080p битмапов.
     */
    private static Bitmap extractScaledFrame(MediaMetadataRetriever retriever, long timeUs) {
        Bitmap bmp = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                bmp = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180);
            } catch (Throwable ignored) {}
        }
        if (bmp == null) {
            try {
                bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp != null && (bmp.getWidth() > 360 || bmp.getHeight() > 240)) {
                    Bitmap scaled = ThumbnailUtils.extractThumbnail(bmp, 320, 180, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                    if (scaled != null) {
                        bmp = scaled;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return bmp;
    }

    /**
     * Вычисление оптимальных меток времени (в микросекундах) для нахождения яркого содержательного кадра.
     */
    private static long[] getCandidateTimesUs(long durationMs) {
        if (durationMs > 30_000) { // Длиннее 30 сек
            return new long[] {
                    Math.max(5_000_000L, Math.min(25_000_000L, (long) (durationMs * 1000L * 0.10f))), // 10% или 5-25 сек (после интро)
                    Math.max(12_000_000L, Math.min(60_000_000L, (long) (durationMs * 1000L * 0.25f))), // 25%
                    (long) (durationMs * 1000L * 0.50f), // 50% середина
                    (long) (durationMs * 1000L * 0.70f), // 70%
                    2_000_000L // 2 сек
            };
        } else if (durationMs > 5_000) { // 5 - 30 сек
            return new long[] {
                    (long) (durationMs * 1000L * 0.30f), // 30%
                    (long) (durationMs * 1000L * 0.60f), // 60%
                    (long) (durationMs * 1000L * 0.15f), // 15%
                    1_000_000L // 1 сек
            };
        } else if (durationMs > 0) { // короткий клип < 5 сек
            return new long[] {
                    (long) (durationMs * 1000L * 0.50f),
                    (long) (durationMs * 1000L * 0.25f),
                    (long) (durationMs * 1000L * 0.75f)
            };
        } else { // Длительность неизвестна
            return new long[] {
                    8_000_000L,  // 8 сек
                    20_000_000L, // 20 сек
                    40_000_000L, // 40 сек
                    2_000_000L   // 2 сек
            };
        }
    }

    /**
     * Высокоскоростной анализ на темный / черный кадр.
     * Замеряет 49 точек во внутренней 70% области кадра (исключая черные полосы леттербоксинга фильмов 21:9).
     * Выполняется за микросекунды без дополнительных аллокаций.
     */
    public static boolean isMostlyBlack(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return true;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) return true;

        int startX = (int) (width * 0.15f);
        int endX = (int) (width * 0.85f);
        int startY = (int) (height * 0.15f);
        int endY = (int) (height * 0.85f);

        int stepX = Math.max(1, (endX - startX) / 7);
        int stepY = Math.max(1, (endY - startY) / 7);

        int nonBlackCount = 0;
        int totalLuminance = 0;
        int sampleCount = 0;

        for (int x = startX; x < endX; x += stepX) {
            for (int y = startY; y < endY; y += stepY) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                int luminance = (r * 299 + g * 587 + b * 114) / 1000;
                totalLuminance += luminance;
                sampleCount++;
                if (luminance >= 25) {
                    nonBlackCount++;
                }
            }
        }

        if (sampleCount == 0) return true;
        // Если хотя бы 6 точек имеют видимую яркость >= 25, в кадре есть изображение
        if (nonBlackCount >= 6) return false;
        int avgLuminance = totalLuminance / sampleCount;
        return avgLuminance < 18;
    }
}
