package com.example.animelib.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppSettings settings);

    @Update
    void update(AppSettings settings);

    @Query("SELECT * FROM app_settings LIMIT 1")
    LiveData<AppSettings> getSettings();

    @Query("SELECT * FROM app_settings LIMIT 1")
    AppSettings getSettingsSync();

    @Query("SELECT COUNT(*) FROM app_settings")
    int getSettingsCount();

    @Query("DELETE FROM app_settings WHERE id NOT IN (SELECT id FROM app_settings ORDER BY id ASC LIMIT 1)")
    void cleanupDuplicateSettings();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppSettings settings);
}
