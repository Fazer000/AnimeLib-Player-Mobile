package com.example.animelib.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.VideoFolder;

import java.util.List;

public class VideoFoldersAdapter extends RecyclerView.Adapter<VideoFoldersAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(VideoFolder folder);
    }

    private final List<VideoFolder> folders;
    private final OnFolderClickListener listener;
    private int selectedPosition = 0;

    public VideoFoldersAdapter(List<VideoFolder> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_chip, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        VideoFolder folder = folders.get(position);
        Context context = holder.itemView.getContext();

        holder.tvFolderChip.setText(folder.getName() + " (" + folder.getCount() + ")");

        if (position == selectedPosition) {
            int accentColor = com.example.animelib.util.ThemeUtils.getAccentColorInt(context);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setCornerRadius(com.example.animelib.util.DisplayUtils.dpToPx(context, 10));
            gd.setColor(accentColor);
            holder.tvFolderChip.setBackground(gd);
            holder.tvFolderChip.setTextColor(ContextCompat.getColor(context, R.color.white_color));
        } else {
            holder.tvFolderChip.setBackgroundResource(R.drawable.chip_unselected);
            holder.tvFolderChip.setTextColor(ContextCompat.getColor(context, R.color.player_item_inactive_text));
        }

        holder.tvFolderChip.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onFolderClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void setSelectedPosition(int position) {
        int prev = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(prev);
        notifyItemChanged(selectedPosition);
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolderChip;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderChip = itemView.findViewById(R.id.tvFolderChip);
        }
    }
}
