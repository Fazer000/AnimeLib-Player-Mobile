package com.example.animelib.controller;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер плейлиста для навигации между файлами (Next, Previous, текущий индекс).
 */
public class PlayerPlaylistManager {

    private String currentVideoPath;
    private String currentVideoTitle;
    private String currentFolderName = "Видео";
    private boolean isPlaylist = false;
    private String playlistName = null;

    private final List<String> playlistPaths = new ArrayList<>();
    private final List<String> playlistTitles = new ArrayList<>();
    private int currentPlaylistIndex = 0;

    public void parseIntent(@Nullable Intent intent) {
        if (intent == null) return;

        Uri dataUri = intent.getData();
        String localPath = intent.getStringExtra("EXTRA_LOCAL_FILE_PATH");
        if (localPath == null) {
            localPath = intent.getStringExtra("EXTRA_VIDEO_PATH");
        }

        if (localPath != null) {
            currentVideoPath = localPath;
        } else if (dataUri != null) {
            currentVideoPath = dataUri.toString();
        }

        currentVideoTitle = intent.getStringExtra("EXTRA_VIDEO_TITLE");
        if (currentVideoTitle == null && currentVideoPath != null) {
            currentVideoTitle = new File(currentVideoPath).getName();
        }

        currentFolderName = intent.getStringExtra("EXTRA_FOLDER_NAME");
        if (currentFolderName == null) currentFolderName = "Видео";

        isPlaylist = intent.getBooleanExtra("EXTRA_IS_PLAYLIST", false);
        playlistName = intent.getStringExtra("EXTRA_PLAYLIST_NAME");
        if (playlistName != null && !playlistName.trim().isEmpty()) {
            isPlaylist = true;
        } else if (isPlaylist) {
            playlistName = currentFolderName;
        }

        ArrayList<String> paths = intent.getStringArrayListExtra("EXTRA_PLAYLIST_PATHS");
        ArrayList<String> titles = intent.getStringArrayListExtra("EXTRA_PLAYLIST_TITLES");
        int index = intent.getIntExtra("EXTRA_PLAYLIST_INDEX", 0);

        if (paths != null && !paths.isEmpty()) {
            playlistPaths.clear();
            playlistPaths.addAll(paths);

            playlistTitles.clear();
            if (titles != null) playlistTitles.addAll(titles);

            currentPlaylistIndex = Math.max(0, Math.min(index, playlistPaths.size() - 1));
            currentVideoPath = playlistPaths.get(currentPlaylistIndex);
            if (currentPlaylistIndex < playlistTitles.size()) {
                currentVideoTitle = playlistTitles.get(currentPlaylistIndex);
            }
        } else if (currentVideoPath != null) {
            playlistPaths.clear();
            playlistPaths.add(currentVideoPath);
            playlistTitles.clear();
            playlistTitles.add(currentVideoTitle);
            currentPlaylistIndex = 0;
        }
    }

    public boolean hasNext() {
        return currentPlaylistIndex < playlistPaths.size() - 1;
    }

    public boolean hasPrevious() {
        return currentPlaylistIndex > 0;
    }

    public int getNextIndex() {
        return currentPlaylistIndex + 1;
    }

    public int getPreviousIndex() {
        return currentPlaylistIndex - 1;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < playlistPaths.size()) {
            currentPlaylistIndex = index;
            currentVideoPath = playlistPaths.get(currentPlaylistIndex);
            if (currentPlaylistIndex < playlistTitles.size()) {
                currentVideoTitle = playlistTitles.get(currentPlaylistIndex);
            }
        }
    }

    public String getCurrentPath() {
        return currentVideoPath;
    }

    public String getCurrentTitle() {
        return currentVideoTitle;
    }

    public String getCurrentFolderName() {
        return currentFolderName;
    }

    public List<String> getPlaylistPaths() {
        return playlistPaths;
    }

    public List<String> getPlaylistTitles() {
        return playlistTitles;
    }

    public int getCurrentIndex() {
        return currentPlaylistIndex;
    }

    public boolean isEmpty() {
        return playlistPaths.isEmpty();
    }

    public boolean isPlaylist() {
        return isPlaylist;
    }

    public void setPlaylist(boolean playlist) {
        this.isPlaylist = playlist;
    }

    public String getPlaylistName() {
        if (playlistName != null && !playlistName.trim().isEmpty()) {
            return playlistName;
        }
        return currentFolderName;
    }

    public void setPlaylistName(String name) {
        this.playlistName = name;
        if (name != null) this.isPlaylist = true;
    }
}
