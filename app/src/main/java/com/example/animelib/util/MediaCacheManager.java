package com.example.animelib.util;

import android.content.Context;
import android.util.Log;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/**
 * Менеджер общего кэша медиапотоков для предотвращения повторных сетевых запросов
 * при одновременном воспроизведении основного плеера и плеера подсветки (Ambilight).
 */
public class MediaCacheManager {
    private static final String TAG = "MediaCacheManager";
    private static final long MAX_CACHE_SIZE = 300 * 1024 * 1024L; // 300 МБ кэша
    private static SimpleCache simpleCache;

    public static synchronized SimpleCache getCache(Context context) {
        if (simpleCache == null) {
            try {
                Context appContext = context.getApplicationContext();
                File cacheDir = new File(appContext.getCacheDir(), "media_player_stream_cache");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE);
                StandaloneDatabaseProvider databaseProvider = new StandaloneDatabaseProvider(appContext);
                simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);
                Log.d(TAG, "Media stream cache initialized at " + cacheDir.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize MediaCacheManager", e);
            }
        }
        return simpleCache;
    }

    public static DataSource.Factory createCacheDataSourceFactory(Context context, DataSource.Factory upstreamFactory) {
        SimpleCache cache = getCache(context);
        DataSource.Factory defaultFactory = new androidx.media3.datasource.DefaultDataSource.Factory(context, upstreamFactory);
        if (cache == null) {
            return defaultFactory;
        }
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(defaultFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    public static synchronized void releaseCache() {
        if (simpleCache != null) {
            try {
                simpleCache.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing SimpleCache", e);
            }
            simpleCache = null;
        }
    }
}
