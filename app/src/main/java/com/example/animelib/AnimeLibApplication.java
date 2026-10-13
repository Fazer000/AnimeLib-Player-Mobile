package com.example.animelib;

import android.app.Application;
import com.example.animelib.util.ImageLoader;

public class AnimeLibApplication extends Application {
    private static AnimeLibApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ImageLoader.init(this);
    }

    public static AnimeLibApplication getInstance() {
        return instance;
    }
}
