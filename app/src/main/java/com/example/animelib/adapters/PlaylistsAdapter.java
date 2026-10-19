package com.example.animelib.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.util.VideoThumbnailHelper;

import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

    public interface OnPlaylistListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onPlaylistDelete(PlaylistEntity playlist);
    }

    private final List<PlaylistEntity> playlists;
    private final OnPlaylistListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlaylistsAdapter(List<PlaylistEntity> playlists, OnPlaylistListener listener) {
        this.playlists = playlists;
        this.listener = listener;
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
        String dateStr = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(new java.util.Date(playlist.getCreatedAt()));
        holder.tvPlaylistName.setText(playlist.getName());
        holder.tvPlaylistVideoCount.setText("... • " + dateStr);

        holder.ivPlaylistThumbnail.setImageResource(R.drawable.placeholder_image);
        String tagKey = "playlist_" + playlist.getId();
        holder.ivPlaylistThumbnail.setTag(tagKey);

        Context context = holder.itemView.getContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PlaylistVideoEntity> videos = DatabaseManager.getInstance(context).getVideosForPlaylist(playlist.getId());
            int count = videos != null ? videos.size() : 0;
            String countText = count + " видео • " + dateStr;
            Bitmap thumb = null;
            if (videos != null && !videos.isEmpty()) {
                String firstPath = videos.get(0).getVideoPath();
                if (firstPath != null) {
                    thumb = VideoThumbnailHelper.createFrameThumbnail(context, firstPath, null, 0);
                }
            }
            final Bitmap finalThumb = thumb;
            mainHandler.post(() -> {
                if (tagKey.equals(holder.ivPlaylistThumbnail.getTag())) {
                    holder.tvPlaylistVideoCount.setText(countText);
                    if (finalThumb != null) {
                        holder.ivPlaylistThumbnail.setImageBitmap(finalThumb);
                    } else {
                        holder.ivPlaylistThumbnail.setImageResource(R.drawable.placeholder_image);
                    }
                }
            });
        });

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
