package com.example.animelib.data;

import android.content.Context;
import android.util.Log;

import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.models.VideoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер для операций с базой данных Room (Настройки плеера, история просмотров)
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    private final AppDatabase db;
    private final ExecutorService executor;
    private final Context context;

    private static volatile DatabaseManager instance;

    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getDatabase(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        instance = this;
    }

    // ========== AppSettings операции ==========

    public void saveAmbientLightSetting(boolean enableAmbientLight) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnableAmbientLight(enableAmbientLight);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save ambient light setting", e);
            }
        });
    }

    public boolean loadAmbientLightSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnableAmbientLight();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load ambient light setting", e);
            return true;
        }
    }

    public void saveVideoFilters(float brightness, float contrast, float saturation, float gamma, float hue) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) settings = new AppSettings();
                settings.setFilterBrightness(brightness);
                settings.setFilterContrast(contrast);
                settings.setFilterSaturation(saturation);
                settings.setFilterGamma(gamma);
                settings.setFilterHue(hue);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save video filters", e);
            }
        });
    }

    public float[] loadVideoFilters() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            if (settings != null) {
                return new float[]{
                        settings.getFilterBrightness(),
                        settings.getFilterContrast(),
                        settings.getFilterSaturation(),
                        settings.getFilterGamma(),
                        settings.getFilterHue()
                };
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load video filters", e);
        }
        return new float[]{0f, 100f, 100f, 1.0f, 0f};
    }

    public void saveSurroundSoundSetting(boolean enableSurroundSound) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnableSurroundSound(enableSurroundSound);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save surround sound setting", e);
            }
        });
    }

    public void saveSurround3DSettings(int mode, float spatialWidth, float dialogueBoost, float bassBoost, float trebleBoost) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setSurroundMode(mode);
                settings.setSurroundSpatialWidth(spatialWidth);
                settings.setSurroundDialogueBoost(dialogueBoost);
                settings.setSurroundBassBoost(bassBoost);
                settings.setSurroundTrebleBoost(trebleBoost);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save surround 3D settings", e);
            }
        });
    }

    public boolean loadSurroundSoundSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null || settings.isEnableSurroundSound();
        } catch (Exception e) {
            return true;
        }
    }

    public int loadSurround3DMode() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSurroundMode() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public float loadSurroundSpatialWidth() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSurroundSpatialWidth() : 1.0f;
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public float loadSurroundDialogueBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSurroundDialogueBoost() : 1.0f;
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public float loadSurroundBassBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSurroundBassBoost() : 1.0f;
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public float loadSurroundTrebleBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSurroundTrebleBoost() : 1.0f;
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public void saveAutoPlaySetting(boolean autoPlay) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setAutoPlay(autoPlay);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save autoPlay setting", e);
            }
        });
    }

    public boolean loadAutoPlaySetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null || settings.isAutoPlay();
        } catch (Exception e) {
            return true;
        }
    }

    public void saveLongSkipDurationSetting(int duration) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setLongSkipDuration(duration);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save longSkipDuration setting", e);
            }
        });
    }

    public int loadLongSkipDurationSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getLongSkipDuration() : 85;
        } catch (Exception e) {
            return 85;
        }
    }

    public void saveThemeSetting(int themeMode) {
        saveThemeSettingInternal(themeMode);
        com.example.animelib.util.ThemeUtils.saveThemePreference(context, themeMode);
    }

    public void saveThemeSettingInternal(int themeMode) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setThemeMode(themeMode);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save theme setting", e);
            }
        });
    }

    public int loadThemeSettingFromDb() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getThemeMode() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int loadThemeSetting() {
        return com.example.animelib.util.ThemeUtils.getSavedThemePreference(context);
    }

    public void saveTestModeSetting(boolean enabled) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setTestModeEnabled(enabled);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save test mode setting", e);
            }
        });
    }

    public boolean loadTestModeSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null || settings.isTestModeEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    public void saveSubtitlesEnabledSetting(boolean subtitlesEnabled) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setSubtitlesEnabled(subtitlesEnabled);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save subtitlesEnabled setting", e);
            }
        });
    }

    public boolean loadSubtitlesEnabledSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null || settings.isSubtitlesEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    public void saveSubtitleFormatSetting(String subtitleFormat) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setSubtitleFormat(subtitleFormat);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save subtitleFormat setting", e);
            }
        });
    }

    public String loadSubtitleFormatSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getSubtitleFormat() : "ass";
        } catch (Exception e) {
            return "ass";
        }
    }

    public void saveSubtitleStyleSettings(float textSize, int textColor, int bgColor, int edgeType, int edgeColor) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) settings = new AppSettings();
                settings.setSubtitleTextSize(textSize);
                settings.setSubtitleTextColor(textColor);
                settings.setSubtitleBackgroundColor(bgColor);
                settings.setSubtitleEdgeType(edgeType);
                settings.setSubtitleEdgeColor(edgeColor);
                db.appSettingsDao().upsert(settings);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save subtitle styles", e);
            }
        });
    }

    public float loadSubtitleTextSizeSetting() {
        try {
            AppSettings s = db.appSettingsDao().getSettingsSync();
            return s != null ? s.getSubtitleTextSize() : 18f;
        } catch (Exception e) { return 18f; }
    }

    public int loadSubtitleTextColorSetting() {
        try {
            AppSettings s = db.appSettingsDao().getSettingsSync();
            return s != null ? s.getSubtitleTextColor() : 0xFFFFFFFF;
        } catch (Exception e) { return 0xFFFFFFFF; }
    }

    public int loadSubtitleBackgroundColorSetting() {
        try {
            AppSettings s = db.appSettingsDao().getSettingsSync();
            return s != null ? s.getSubtitleBackgroundColor() : 0x00000000;
        } catch (Exception e) { return 0x00000000; }
    }

    public int loadSubtitleEdgeTypeSetting() {
        try {
            AppSettings s = db.appSettingsDao().getSettingsSync();
            return s != null ? s.getSubtitleEdgeType() : 1;
        } catch (Exception e) { return 1; }
    }

    public int loadSubtitleEdgeColorSetting() {
        try {
            AppSettings s = db.appSettingsDao().getSettingsSync();
            return s != null ? s.getSubtitleEdgeColor() : 0xFF000000;
        } catch (Exception e) { return 0xFF000000; }
    }

    public void saveResizeModeSetting(int mode) {
        context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
                .edit().putInt("resize_mode", mode).apply();
    }

    public int loadResizeModeSetting() {
        return context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
                .getInt("resize_mode", 0);
    }

    // ========== Video Playback History Operations ==========

    public void saveVideoProgress(String filePath, String title, long durationMs, long lastPositionMs, String folderName) {
        if (filePath == null || filePath.trim().isEmpty()) return;
        executor.execute(() -> {
            try {
                long duration = durationMs;
                if (duration <= 0) {
                    VideoPlaybackHistoryEntity old = db.videoPlaybackHistoryDao().getHistoryForPath(filePath);
                    if (old != null && old.getDurationMs() > 0) {
                        duration = old.getDurationMs();
                    }
                }
                VideoPlaybackHistoryEntity entity = new VideoPlaybackHistoryEntity(
                        filePath,
                        title != null ? title : "",
                        duration,
                        lastPositionMs,
                        System.currentTimeMillis(),
                        folderName != null ? folderName : ""
                );
                db.videoPlaybackHistoryDao().insertOrUpdate(entity);
            } catch (Exception e) {
                Log.e(TAG, "Error saving video playback progress", e);
            }
        });
    }

    public VideoPlaybackHistoryEntity getVideoHistory(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return null;
        try {
            return db.videoPlaybackHistoryDao().getHistoryForPath(filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error getting video history", e);
            return null;
        }
    }

    public List<VideoPlaybackHistoryEntity> getAllVideoHistory() {
        try {
            return db.videoPlaybackHistoryDao().getAllHistory();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all video history", e);
            return new ArrayList<>();
        }
    }

    public VideoPlaybackHistoryEntity getLastPlayedVideo() {
        try {
            List<VideoPlaybackHistoryEntity> list = db.videoPlaybackHistoryDao().getAllHistory();
            for (VideoPlaybackHistoryEntity item : list) {
                if (item.getFilePath() != null && new java.io.File(item.getFilePath()).exists()) {
                    return item;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last played video", e);
        }
        return null;
    }

    public void deleteVideoHistory(String filePath) {
        if (filePath == null) return;
        executor.execute(() -> {
            try {
                db.videoPlaybackHistoryDao().deleteByPath(filePath);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting video history", e);
            }
        });
    }

    public void clearAllVideoHistory() {
        executor.execute(() -> {
            try {
                db.videoPlaybackHistoryDao().clearAllHistory();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing video history", e);
            }
        });
    }

    // ========== Playlist Operations ==========

    public long createPlaylist(String name, List<VideoItem> selectedVideos) {
        try {
            com.example.animelib.data.entity.PlaylistEntity playlist = new com.example.animelib.data.entity.PlaylistEntity(name, System.currentTimeMillis());
            long playlistId = db.playlistDao().insertPlaylist(playlist);
            if (selectedVideos != null && !selectedVideos.isEmpty()) {
                List<com.example.animelib.data.entity.PlaylistVideoEntity> videoEntities = new ArrayList<>();
                for (int i = 0; i < selectedVideos.size(); i++) {
                    VideoItem item = selectedVideos.get(i);
                    String path = item.getPath() != null && !item.getPath().isEmpty() ? item.getPath() : item.getUriString();
                    videoEntities.add(new com.example.animelib.data.entity.PlaylistVideoEntity(playlistId, path, item.getTitle(), i));
                }
                db.playlistDao().insertPlaylistVideos(videoEntities);
            }
            return playlistId;
        } catch (Exception e) {
            Log.e(TAG, "Error creating playlist", e);
            return -1;
        }
    }

    public List<com.example.animelib.data.entity.PlaylistEntity> getAllPlaylists() {
        try {
            return db.playlistDao().getAllPlaylists();
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlists", e);
            return new ArrayList<>();
        }
    }

    public List<com.example.animelib.data.entity.PlaylistVideoEntity> getVideosForPlaylist(long playlistId) {
        try {
            return db.playlistDao().getVideosForPlaylist(playlistId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist videos", e);
            return new ArrayList<>();
        }
    }

    public int getVideoCountForPlaylist(long playlistId) {
        try {
            return db.playlistDao().getVideoCountForPlaylist(playlistId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist video count", e);
            return 0;
        }
    }

    public com.example.animelib.data.entity.PlaylistEntity getPlaylistById(long playlistId) {
        try {
            return db.playlistDao().getPlaylistById(playlistId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist by id", e);
            return null;
        }
    }

    public com.example.animelib.data.entity.PlaylistEntity getPlaylistForVideoPath(String videoPath) {
        if (videoPath == null || videoPath.trim().isEmpty()) return null;
        try {
            return db.playlistDao().getPlaylistForVideoPath(videoPath);
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist for video path", e);
            return null;
        }
    }

    public void deletePlaylist(long playlistId) {
        executor.execute(() -> {
            try {
                db.playlistDao().deleteVideosForPlaylist(playlistId);
                db.playlistDao().deletePlaylist(playlistId);
                com.example.animelib.adapters.PlaylistsAdapter.invalidatePlaylist(playlistId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting playlist", e);
            }
        });
    }

    public void renamePlaylist(long playlistId, String newName) {
        executor.execute(() -> {
            try {
                db.playlistDao().updatePlaylistName(playlistId, newName);
            } catch (Exception e) {
                Log.e(TAG, "Error renaming playlist", e);
            }
        });
    }

    public void clearPlaylistVideos(long playlistId) {
        executor.execute(() -> {
            try {
                db.playlistDao().deleteVideosForPlaylist(playlistId);
                com.example.animelib.adapters.PlaylistsAdapter.invalidatePlaylist(playlistId);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing playlist videos", e);
            }
        });
    }

    public void removeVideoFromPlaylist(long playlistId, String videoPath) {
        executor.execute(() -> {
            try {
                db.playlistDao().deleteVideoFromPlaylist(playlistId, videoPath);
                com.example.animelib.adapters.PlaylistsAdapter.invalidatePlaylist(playlistId);
            } catch (Exception e) {
                Log.e(TAG, "Error removing video from playlist", e);
            }
        });
    }

    public void addVideoToPlaylist(long playlistId, String path, String title) {
        executor.execute(() -> {
            try {
                List<com.example.animelib.data.entity.PlaylistVideoEntity> existing = db.playlistDao().getVideosForPlaylist(playlistId);
                int nextIndex = existing != null ? existing.size() : 0;
                db.playlistDao().insertPlaylistVideo(new com.example.animelib.data.entity.PlaylistVideoEntity(playlistId, path, title, nextIndex));
                com.example.animelib.adapters.PlaylistsAdapter.invalidatePlaylist(playlistId);
            } catch (Exception e) {
                Log.e(TAG, "Error adding video to playlist", e);
            }
        });
    }

    public void updateVideoTitle(String path, String newTitle) {
        executor.execute(() -> {
            try {
                com.example.animelib.data.entity.VideoPlaybackHistoryEntity history = db.videoPlaybackHistoryDao().getHistoryForPath(path);
                if (history != null) {
                    history.setTitle(newTitle);
                    db.videoPlaybackHistoryDao().insertOrUpdate(history);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating video title", e);
            }
        });
    }

    public void deleteVideoEverywhere(String path) {
        executor.execute(() -> {
            try {
                db.videoPlaybackHistoryDao().deleteByPath(path);
                db.playlistDao().deleteVideoFromAllPlaylists(path);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting video from database", e);
            }
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
