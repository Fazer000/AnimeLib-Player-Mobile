package com.example.animelib.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.VideoPlaybackHistoryEntity;
import com.example.animelib.models.VideoItem;
import com.example.animelib.ui.M3LinearProgressIndicator;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistEpisodesAdapter extends RecyclerView.Adapter<PlaylistEpisodesAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(int position);
        default void onEpisodeMoreClick(VideoItem video, int position) {}
    }

    private final List<VideoItem> videos = new ArrayList<>();
    private int currentIndex;
    private final OnEpisodeClickListener listener;

    private static final int CACHE_SIZE = 30 * 1024 * 1024;
    private static final LruCache<String, Bitmap> thumbnailCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };
    private static final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlaylistEpisodesAdapter(List<String> titles, List<String> paths, int currentIndex, OnEpisodeClickListener listener) {
        this.currentIndex = currentIndex;
        this.listener = listener;
        if (titles != null && paths != null) {
            for (int i = 0; i < paths.size(); i++) {
                String path = paths.get(i);
                String title = i < titles.size() ? titles.get(i) : "Серия " + (i + 1);
                File file = (path != null && !path.isEmpty()) ? new File(path) : null;
                long size = file != null && file.exists() ? file.length() : 0;
                long modified = file != null && file.exists() ? file.lastModified() / 1000L : 0;
                String folder = file != null && file.getParentFile() != null ? file.getParentFile().getName() : "Серия " + (i + 1);
                this.videos.add(new VideoItem(i, title, path, null, 0, size, modified, folder));
            }
        }
    }

    public PlaylistEpisodesAdapter(List<VideoItem> initialVideos, int currentIndex, OnEpisodeClickListener listener) {
        this.currentIndex = currentIndex;
        this.listener = listener;
        if (initialVideos != null) {
            this.videos.addAll(initialVideos);
        }
    }

    public void updateVideoList(List<VideoItem> newVideos) {
        this.videos.clear();
        if (newVideos != null) {
            this.videos.addAll(newVideos);
        }
        notifyDataSetChanged();
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
        if (position < 0 || position >= videos.size()) return;
        VideoItem video = videos.get(position);
        Context context = holder.itemView.getContext();
        boolean isCurrent = (position == currentIndex);

        // Transparent background & no stroke (identical to VideoFilesAdapter)
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        holder.cardView.setStrokeWidth(0);

        // Title
        holder.tvVideoTitle.setText(video.getDisplayTitle());
        if (isCurrent) {
            holder.tvVideoTitle.setTextColor(ContextCompat.getColor(context, R.color.purple_primary));
        } else {
            holder.tvVideoTitle.setTextColor(ContextCompat.getColor(context, R.color.primary_text_color));
        }

        // "НОВОЕ" Tag
        if (holder.tvVideoNewTag != null) {
            holder.tvVideoNewTag.setVisibility(video.isNew() ? View.VISIBLE : View.GONE);
        }

        // Extension Tag
        String extension = video.getFileExtension();
        if (holder.tvVideoExtensionTag != null) {
            if (!extension.isEmpty() && extension.length() <= 5) {
                holder.tvVideoExtensionTag.setText(extension);
                holder.tvVideoExtensionTag.setVisibility(View.VISIBLE);
            } else {
                holder.tvVideoExtensionTag.setVisibility(View.GONE);
            }
        }

        // Selection / Numbering badge
        if (holder.tvSelectionBadge != null) {
            holder.tvSelectionBadge.setVisibility(View.GONE);
        }

        // Fetch watch history if available
        VideoPlaybackHistoryEntity history = (video.getPath() != null && !video.getPath().isEmpty())
                ? DatabaseManager.getInstance(context).getVideoHistory(video.getPath())
                : null;

        // Folder & size/date info
        holder.tvVideoFolder.setText(video.getFolderName() != null ? video.getFolderName() : "Серия " + (position + 1));

        String sizeStr = video.getFormattedSize();
        String dateStr = video.getFormattedDate();
        if (sizeStr.isEmpty() && video.getPath() != null) {
            File f = new File(video.getPath());
            if (f.exists()) {
                long sizeBytes = f.length();
                double mb = sizeBytes / (1024.0 * 1024.0);
                double gb = mb / 1024.0;
                sizeStr = gb >= 1.0 ? String.format(Locale.getDefault(), "%.1f ГБ", gb) : String.format(Locale.getDefault(), "%.0f МБ", mb);
                if (f.lastModified() > 0) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        dateStr = sdf.format(new java.util.Date(f.lastModified()));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (!sizeStr.isEmpty() && !dateStr.isEmpty()) {
            holder.tvVideoSize.setText(sizeStr + " • " + dateStr);
        } else if (!sizeStr.isEmpty()) {
            holder.tvVideoSize.setText(sizeStr);
        } else {
            holder.tvVideoSize.setText(dateStr);
        }

        // Duration badge (bottom-left)
        long durationMs = video.getDurationMs();
        if (durationMs <= 0 && history != null) {
            durationMs = history.getDurationMs();
        }
        if (durationMs > 0) {
            long totalSecs = durationMs / 1000;
            long hours = totalSecs / 3600;
            long mins = (totalSecs % 3600) / 60;
            long secs = totalSecs % 60;
            String durStr = hours > 0
                    ? String.format(Locale.getDefault(), "%d:%02d:%02d", hours, mins, secs)
                    : String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
            holder.tvVideoDuration.setText(durStr);
            holder.tvVideoDuration.setVisibility(View.VISIBLE);
        } else {
            holder.tvVideoDuration.setText("--:--");
            holder.tvVideoDuration.setVisibility(View.VISIBLE);
        }

        // Progress percentage badge & M3 progress bar
        int progressPercent = video.getProgressPercent();
        if (progressPercent <= 0 && history != null) {
            progressPercent = history.getProgressPercentage();
        }
        if (progressPercent > 0) {
            holder.pbWatchProgress.setVisibility(View.VISIBLE);
            holder.pbWatchProgress.setProgress(progressPercent, false);
            holder.tvWatchProgressStatus.setVisibility(View.VISIBLE);
            holder.tvWatchProgressStatus.setText(progressPercent + "%");
        } else {
            holder.pbWatchProgress.setVisibility(View.GONE);
            holder.tvWatchProgressStatus.setVisibility(View.GONE);
        }

        // 3-dots More Button
        if (holder.btnVideoMore != null) {
            holder.btnVideoMore.setVisibility(View.VISIBLE);
            holder.btnVideoMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEpisodeMoreClick(video, position);
                }
            });
        }

        // Thumbnail loading
        String cacheKey = (video.getUriString() != null && !video.getUriString().isEmpty())
                ? video.getUriString() : video.getPath();
        if (cacheKey != null && !cacheKey.isEmpty()) {
            holder.ivVideoThumbnail.setTag(cacheKey);
            Bitmap cached = thumbnailCache.get(cacheKey);
            if (cached != null) {
                holder.ivVideoThumbnail.setImageBitmap(cached);
            } else {
                holder.ivVideoThumbnail.setImageResource(R.drawable.placeholder_image);
                loadThumbnailAsync(context, video, cacheKey, holder.ivVideoThumbnail);
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

    private void loadThumbnailAsync(Context context, VideoItem video, String cacheKey, ImageView imageView) {
        final Context appContext = context.getApplicationContext();
        thumbnailExecutor.execute(() -> {
            Bitmap thumb = com.example.animelib.util.VideoThumbnailHelper.createFrameThumbnail(
                    appContext,
                    video.getPath(),
                    video.getUri(),
                    video.getDurationMs()
            );

            if (thumb != null) {
                final Bitmap finalThumb = thumb;
                thumbnailCache.put(cacheKey, finalThumb);
                mainHandler.post(() -> {
                    if (cacheKey.equals(imageView.getTag())) {
                        imageView.setImageBitmap(finalThumb);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivVideoThumbnail;
        TextView tvVideoDuration;
        TextView tvVideoTitle;
        TextView tvVideoNewTag;
        TextView tvVideoExtensionTag;
        TextView tvVideoFolder;
        TextView tvVideoSize;
        TextView tvSelectionBadge;
        TextView tvWatchProgressStatus;
        M3LinearProgressIndicator pbWatchProgress;
        ImageButton btnVideoMore;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvVideoNewTag = itemView.findViewById(R.id.tvVideoNewTag);
            tvVideoExtensionTag = itemView.findViewById(R.id.tvVideoExtensionTag);
            tvVideoFolder = itemView.findViewById(R.id.tvVideoFolder);
            tvVideoSize = itemView.findViewById(R.id.tvVideoSize);
            tvSelectionBadge = itemView.findViewById(R.id.tvSelectionBadge);
            tvWatchProgressStatus = itemView.findViewById(R.id.tvWatchProgressStatus);
            pbWatchProgress = itemView.findViewById(R.id.pbWatchProgress);
            btnVideoMore = itemView.findViewById(R.id.btnVideoMore);
        }
    }
}
