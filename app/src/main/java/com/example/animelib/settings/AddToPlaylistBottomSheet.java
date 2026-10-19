package com.example.animelib.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.models.VideoItem;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.DialogUtils;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;
import com.example.animelib.util.VideoThumbnailHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BottomSheet в Material 3 стиле для добавления видеофайла в существующий или новый плейлист.
 */
public class AddToPlaylistBottomSheet extends FlexibleBottomSheetDialog {

    public interface PlaylistAddedCallback {
        void onPlaylistUpdated(String playlistName);
    }

    private final VideoItem video;
    private final PlaylistAddedCallback callback;
    private final DatabaseManager databaseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProgressBar pbLoading;
    private RecyclerView rvPlaylists;
    private View layoutEmpty;
    private TextView tvSectionTitle;
    private PlaylistsAdapter adapter;

    public AddToPlaylistBottomSheet(@NonNull Context context,
                                    @NonNull VideoItem video,
                                    @Nullable PlaylistAddedCallback callback) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.video = video;
        this.callback = callback;
        this.databaseManager = DatabaseManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_add_to_playlist, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this, true, R.drawable.bs_background);

        ImageButton btnClose = view.findViewById(R.id.btnCloseAddToPlaylist);
        TextView tvTargetTitle = view.findViewById(R.id.tvBsVideoTargetTitle);
        View cardCreateNew = view.findViewById(R.id.cardCreateNewPlaylist);

        pbLoading = view.findViewById(R.id.pbLoadingPlaylists);
        rvPlaylists = view.findViewById(R.id.rvPlaylistsList);
        layoutEmpty = view.findViewById(R.id.layoutEmptyPlaylists);
        tvSectionTitle = view.findViewById(R.id.tvPlaylistsSectionTitle);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        if (tvTargetTitle != null) {
            String title = video.getDisplayTitle();
            tvTargetTitle.setText("Видео: " + (title != null && !title.isEmpty() ? title : "видеофайл"));
        }

        if (cardCreateNew != null) {
            cardCreateNew.setOnClickListener(v -> promptCreateNewPlaylist());
        }

        if (rvPlaylists != null) {
            rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new PlaylistsAdapter();
            rvPlaylists.setAdapter(adapter);
        }

        loadPlaylistsData();
    }

    private void promptCreateNewPlaylist() {
        DialogUtils.showInputDialog(getContext(), "Новый плейлист", "Введите название плейлиста:", "", "Название", "Создать", "Отмена", new DialogUtils.InputDialogListener() {
            @Override
            public void onInputConfirmed(String input) {
                if (input == null || input.trim().isEmpty()) {
                    CustomToast.showWarning(getContext(), "Имя плейлиста не может быть пустым");
                    return;
                }

                final String playlistName = input.trim();
                executor.execute(() -> {
                    ArrayList<VideoItem> single = new ArrayList<>();
                    single.add(video);
                    long newId = databaseManager.createPlaylist(playlistName, single);

                    mainHandler.post(() -> {
                        if (newId > 0) {
                            CustomToast.showSuccess(getContext(), "Плейлист «" + playlistName + "» создан и видео добавлено");
                            if (callback != null) {
                                callback.onPlaylistUpdated(playlistName);
                            }
                            dismiss();
                        } else {
                            CustomToast.showWarning(getContext(), "Не удалось создать плейлист");
                        }
                    });
                });
            }

            @Override
            public void onCancelled() {}
        });
    }

    private void loadPlaylistsData() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (rvPlaylists != null) rvPlaylists.setVisibility(View.GONE);

        final String targetPath = video.getPath() != null && !video.getPath().isEmpty()
                ? video.getPath()
                : video.getUriString();

        executor.execute(() -> {
            List<PlaylistEntity> playlists = databaseManager.getAllPlaylists();
            Map<Long, Integer> counts = new HashMap<>();
            Set<Long> containingPlaylistIds = new HashSet<>();
            Map<Long, String> firstVideoPathMap = new HashMap<>();

            if (playlists != null) {
                for (PlaylistEntity p : playlists) {
                    List<PlaylistVideoEntity> pVideos = databaseManager.getVideosForPlaylist(p.getId());
                    int count = pVideos != null ? pVideos.size() : 0;
                    counts.put(p.getId(), count);

                    if (pVideos != null && !pVideos.isEmpty()) {
                        firstVideoPathMap.put(p.getId(), pVideos.get(0).getVideoPath());
                        for (PlaylistVideoEntity ve : pVideos) {
                            if (targetPath != null && (targetPath.equals(ve.getVideoPath())
                                    || (video.getUriString() != null && video.getUriString().equals(ve.getVideoPath())))) {
                                containingPlaylistIds.add(p.getId());
                                break;
                            }
                        }
                    }
                }
            }

            mainHandler.post(() -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                if (playlists == null || playlists.isEmpty()) {
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                    if (rvPlaylists != null) rvPlaylists.setVisibility(View.GONE);
                    if (tvSectionTitle != null) tvSectionTitle.setVisibility(View.GONE);
                } else {
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
                    if (rvPlaylists != null) rvPlaylists.setVisibility(View.VISIBLE);
                    if (tvSectionTitle != null) {
                        tvSectionTitle.setVisibility(View.VISIBLE);
                        tvSectionTitle.setText("Ваши плейлисты (" + playlists.size() + ")");
                    }
                    if (adapter != null) {
                        adapter.setData(playlists, counts, containingPlaylistIds, firstVideoPathMap);
                    }
                }
            });
        });
    }

    private void onPlaylistClicked(PlaylistEntity playlist, boolean alreadyIn) {
        if (alreadyIn) {
            CustomToast.showInfo(getContext(), "Видео уже в плейлисте «" + playlist.getName() + "»");
            return;
        }

        final String path = video.getPath() != null && !video.getPath().isEmpty() ? video.getPath() : video.getUriString();
        executor.execute(() -> {
            databaseManager.addVideoToPlaylist(playlist.getId(), path, video.getTitle());
            mainHandler.post(() -> {
                CustomToast.showSuccess(getContext(), "Добавлено в «" + playlist.getName() + "»");
                if (callback != null) {
                    callback.onPlaylistUpdated(playlist.getName());
                }
                dismiss();
            });
        });
    }

    // =========================================================================
    // Material 3 RecyclerView Adapter
    // =========================================================================
    private class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder> {

        private List<PlaylistEntity> items = Collections.emptyList();
        private Map<Long, Integer> counts = Collections.emptyMap();
        private Set<Long> containingIds = Collections.emptySet();
        private Map<Long, String> firstVideoPaths = Collections.emptyMap();

        void setData(List<PlaylistEntity> items,
                     Map<Long, Integer> counts,
                     Set<Long> containingIds,
                     Map<Long, String> firstVideoPaths) {
            this.items = items != null ? items : Collections.emptyList();
            this.counts = counts != null ? counts : Collections.emptyMap();
            this.containingIds = containingIds != null ? containingIds : Collections.emptySet();
            this.firstVideoPaths = firstVideoPaths != null ? firstVideoPaths : Collections.emptyMap();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_to_playlist_m3, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlaylistEntity playlist = items.get(position);
            long id = playlist.getId();
            int count = counts.containsKey(id) ? counts.get(id) : 0;
            boolean alreadyIn = containingIds.contains(id);

            // Set grouped M3 item background depending on position (top, middle, bottom, single)
            if (getItemCount() == 1) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_single);
            } else if (position == 0) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_top);
            } else if (position == getItemCount() - 1) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_bottom);
            } else {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_middle);
            }

            holder.tvName.setText(playlist.getName());
            holder.tvCount.setText(count + " видео");

            // M3 checkmark pill (active) vs unselected circle (inactive) matching ThemeSelectionBottomSheet
            if (alreadyIn) {
                holder.selectedPill.setVisibility(View.VISIBLE);
                holder.unselectedCircle.setVisibility(View.GONE);
            } else {
                holder.selectedPill.setVisibility(View.GONE);
                holder.unselectedCircle.setVisibility(View.VISIBLE);
            }

            // Thumbnail loading
            String firstPath = firstVideoPaths.get(id);
            String thumbKey = "playlist_thumb_" + id;
            holder.ivThumbnail.setTag(thumbKey);

            Bitmap cachedThumb = VideoThumbnailHelper.getCachedThumbnail(thumbKey);
            if (cachedThumb != null && !cachedThumb.isRecycled()) {
                holder.ivThumbnail.setImageBitmap(cachedThumb);
                holder.ivFallback.setVisibility(View.GONE);
            } else if (firstPath != null && !firstPath.isEmpty()) {
                holder.ivThumbnail.setImageResource(R.drawable.placeholder_image);
                holder.ivFallback.setVisibility(View.GONE);

                final Context appContext = getContext().getApplicationContext();
                executor.execute(() -> {
                    Uri uri = firstPath.startsWith("content://") ? Uri.parse(firstPath) : null;
                    Bitmap thumb = VideoThumbnailHelper.createFrameThumbnail(appContext, firstPath, uri, 0);
                    if (thumb != null) {
                        VideoThumbnailHelper.putCachedThumbnail(thumbKey, thumb);
                        mainHandler.post(() -> {
                            if (thumbKey.equals(holder.ivThumbnail.getTag())) {
                                holder.ivThumbnail.setImageBitmap(thumb);
                                holder.ivFallback.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.placeholder_image);
                holder.ivFallback.setVisibility(View.VISIBLE);
            }

            holder.itemContainer.setOnClickListener(v -> onPlaylistClicked(playlist, alreadyIn));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View itemContainer;
            final ImageView ivThumbnail;
            final ImageView ivFallback;
            final TextView tvName;
            final TextView tvCount;
            final View selectedPill;
            final View unselectedCircle;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemContainer = itemView.findViewById(R.id.itemContainer);
                ivThumbnail = itemView.findViewById(R.id.ivItemPlaylistThumbnail);
                ivFallback = itemView.findViewById(R.id.ivItemPlaylistFallback);
                tvName = itemView.findViewById(R.id.tvItemPlaylistName);
                tvCount = itemView.findViewById(R.id.tvItemPlaylistCount);
                selectedPill = itemView.findViewById(R.id.itemSelectedPill);
                unselectedCircle = itemView.findViewById(R.id.itemUnselectedCircle);
            }
        }
    }
}
