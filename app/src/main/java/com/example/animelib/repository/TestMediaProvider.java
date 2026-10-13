package com.example.animelib.repository;

import android.net.Uri;

import com.example.animelib.models.VideoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Провайдер тестовых медиафайлов для проверки всех функций плеера (Ambilight, 5.1 Surround, фильтры, жесты, кастомные сегменты).
 */
public class TestMediaProvider {

    public static final String TEST_FOLDER_NAME = "Тестовые видео";

    public static List<VideoItem> getTestVideoItems() {
        List<VideoItem> list = new ArrayList<>();

        list.add(new VideoItem(
                9001L,
                "Big Buck Bunny (1080p FHD Test)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                596000L, // ~9 min 56 sec
                158000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9002L,
                "Tears of Steel (Sci-Fi 4K Ambilight Demo)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
                734000L, // ~12 min 14 sec
                225000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9003L,
                "Sintel (720p HD Animation Demo)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
                888000L, // ~14 min 48 sec
                130000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9004L,
                "Elephants Dream (FHD Audio & Surround Demo)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                653000L, // ~10 min 53 sec
                175000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9005L,
                "For Bigger Blazes (Short Format Demo)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                15000L, // ~15 sec
                18000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        return list;
    }
}
