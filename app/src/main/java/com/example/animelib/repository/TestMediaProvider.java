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
                "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-progress-h264.mp4",
                Uri.parse("https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-progress-h264.mp4"),
                596000L, // ~9 min 56 sec
                158000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9002L,
                "Tears of Steel (Sci-Fi 4K Ambilight Demo)",
                "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-4k-h264.mkv",
                Uri.parse("https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-4k-h264.mkv"),
                734000L, // ~12 min 14 sec
                225000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9003L,
                "Sintel (720p HD Animation Demo)",
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                Uri.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                888000L, // ~14 min 48 sec
                130000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9004L,
                "Elephants Dream (FHD Audio & Surround Demo)",
                "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash/universe-streams/playlist.m3u8",
                Uri.parse("https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash/universe-streams/playlist.m3u8"),
                653000L, // ~10 min 53 sec
                175000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        list.add(new VideoItem(
                9005L,
                "For Bigger Blazes (Short Format Demo)",
                "https://www.w3schools.com/html/mov_bbb.mp4",
                Uri.parse("https://www.w3schools.com/html/mov_bbb.mp4"),
                15000L, // ~15 sec
                18000000L,
                System.currentTimeMillis() / 1000,
                TEST_FOLDER_NAME
        ));

        return list;
    }
}
