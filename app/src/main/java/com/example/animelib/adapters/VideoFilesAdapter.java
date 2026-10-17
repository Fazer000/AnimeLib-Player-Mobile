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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.VideoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoFilesAdapter extends RecyclerView.Adapter<VideoFilesAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video, int position);
    }

    private final List<VideoItem> videos;
    private final OnVideoClickListener listener;

    private boolean isSelectionMode = false;
    private final List<VideoItem> selectedVideos = new ArrayList<>();

    private static final int CACHE_SIZE = 40 * 1024 * 1024; // 40MB memory cache
    private static final LruCache<String, Bitmap> thumbnailCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    private static final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VideoFilesAdapter(List<VideoItem> videos, OnVideoClickListener listener) {
        this.videos = videos;
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedVideos.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public List<VideoItem> getSelectedVideos() {
        return selectedVideos;
    }

    public void toggleSelection(VideoItem video) {
        int index = -1;
        for (int i = 0; i < selectedVideos.size(); i++) {
            VideoItem item = selectedVideos.get(i);
            if (item.getId() == video.getId() || (item.getPath() != null && item.getPath().equals(video.getPath()))) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            selectedVideos.remove(index);
        } else {
            selectedVideos.add(video);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videos.get(position);

        holder.tvVideoTitle.setText(video.getTitle());
        holder.tvVideoDuration.setText(video.getFormattedDuration());
        holder.tvVideoFolder.setText(video.getFolderName());
        String sizeStr = video.getFormattedSize();
        String dateStr = video.getFormattedDate();
        if (!sizeStr.isEmpty() && !dateStr.isEmpty()) {
            holder.tvVideoSize.setText(sizeStr + " • " + dateStr);
        } else if (!sizeStr.isEmpty()) {
            holder.tvVideoSize.setText(sizeStr);
        } else {
            holder.tvVideoSize.setText(dateStr);
        }

        if (video.getProgressPercent() > 0) {
            holder.pbWatchProgress.setVisibility(View.VISIBLE);
            holder.pbWatchProgress.setProgress(video.getProgressPercent());
            holder.tvWatchProgressStatus.setVisibility(View.VISIBLE);
            holder.tvWatchProgressStatus.setText(video.getProgressPercent() + "%");
        } else {
            holder.pbWatchProgress.setVisibility(View.GONE);
            holder.tvWatchProgressStatus.setVisibility(View.GONE);
        }

        int selectionIndex = -1;
        if (isSelectionMode) {
            for (int i = 0; i < selectedVideos.size(); i++) {
                VideoItem item = selectedVideos.get(i);
                if (item.getId() == video.getId() || (item.getPath() != null && item.getPath().equals(video.getPath()))) {
                    selectionIndex = i;
                    break;
                }
            }
        }

        if (selectionIndex >= 0) {
            holder.tvSelectionBadge.setVisibility(View.VISIBLE);
            holder.tvSelectionBadge.setText(String.valueOf(selectionIndex + 1));
            holder.itemView.setAlpha(1.0f);
        } else if (isSelectionMode) {
            holder.tvSelectionBadge.setVisibility(View.VISIBLE);
            holder.tvSelectionBadge.setText("");
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.tvSelectionBadge.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
        }

        // Thumbnail loading
        String cacheKey = video.getUriString() != null && !video.getUriString().isEmpty()
                ? video.getUriString() : video.getPath();
        holder.ivVideoThumbnail.setTag(cacheKey);

        Bitmap cached = thumbnailCache.get(cacheKey);
        if (cached != null) {
            holder.ivVideoThumbnail.setImageBitmap(cached);
        } else {
            holder.ivVideoThumbnail.setImageResource(R.drawable.placeholder_image);
            loadThumbnailAsync(holder.itemView.getContext(), video, cacheKey, holder.ivVideoThumbnail);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onVideoClick(video, pos);
                }
            }
        });
    }

    private void loadThumbnailAsync(Context context, VideoItem video, String cacheKey, ImageView imageView) {
        final Context appContext = context.getApplicationContext();
        thumbnailExecutor.execute(() -> {
            Bitmap thumb = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && video.getUri() != null) {
                    thumb = appContext.getContentResolver().loadThumbnail(
                            video.getUri(),
                            new Size(320, 180),
                            null
                    );
                }
            } catch (Throwable ignored) {
            }

            if (thumb == null && video.getPath() != null) {
                try {
                    File file = new File(video.getPath());
                    if (file.exists()) {
                        thumb = ThumbnailUtils.createVideoThumbnail(video.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                    }
                } catch (Throwable ignored) {
                }
            }

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

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVideoThumbnail;
        TextView tvVideoDuration;
        TextView tvVideoTitle;
        TextView tvVideoFolder;
        TextView tvVideoSize;
        TextView tvWatchProgressStatus;
        TextView tvSelectionBadge;
        ProgressBar pbWatchProgress;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvVideoFolder = itemView.findViewById(R.id.tvVideoFolder);
            tvVideoSize = itemView.findViewById(R.id.tvVideoSize);
            tvWatchProgressStatus = itemView.findViewById(R.id.tvWatchProgressStatus);
            tvSelectionBadge = itemView.findViewById(R.id.tvSelectionBadge);
            pbWatchProgress = itemView.findViewById(R.id.pbWatchProgress);
        }
    }
}
