package com.example.animelib.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(PlaylistEntity playlist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistVideos(List<PlaylistVideoEntity> videos);

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllPlaylists();

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    List<PlaylistVideoEntity> getVideosForPlaylist(long playlistId);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylist(long playlistId);

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    void deleteVideosForPlaylist(long playlistId);
}
