package com.example.animelib.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_videos")
public class PlaylistVideoEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long playlistId;
    private String videoPath;
    private String videoTitle;
    private int orderIndex;

    public PlaylistVideoEntity(long playlistId, String videoPath, String videoTitle, int orderIndex) {
        this.playlistId = playlistId;
        this.videoPath = videoPath;
        this.videoTitle = videoTitle;
        this.orderIndex = orderIndex;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
