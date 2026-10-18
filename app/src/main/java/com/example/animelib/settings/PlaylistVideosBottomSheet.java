package com.example.animelib.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.adapters.PlaylistEpisodesAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.models.VideoItem;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistVideosBottomSheet extends FlexibleBottomSheetDialog {

    private final PlaylistEntity playlist;
    private final DatabaseManager databaseManager;

    public PlaylistVideosBottomSheet(@NonNull Context context, @NonNull PlaylistEntity playlist) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.playlist = playlist;
        this.databaseManager = DatabaseManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_playlist_videos, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this, true, R.drawable.bs_background);

        ImageButton btnClose = view.findViewById(R.id.btnClosePlaylistBs);
        TextView tvTitle = view.findViewById(R.id.tvBsPlaylistTitle);
        TextView tvCount = view.findViewById(R.id.tvBsPlaylistCount);
        View layoutEmpty = view.findViewById(R.id.layoutBsPlaylistEmpty);
        RecyclerView rvEpisodes = view.findViewById(R.id.rvBsPlaylistEpisodes);

        btnClose.setOnClickListener(v -> dismiss());

        String playlistName = playlist.getName() != null ? playlist.getName() : "Плейлист";
        tvTitle.setText(playlistName);

        List<PlaylistVideoEntity> videoEntities = databaseManager.getVideosForPlaylist(playlist.getId());
        if (videoEntities == null || videoEntities.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvEpisodes.setVisibility(View.GONE);
            tvCount.setText("0 видео");
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        rvEpisodes.setVisibility(View.VISIBLE);
        tvCount.setText(videoEntities.size() + " видео");

        List<String> titles = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < videoEntities.size(); i++) {
            PlaylistVideoEntity entity = videoEntities.get(i);
            String path = entity.getVideoPath();
            String title = entity.getVideoTitle();
            if (title == null || title.trim().isEmpty()) {
                title = new File(path).getName();
            }
            titles.add(title);
            paths.add(path);
        }

        rvEpisodes.setLayoutManager(new LinearLayoutManager(getContext()));
        PlaylistEpisodesAdapter adapter = new PlaylistEpisodesAdapter(titles, paths, -1, position -> {
            ArrayList<VideoItem> playlistVideos = new ArrayList<>();
            long tempId = 1;
            for (PlaylistVideoEntity entity : videoEntities) {
                String path = entity.getVideoPath();
                File file = new File(path);
                String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
                playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), playlistName));
            }

            if (playlistVideos.isEmpty()) {
                CustomToast.showWarning(getContext(), "Видео не найдены");
                return;
            }

            Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
            intent.putExtra("VIDEO_LIST", playlistVideos);
            intent.putExtra("START_INDEX", position);
            intent.putExtra("PLAYLIST_NAME", playlistName);
            intent.putExtra("EXTRA_PLAYLIST_ID", playlist.getId());
            getContext().startActivity(intent);
            dismiss();
        });
        rvEpisodes.setAdapter(adapter);
    }
}
