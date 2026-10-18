package com.example.animelib.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.example.animelib.ui.M3LinearProgressIndicator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.example.animelib.R;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistEpisodesAdapter extends RecyclerView.Adapter<PlaylistEpisodesAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(int position);
    }

    private final List<String> titles;
    private final List<String> paths;
    private int currentIndex;
    private final OnEpisodeClickListener listener;

    private static final int CACHE_SIZE = 20 * 1024 * 1024;
    private static final LruCache<String, Bitmap> thumbnailCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };
    private static final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlaylistEpisodesAdapter(List<String> titles, List<String> paths, int currentIndex, OnEpisodeClickListener listener) {
        this.titles = titles;
        this.paths = paths;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_card, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        String title = position < titles.size() ? titles.get(position) : "Серия " + (position + 1);
        String path = (paths != null && position < paths.size()) ? paths.get(position) : "";

        String cleanTitle = title;
        String extension = "";
        if (title != null && title.contains(".")) {
            int lastDot = title.lastIndexOf('.');
            if (lastDot > 0) {
                cleanTitle = title.substring(0, lastDot);
                if (lastDot < title.length() - 1) {
                    extension = title.substring(lastDot + 1).toUpperCase(java.util.Locale.ROOT);
                }
            }
        }
        if (extension.isEmpty() && !path.isEmpty() && path.contains(".")) {
            int pathDot = path.lastIndexOf('.');
            if (pathDot > 0 && pathDot < path.length() - 1) {
                extension = path.substring(pathDot + 1).toUpperCase(java.util.Locale.ROOT);
            }
        }

        holder.tvVideoTitle.setText(cleanTitle);

        if (holder.tvVideoExtensionTag != null) {
            if (!extension.isEmpty() && extension.length() <= 5) {
                holder.tvVideoExtensionTag.setText(extension);
                holder.tvVideoExtensionTag.setVisibility(View.VISIBLE);
            } else {
                holder.tvVideoExtensionTag.setVisibility(View.GONE);
            }
        }

        // Episode Numbering Badge
        holder.tvSelectionBadge.setVisibility(View.VISIBLE);
        holder.tvSelectionBadge.setText(String.valueOf(position + 1));

        Context context = holder.itemView.getContext();
        VideoPlaybackHistoryEntity history = !path.isEmpty()
                ? DatabaseManager.getInstance(context).getVideoHistory(path)
                : null;

        // Folder & size info
        File file = !path.isEmpty() ? new File(path) : null;
        if (file != null && file.exists()) {
            holder.tvVideoFolder.setText("Эпизод " + (position + 1));
            long sizeMb = file.length() / (1024 * 1024);
            holder.tvVideoSize.setText(sizeMb + " МБ");
        } else {
            holder.tvVideoFolder.setText("Серия");
            holder.tvVideoSize.setText("");
        }

        // Duration badge (bottom-left)
        if (history != null && history.getDurationMs() > 0) {
            long totalSecs = history.getDurationMs() / 1000;
            long hours = totalSecs / 3600;
            long mins = (totalSecs % 3600) / 60;
            long secs = totalSecs % 60;
            String durStr = hours > 0
                    ? String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs)
                    : String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
            holder.tvVideoDuration.setText(durStr);
            holder.tvVideoDuration.setVisibility(View.VISIBLE);
        } else {
            holder.tvVideoDuration.setVisibility(View.GONE);
        }

        // Progress percentage badge (bottom-right) and M3 progress bar (in description)
        if (history != null && history.getProgressPercentage() > 0) {
            holder.pbWatchProgress.setVisibility(View.VISIBLE);
            holder.pbWatchProgress.setProgress(history.getProgressPercentage(), false);
            holder.tvWatchProgressStatus.setVisibility(View.VISIBLE);
            holder.tvWatchProgressStatus.setText(history.getProgressPercentage() + "%");
        } else {
            holder.pbWatchProgress.setVisibility(View.GONE);
            holder.tvWatchProgressStatus.setVisibility(View.GONE);
        }

        boolean isCurrent = (position == currentIndex);

        if (isCurrent) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.purple_primary));
            holder.cardView.setStrokeWidth(dpToPx(context, 1.5f));
            holder.tvVideoTitle.setTextColor(ContextCompat.getColor(context, R.color.purple_primary));
            holder.tvSelectionBadge.setBackgroundResource(R.drawable.bg_m3_selected_pill);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg_color));
        } else {
            holder.cardView.setStrokeWidth(dpToPx(context, 0.8f));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.player_item_inactive_stroke));
            holder.tvVideoTitle.setTextColor(ContextCompat.getColor(context, R.color.primary_text_color));
            holder.tvSelectionBadge.setBackgroundResource(R.drawable.bg_m3_unselected_circle);
        }

        // Thumbnail loading
        if (!path.isEmpty()) {
            holder.ivVideoThumbnail.setTag(path);
            Bitmap cached = thumbnailCache.get(path);
            if (cached != null) {
                holder.ivVideoThumbnail.setImageBitmap(cached);
            } else {
                holder.ivVideoThumbnail.setImageResource(R.drawable.placeholder_image);
                loadThumbnailAsync(context, path, holder.ivVideoThumbnail);
            }
        } else {
            holder.ivVideoThumbnail.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEpisodeClick(position);
            }
        });
    }

    private int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void loadThumbnailAsync(Context context, String path, ImageView imageView) {
        final Context appContext = context.getApplicationContext();
        thumbnailExecutor.execute(() -> {
            Bitmap thumb = com.example.animelib.util.VideoThumbnailHelper.createFrameThumbnail(
                    appContext,
                    path,
                    null,
                    0
            );

            if (thumb != null) {
                final Bitmap finalThumb = thumb;
                thumbnailCache.put(path, finalThumb);
                mainHandler.post(() -> {
                    if (path.equals(imageView.getTag())) {
                        imageView.setImageBitmap(finalThumb);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivVideoThumbnail;
        TextView tvVideoDuration;
        TextView tvVideoTitle;
        TextView tvVideoExtensionTag;
        TextView tvVideoFolder;
        TextView tvVideoSize;
        TextView tvSelectionBadge;
        TextView tvWatchProgressStatus;
        M3LinearProgressIndicator pbWatchProgress;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvVideoExtensionTag = itemView.findViewById(R.id.tvVideoExtensionTag);
            tvVideoFolder = itemView.findViewById(R.id.tvVideoFolder);
            tvVideoSize = itemView.findViewById(R.id.tvVideoSize);
            tvSelectionBadge = itemView.findViewById(R.id.tvSelectionBadge);
            tvWatchProgressStatus = itemView.findViewById(R.id.tvWatchProgressStatus);
            pbWatchProgress = itemView.findViewById(R.id.pbWatchProgress);
        }
    }
}
