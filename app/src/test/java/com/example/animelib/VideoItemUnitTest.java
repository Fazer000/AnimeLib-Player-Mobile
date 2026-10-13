package com.example.animelib;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.animelib.models.VideoFolder;
import com.example.animelib.models.VideoItem;

public class VideoItemUnitTest {

    @Test
    public void testVideoItemFormatting() {
        VideoItem item = new VideoItem(
                1L,
                "Episode 01.mp4",
                "/storage/emulated/0/Movies/Episode 01.mp4",
                null,
                1425000L,   // 23m 45s
                125000000L, // ~119 MB
                System.currentTimeMillis(),
                "Movies"
        );

        assertEquals("Episode 01.mp4", item.getTitle());
        assertEquals("Movies", item.getFolderName());
        assertEquals(125000000L, item.getSizeBytes());
        assertEquals(1425000L, item.getDurationMs());

        String formattedDuration = item.getFormattedDuration();
        assertEquals("23:45", formattedDuration);

        String formattedSize = item.getFormattedSize();
        assertTrue(formattedSize.contains("МБ"));

        // Test progress
        item.setLastPositionMs(712500L); // 50%
        assertEquals(50, item.getProgressPercent());
    }

    @Test
    public void testVideoFolder() {
        VideoFolder folder = new VideoFolder("Anime", 12);
        assertEquals("Anime", folder.getName());
        assertEquals(12, folder.getCount());
        assertFalse(folder.isSelected());

        folder.setSelected(true);
        assertTrue(folder.isSelected());

        folder.setCount(15);
        assertEquals(15, folder.getCount());
    }
}
