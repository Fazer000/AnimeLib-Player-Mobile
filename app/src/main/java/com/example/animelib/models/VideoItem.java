package com.example.animelib.models;

import android.net.Uri;
import java.io.Serializable;
import java.util.Locale;

public class VideoItem implements Serializable {
    private final long id;
    private final String title;
    private final String path;
    private final String uriString;
    private final long durationMs;
    private final long sizeBytes;
    private final long dateModified;
    private final String folderName;
    private long lastPositionMs;
    private int progressPercent;

    public VideoItem(long id, String title, String path, Uri uri, long durationMs,
                     long sizeBytes, long dateModified, String folderName) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.uriString = uri != null ? uri.toString() : "";
        this.durationMs = durationMs;
        this.sizeBytes = sizeBytes;
        this.dateModified = dateModified;
        this.folderName = folderName != null && !folderName.isEmpty() ? folderName : "Видео";
        this.lastPositionMs = 0;
        this.progressPercent = 0;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public Uri getUri() { return uriString != null && !uriString.isEmpty() ? Uri.parse(uriString) : null; }
    public String getUriString() { return uriString; }
    public long getDurationMs() { return durationMs; }
    public long getSizeBytes() { return sizeBytes; }
    public long getDateModified() { return dateModified; }
    public String getFolderName() { return folderName; }
    public long getLastPositionMs() { return lastPositionMs; }
    public void setLastPositionMs(long lastPositionMs) {
        this.lastPositionMs = lastPositionMs;
        if (durationMs > 0) {
            this.progressPercent = (int) Math.min(100, Math.max(0, (lastPositionMs * 100) / durationMs));
        }
    }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getFormattedDuration() {
        if (durationMs <= 0) return "--:--";
        long totalSecs = durationMs / 1000;
        long hours = totalSecs / 3600;
        long mins = (totalSecs % 3600) / 60;
        long secs = totalSecs % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        }
    }

    public String getFormattedSize() {
        if (sizeBytes <= 0) return "";
        double kb = sizeBytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1.0) {
            return String.format(Locale.getDefault(), "%.1f ГБ", gb);
        } else if (mb >= 1.0) {
            return String.format(Locale.getDefault(), "%.0f МБ", mb);
        } else {
            return String.format(Locale.getDefault(), "%.0f КБ", kb);
        }
    }
}
