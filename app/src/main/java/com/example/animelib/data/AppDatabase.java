package com.example.animelib.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.animelib.data.dao.VideoPlaybackHistoryDao;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;

@Database(entities = {
        AppSettings.class,
        VideoPlaybackHistoryEntity.class,
        com.example.animelib.data.entity.PlaylistEntity.class,
        com.example.animelib.data.entity.PlaylistVideoEntity.class
}, version = 31, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract AppSettingsDao appSettingsDao();
    public abstract VideoPlaybackHistoryDao videoPlaybackHistoryDao();
    public abstract com.example.animelib.data.dao.PlaylistDao playlistDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "animelib_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
