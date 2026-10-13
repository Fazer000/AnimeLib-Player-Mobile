package com.example.animelib.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity for saving watch progress and history of local video files.
 */
@Entity(tableName = "video_playback_history")
public class VideoPlaybackHistoryEntity {

    @PrimaryKey
    @NonNull
    private String filePath;

    private String title;
    private long durationMs;
    private long lastPositionMs;
    private long lastWatchedTimestamp;
    private String folderName;

    public VideoPlaybackHistoryEntity(@NonNull String filePath, String title, long durationMs,
                                      long lastPositionMs, long lastWatchedTimestamp, String folderName) {
        this.filePath = filePath;
        this.title = title;
        this.durationMs = durationMs;
        this.lastPositionMs = lastPositionMs;
        this.lastWatchedTimestamp = lastWatchedTimestamp;
        this.folderName = folderName;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public long getLastPositionMs() {
        return lastPositionMs;
    }

    public void setLastPositionMs(long lastPositionMs) {
        this.lastPositionMs = lastPositionMs;
    }

    public long getLastWatchedTimestamp() {
        return lastWatchedTimestamp;
    }

    public void setLastWatchedTimestamp(long lastWatchedTimestamp) {
        this.lastWatchedTimestamp = lastWatchedTimestamp;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getProgressPercentage() {
        if (durationMs <= 0) return 0;
        int p = (int) ((lastPositionMs * 100) / durationMs);
        return Math.max(0, Math.min(100, p));
    }
}
