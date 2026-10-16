package com.example.animelib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.data.entity.PlaylistEntity;

import java.util.List;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

    public interface OnPlaylistListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onPlaylistDelete(PlaylistEntity playlist);
    }

    private final List<PlaylistEntity> playlists;
    private final OnPlaylistListener listener;

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
        holder.tvPlaylistName.setText(playlist.getName());
        holder.tvPlaylistVideoCount.setText("Создан: " + new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(new java.util.Date(playlist.getCreatedAt())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaylistClick(playlist);
        });

        holder.btnDeletePlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onPlaylistDelete(playlist);
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaylistName;
        TextView tvPlaylistVideoCount;
        ImageButton btnDeletePlaylist;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvPlaylistVideoCount = itemView.findViewById(R.id.tvPlaylistVideoCount);
            btnDeletePlaylist = itemView.findViewById(R.id.btnDeletePlaylist);
        }
    }
}
