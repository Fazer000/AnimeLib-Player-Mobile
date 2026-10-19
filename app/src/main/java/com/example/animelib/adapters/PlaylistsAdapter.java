package com.example.animelib.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.util.VideoThumbnailHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

    public interface OnPlaylistListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onPlaylistDelete(PlaylistEntity playlist);
    }

    private static final Map<Long, Integer> countCache = new ConcurrentHashMap<>();
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);

    private final List<PlaylistEntity> playlists;
    private final OnPlaylistListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlaylistsAdapter(List<PlaylistEntity> playlists, OnPlaylistListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    public static void invalidatePlaylist(long playlistId) {
        countCache.remove(playlistId);
        VideoThumbnailHelper.putCachedThumbnail("playlist_thumb_" + playlistId, null);
    }

    public static void clearCache() {
        countCache.clear();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_card, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistEntity playlist = playlists.get(position);
        long playlistId = playlist.getId();
        String dateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(playlist.getCreatedAt()));
        holder.tvPlaylistName.setText(playlist.getName());

        String tagKey = "playlist_" + playlistId;
        holder.tvPlaylistVideoCount.setTag(tagKey);
        holder.ivPlaylistThumbnail.setTag(tagKey);

        Context context = holder.itemView.getContext();
        DatabaseManager db = DatabaseManager.getInstance(context);

        // 1. Отображение количества видео: синхронно из кэша или быстрым запросом (никаких вечных трех точек)
        Integer cachedCount = countCache.get(playlistId);
        if (cachedCount != null) {
            holder.tvPlaylistVideoCount.setText(cachedCount + " видео • " + dateStr);
        } else {
            // Быстрый запрос количества из базы Room (выполняется за доли миллисекунды)
            int count = db.getVideoCountForPlaylist(playlistId);
            countCache.put(playlistId, count);
            holder.tvPlaylistVideoCount.setText(count + " видео • " + dateStr);
        }

        // 2. Отображение превью: сначала проверяем кэш
        String thumbKey = "playlist_thumb_" + playlistId;
        Bitmap cachedThumb = VideoThumbnailHelper.getCachedThumbnail(thumbKey);
        if (cachedThumb != null && !cachedThumb.isRecycled()) {
            holder.ivPlaylistThumbnail.setImageBitmap(cachedThumb);
        } else {
            holder.ivPlaylistThumbnail.setImageResource(R.drawable.placeholder_image);
            final Context appContext = context.getApplicationContext();
            backgroundExecutor.execute(() -> {
                List<PlaylistVideoEntity> videos = db.getVideosForPlaylist(playlistId);
                Bitmap thumb = null;
                int currentCount = 0;
                if (videos != null && !videos.isEmpty()) {
                    currentCount = videos.size();
                    String firstPath = videos.get(0).getVideoPath();
                    if (firstPath != null) {
                        Uri firstUri = firstPath.startsWith("content://") ? Uri.parse(firstPath) : null;
                        thumb = VideoThumbnailHelper.createFrameThumbnail(appContext, firstPath, firstUri, 0);
                    }
                }
                
                final int finalCount = currentCount;
                countCache.put(playlistId, finalCount);
                final Bitmap finalThumb = thumb;
                if (finalThumb != null) {
                    VideoThumbnailHelper.putCachedThumbnail(thumbKey, finalThumb);
                }

                mainHandler.post(() -> {
                    if (tagKey.equals(holder.tvPlaylistVideoCount.getTag())) {
                        holder.tvPlaylistVideoCount.setText(finalCount + " видео • " + dateStr);
                    }
                    if (tagKey.equals(holder.ivPlaylistThumbnail.getTag())) {
                        if (finalThumb != null) {
                            holder.ivPlaylistThumbnail.setImageBitmap(finalThumb);
                        } else {
                            holder.ivPlaylistThumbnail.setImageResource(R.drawable.placeholder_image);
                        }
                    }
                });
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaylistClick(playlist);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onPlaylistDelete(playlist);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaylistThumbnail;
        TextView tvPlaylistName;
        TextView tvPlaylistVideoCount;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaylistThumbnail = itemView.findViewById(R.id.ivPlaylistThumbnail);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvPlaylistVideoCount = itemView.findViewById(R.id.tvPlaylistVideoCount);
        }
    }
}
