package com.example.animelib.domain;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.models.VideoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Провайдер тестовых видеосценариев для полноценной проверки возможностей плеера
 * (Ambilight, 3D Surround Sound 5.1, жесты свайпов, масштабирование, PiP и блокировка).
 */
public class DemoVideoProvider {

    public static class TestScenario {
        private final VideoItem videoItem;
        private final String category;
        private final String featureTags;
        private final String testInstructions;

        public TestScenario(VideoItem videoItem, String category, String featureTags, String testInstructions) {
            this.videoItem = videoItem;
            this.category = category;
            this.featureTags = featureTags;
            this.testInstructions = testInstructions;
        }

        public VideoItem getVideoItem() { return videoItem; }
        public String getCategory() { return category; }
        public String getFeatureTags() { return featureTags; }
        public String getTestInstructions() { return testInstructions; }
    }

    public static List<TestScenario> getTestScenarios() {
        List<TestScenario> scenarios = new ArrayList<>();

        // 1. Базовый тест: жесты, плавность, скраббинг
        VideoItem item1 = new VideoItem(
                900001L,
                "Big Buck Bunny (1080p, 60 FPS)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                596000L,
                158000000L,
                System.currentTimeMillis() / 1000,
                "Тестовые видео"
        );
        scenarios.add(new TestScenario(
                item1,
                "Базовый тест",
                "60 FPS • Свайпы громкости/яркости • Скраббинг",
                "Проверьте плавность 60fps, свайп по левой стороне для яркости, по правой для громкости и горизонтальную перемотку с превью."
        ));

        // 2. Ambilight и видеофильтры
        VideoItem item2 = new VideoItem(
                900002L,
                "Tears of Steel (Sci-Fi, 4K HDR)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
                734000L,
                240000000L,
                System.currentTimeMillis() / 1000 - 3600,
                "Тестовые видео"
        );
        scenarios.add(new TestScenario(
                item2,
                "Ambilight & Фильтры",
                "Ambilight Подсветка • Гамма и контраст • 2x Скорость",
                "Проверьте мягкую фоновую подсветку краев экрана (Ambilight), настройку насыщенности в меню и удержание пальца для ускорения 2x."
        ));

        // 3. 3D Surround Sound 5.1
        VideoItem item3 = new VideoItem(
                900003L,
                "Elephant's Dream (Surround 5.1 Test)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                653000L,
                180000000L,
                System.currentTimeMillis() / 1000 - 7200,
                "Тестовые видео"
        );
        scenarios.add(new TestScenario(
                item3,
                "Звук 3D Surround 5.1",
                "Пространственный звук • Bass Boost • Диалоги",
                "Проверьте режим 5.1 Surround Sound в меню: переключение режимов Кино/3D, усиление баса и пространственное расширение стерео."
        ));

        // 4. PiP и Блокировка экрана
        VideoItem item4 = new VideoItem(
                900004L,
                "Sintel (Full HD, 1080p)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
                888000L,
                210000000L,
                System.currentTimeMillis() / 1000 - 10800,
                "Тестовые видео"
        );
        scenarios.add(new TestScenario(
                item4,
                "PiP & Lock Screen",
                "Картинка в картинке • Замок экрана • Свайп вверх",
                "Нажмите иконку замка для блокировки от случайных касаний и свайпните снизу вверх для разблокировки. Проверьте кнопку PiP."
        ));

        // 5. Двойной тап и соотношение сторон
        VideoItem item5 = new VideoItem(
                900005L,
                "For Bigger Blazes (Action 1080p)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                15000L,
                48000000L,
                System.currentTimeMillis() / 1000 - 14400,
                "Тестовые видео"
        );
        scenarios.add(new TestScenario(
                item5,
                "Скип & Масштабирование",
                "Двойной тап ±10 сек • Режимы Fit/Zoom/Stretch",
                "Проверьте двойной тап слева/справа для быстрого прыжка и смену соотношения сторон в настройках (По размеру / Заполнение)."
        ));

        return scenarios;
    }

    public static List<VideoItem> getDemoVideos() {
        List<VideoItem> list = new ArrayList<>();
        for (TestScenario s : getTestScenarios()) {
            list.add(s.getVideoItem());
        }
        return list;
    }

    /**
     * Создает интент для запуска выбранного сценария со всеми тестовыми видео в качестве плейлиста.
     */
    public static Intent createTestPlaylistIntent(Context context, int startIndex) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        List<VideoItem> demoList = getDemoVideos();

        if (startIndex < 0 || startIndex >= demoList.size()) startIndex = 0;
        VideoItem current = demoList.get(startIndex);

        intent.putExtra("EXTRA_LOCAL_FILE_PATH", current.getPath());
        intent.putExtra("EXTRA_VIDEO_TITLE", current.getTitle());
        intent.putExtra("EXTRA_FOLDER_NAME", current.getFolderName());

        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        for (VideoItem v : demoList) {
            paths.add(v.getPath());
            titles.add(v.getTitle());
        }

        intent.putStringArrayListExtra("EXTRA_PLAYLIST_PATHS", paths);
        intent.putStringArrayListExtra("EXTRA_PLAYLIST_TITLES", titles);
        intent.putExtra("EXTRA_PLAYLIST_INDEX", startIndex);

        return intent;
    }
}
