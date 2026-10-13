package com.example.animelib.repository;

import com.example.animelib.models.VideoItem;

import java.util.List;

/**
 * Интерфейс репозитория для доступа к локальным видеофайлам устройства.
 */
public interface VideoRepository {
    /**
     * Сканирует локальные видеофайлы и синхронизирует сохраненный прогресс воспроизведения.
     */
    List<VideoItem> scanVideos();

    /**
     * Быстро обновляет информацию о прогрессе просмотра для уже загруженного списка видео.
     * @return true, если хотя бы у одного видео прогресс изменился.
     */
    boolean refreshWatchProgress(List<VideoItem> items);

    /**
     * Возвращает список тестовых медиафайлов для демо-режима плеера.
     */
    List<VideoItem> getTestVideos();
}
