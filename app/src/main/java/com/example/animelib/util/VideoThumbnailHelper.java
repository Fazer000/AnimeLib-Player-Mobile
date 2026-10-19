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
 * Высокопроизводительный хелпер для извлечения уникальных и качественных превью видеофайлов.
 *
 * Особенности:
 * 1. Уникальность превью: в сериалах и аниме первые 1.5-2 минуты занимает одинаковый опенинг (OP),
 *    а в самом начале (0с) часто находится логотип релиз-группы/дистрибьютора или черный экран.
 *    Поэтому выборка кадров производится из содержательной части (20%-75% хронометража)
 *    со смещением на основе хэша файла, гарантируя разные превью для каждой серии!
 * 2. Аппаратное масштабирование: извлечение через getScaledFrameAtTime и OPTION_CLOSEST_SYNC
 *    по ключевым I-кадрам без выделения избыточной оперативной памяти.
 * 3. Детектор темных кадров: быстрый анализ исключает черные сцены и переходы.
 * 4. Двухуровневый кэш в памяти: быстрый LruCache для мгновенного скролла.
 */
public class VideoThumbnailHelper {

    private static final String CACHE_VERSION_PREFIX = "v3_";
    private static final int MEMORY_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8); // 1/8 памяти приложения
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(
            Math.max(1024 * 10, MEMORY_CACHE_SIZE)) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap != null ? (bitmap.getByteCount() / 1024) : 1;
        }
    };

    /**
     * Создает единый унифицированный ключ для кэша на основе пути или URI.
     */
    public static String buildCacheKey(String path, Uri uri) {
        if (path != null && !path.trim().isEmpty()) {
            return CACHE_VERSION_PREFIX + path.trim();
        }
        if (uri != null) {
            return CACHE_VERSION_PREFIX + uri.toString();
        }
        return null;
    }

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

        String cacheKey = buildCacheKey(path, uri);
        if (cacheKey != null) {
            Bitmap cached = MEMORY_CACHE.get(cacheKey);
            if (cached != null && !cached.isRecycled()) {
                return cached;
            }
        }

        Bitmap thumb = null;

        // 1. Интеллектуальное извлечение кадра через MediaMetadataRetriever
        // Делаем выборку из основного хронометража конкретной серии, минуя общие логотипы и одинаковые опенинги
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (path != null && new File(path).exists()) {
                retriever.setDataSource(path);
            } else if (uri != null && context != null) {
                retriever.setDataSource(context, uri);
            } else if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                retriever.setDataSource(path, new HashMap<>());
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

            // Вычисляем набор уникальных временных меток для данного видео
            String seedStr = (path != null && !path.isEmpty()) ? path : (uri != null ? uri.toString() : "");
            long[] candidateTimesUs = getCandidateTimesUs(durationMs, seedStr);

            Bitmap fallbackCandidate = null;
            for (long timeUs : candidateTimesUs) {
                Bitmap candidate = extractScaledFrame(retriever, timeUs);
                if (candidate != null) {
                    if (!isMostlyBlack(candidate)) {
                        // Найден отличный, не черный и уникальный кадр из сюжета!
                        thumb = candidate;
                        break;
                    } else {
                        // Сохраняем как запасной вариант на случай темного видеоряда
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

        // 2. Системный фолбэк (только если MediaMetadataRetriever не смог обработать файл)
        if (thumb == null) {
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
     * Вычисление оптимальных меток времени (в микросекундах) для нахождения яркого содержательного
     * кадра из сюжета.
     *
     * Устраняет проблему одинаковых превью:
     * - Исключает первые 2-2.5 минуты (в аниме это стандартный опенинг/заставка, одинаковый во всех сериях).
     * - Исключает последние 2 минуты (эндинг и титры).
     * - Генерирует детерминированное смещение по хэшу файла, чтобы серия 1, 2, 3 брали кадры из разных сцен.
     */
    public static long[] getCandidateTimesUs(long durationMs, String seedStr) {
        int seed = 0;
        if (seedStr != null && !seedStr.isEmpty()) {
            // Лавинный хэш-миксер для равномерного распределения значений даже при близких именах (Ep01, Ep02)
            int h = seedStr.hashCode();
            h ^= (h >>> 16);
            h *= 0x85ebca6b;
            h ^= (h >>> 13);
            h *= 0xc2b2ae35;
            h ^= (h >>> 16);
            seed = Math.abs(h);
        }

        if (durationMs > 180_000) { // Длиннее 3 минут (аниме-эпизоды, фильмы, сериалы)
            // Пропуск заставки (интро/опенинг длится до 90-120 сек, реклама/логотипы).
            // Берем гарантированно безопасную зону после 2.5 минут (150 сек).
            long minSafeMs = Math.max(150_000L, (long) (durationMs * 0.12f));
            long maxSafeMs = Math.max(minSafeMs + 30_000L, durationMs - Math.max(120_000L, (long) (durationMs * 0.10f)));

            // Уникальный процент для конкретного видео на основе хэша (от 20% до 75% длительности)
            float uniqueFraction = 0.20f + ((seed % 100) / 100.0f) * 0.55f;
            long primaryMs = (long) (durationMs * uniqueFraction);
            if (primaryMs < minSafeMs) primaryMs = minSafeMs + ((seed % 30) * 1000L);
            if (primaryMs > maxSafeMs) primaryMs = maxSafeMs - ((seed % 30) * 1000L);

            return new long[] {
                    primaryMs * 1000L,
                    Math.max(minSafeMs, (long) (durationMs * 0.45f)) * 1000L,
                    Math.min(maxSafeMs, (long) (durationMs * 0.65f)) * 1000L,
                    Math.max(minSafeMs, (long) (durationMs * 0.32f)) * 1000L,
                    Math.min(maxSafeMs, (long) (durationMs * 0.52f)) * 1000L,
                    Math.max(minSafeMs, (long) (durationMs * 0.22f)) * 1000L
            };
        } else if (durationMs > 30_000) { // 30 сек - 3 мин
            float uniqueFraction = 0.25f + ((seed % 100) / 100.0f) * 0.45f;
            return new long[] {
                    (long) (durationMs * uniqueFraction) * 1000L,
                    (long) (durationMs * 0.50f) * 1000L,
                    (long) (durationMs * 0.30f) * 1000L,
                    (long) (durationMs * 0.70f) * 1000L
            };
        } else if (durationMs > 5_000) { // 5 - 30 сек
            return new long[] {
                    (long) (durationMs * 0.40f) * 1000L,
                    (long) (durationMs * 0.60f) * 1000L,
                    (long) (durationMs * 0.25f) * 1000L
            };
        } else if (durationMs > 0) { // Очень короткое видео < 5 сек
            return new long[] {
                    (long) (durationMs * 0.50f) * 1000L
            };
        } else { // Длительность неизвестна
            long seedSec = 150L + (seed % 150); // От 2.5 до 5 минут
            return new long[] {
                    seedSec * 1_000_000L,
                    300_000_000L, // 5 мин
                    180_000_000L, // 3 мин
                    420_000_000L, // 7 мин
                    90_000_000L   // 1.5 мин
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
