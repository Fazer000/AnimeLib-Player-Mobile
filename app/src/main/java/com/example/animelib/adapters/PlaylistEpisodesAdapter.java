package com.example.animelib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;

import java.util.List;

public class PlaylistEpisodesAdapter extends RecyclerView.Adapter<PlaylistEpisodesAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(int position);
    }

    private final List<String> titles;
    private int currentIndex;
    private final OnEpisodeClickListener listener;

    public PlaylistEpisodesAdapter(List<String> titles, int currentIndex, OnEpisodeClickListener listener) {
        this.titles = titles;
        this.currentIndex = currentIndex;
        this.listener = listener;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        String title = titles.get(position);
        holder.tvEpisodeNumber.setText(String.valueOf(position + 1));
        holder.tvEpisodeTitle.setText(title);

        boolean isCurrent = (position == currentIndex);
        if (isCurrent) {
            holder.tvEpisodeNumber.setBackgroundResource(R.drawable.bg_m3_selected_pill);
            holder.tvEpisodeNumber.setTextColor(0xFFFFFFFF);
            holder.tvEpisodeTitle.setTextColor(0xFFA855F7);
            holder.ivPlayingIndicator.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(0x15A855F7);
        } else {
            holder.tvEpisodeNumber.setBackgroundResource(R.drawable.bg_m3_unselected_circle);
            holder.tvEpisodeNumber.setTextColor(0xFF8E8E93);
            holder.tvEpisodeTitle.setTextColor(0xFFE5E5EA);
            holder.ivPlayingIndicator.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEpisodeClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvEpisodeNumber;
        TextView tvEpisodeTitle;
        ImageView ivPlayingIndicator;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEpisodeNumber = itemView.findViewById(R.id.tvEpisodeNumber);
            tvEpisodeTitle = itemView.findViewById(R.id.tvEpisodeTitle);
            ivPlayingIndicator = itemView.findViewById(R.id.ivPlayingIndicator);
        }
    }
}
