package com.example.animelib.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;

import java.util.List;

@Dao
public interface VideoPlaybackHistoryDao {

    @Query("SELECT * FROM video_playback_history ORDER BY lastWatchedTimestamp DESC")
    List<VideoPlaybackHistoryEntity> getAllHistory();

    @Query("SELECT * FROM video_playback_history WHERE filePath = :filePath LIMIT 1")
    VideoPlaybackHistoryEntity getHistoryForPath(String filePath);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(VideoPlaybackHistoryEntity entity);

    @Query("DELETE FROM video_playback_history WHERE filePath = :filePath")
    void deleteByPath(String filePath);

    @Query("DELETE FROM video_playback_history")
    void clearAllHistory();
}
