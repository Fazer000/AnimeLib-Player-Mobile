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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.example.animelib.R;

import java.io.File;
import java.util.List;
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
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.setMargins(0, params.topMargin, 0, params.bottomMargin);
        view.setLayoutParams(params);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        String title = position < titles.size() ? titles.get(position) : "Серия " + (position + 1);
        String path = (paths != null && position < paths.size()) ? paths.get(position) : "";

        holder.tvVideoTitle.setText(title);

        // Episode Numbering Badge
        holder.tvSelectionBadge.setVisibility(View.VISIBLE);
        holder.tvSelectionBadge.setText(String.valueOf(position + 1));

        // Folder & size info
        File file = !path.isEmpty() ? new File(path) : null;
        if (file != null && file.exists()) {
            holder.tvVideoFolder.setText("Эпизод " + (position + 1));
            long sizeMb = file.length() / (1024 * 1024);
            holder.tvVideoSize.setText(sizeMb + " МБ");
            holder.tvVideoDuration.setVisibility(View.GONE);
        } else {
            holder.tvVideoFolder.setText("Серия");
            holder.tvVideoSize.setText("");
            holder.tvVideoDuration.setVisibility(View.GONE);
        }

        holder.pbWatchProgress.setVisibility(View.GONE);
        holder.tvWatchProgressStatus.setVisibility(View.GONE);

        boolean isCurrent = (position == currentIndex);
        Context context = holder.itemView.getContext();

        if (isCurrent) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.purple_primary));
            holder.cardView.setStrokeWidth(dpToPx(context, 0.5f));
            holder.tvVideoTitle.setTextColor(ContextCompat.getColor(context, R.color.purple_primary));
            holder.tvSelectionBadge.setBackgroundResource(R.drawable.bg_m3_selected_pill);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg_color));
        } else {
            holder.cardView.setStrokeWidth(dpToPx(context, 0.5f));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
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
            Bitmap thumb = null;
            try {
                if (path != null && !path.isEmpty()) {
                    thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
                }
            } catch (Throwable ignored) {}

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
        TextView tvVideoFolder;
        TextView tvVideoSize;
        TextView tvSelectionBadge;
        TextView tvWatchProgressStatus;
        ProgressBar pbWatchProgress;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvVideoFolder = itemView.findViewById(R.id.tvVideoFolder);
            tvVideoSize = itemView.findViewById(R.id.tvVideoSize);
            tvSelectionBadge = itemView.findViewById(R.id.tvSelectionBadge);
            tvWatchProgressStatus = itemView.findViewById(R.id.tvWatchProgressStatus);
            pbWatchProgress = itemView.findViewById(R.id.pbWatchProgress);
        }
    }
}
