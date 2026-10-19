package com.example.animelib.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.adapters.PlaylistsAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.PlaylistEntity;
import com.example.animelib.data.entity.PlaylistVideoEntity;
import com.example.animelib.models.VideoItem;
import com.example.animelib.settings.PlaylistVideosBottomSheet;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.DialogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private RecyclerView rvPlaylists;
    private View layoutPlaylistsEmptyState;
    private View layoutPlaylistsTopBar;
    private View btnCreatePlaylistHeader;
    private View btnEmptyCreatePlaylist;
    private DatabaseManager databaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            updateWindowInsets(statusBarInsets.top, navBarInsets.bottom);
            return insets;
        });

        databaseManager = DatabaseManager.getInstance(requireContext());

        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        layoutPlaylistsEmptyState = view.findViewById(R.id.layoutPlaylistsEmptyState);
        layoutPlaylistsTopBar = view.findViewById(R.id.layoutPlaylistsTopBar);
        btnCreatePlaylistHeader = view.findViewById(R.id.btnCreatePlaylistHeader);
        btnEmptyCreatePlaylist = view.findViewById(R.id.btnEmptyCreatePlaylist);

        if (btnCreatePlaylistHeader != null) {
            btnCreatePlaylistHeader.setOnClickListener(v -> showCreatePlaylistDialog());
        }

        if (btnEmptyCreatePlaylist != null) {
            btnEmptyCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        }

        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadAndDisplayPlaylists();
    }

    public void updateWindowInsets(int topInset, int bottomInset) {
        if (getView() == null || rvPlaylists == null) return;
        float density = getResources().getDisplayMetrics().density;
        int totalPaddingTop = topInset + (int) (64 * density);
        int totalPaddingBottom = bottomInset + (int) (96 * density);
        int horizontalPadding = 0;
        rvPlaylists.setPadding(horizontalPadding, totalPaddingTop, horizontalPadding, totalPaddingBottom);
        rvPlaylists.setClipToPadding(false);
        rvPlaylists.setClipChildren(false);

        if (layoutPlaylistsTopBar != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) layoutPlaylistsTopBar.getLayoutParams();
            lp.topMargin = topInset + (int) (8 * density);
            layoutPlaylistsTopBar.setLayoutParams(lp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayPlaylists();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadAndDisplayPlaylists();
        }
    }

    public void loadAndDisplayPlaylists() {
        if (databaseManager == null || getContext() == null) return;

        List<PlaylistEntity> playlists = databaseManager.getAllPlaylists();
        if (playlists == null || playlists.isEmpty()) {
            layoutPlaylistsEmptyState.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
            return;
        }

        layoutPlaylistsEmptyState.setVisibility(View.GONE);
        rvPlaylists.setVisibility(View.VISIBLE);

        PlaylistsAdapter adapter = new PlaylistsAdapter(playlists, new PlaylistsAdapter.OnPlaylistListener() {
            @Override
            public void onPlaylistClick(PlaylistEntity playlist) {
                openPlaylistBottomSheet(playlist);
            }

            @Override
            public void onPlaylistDelete(PlaylistEntity playlist) {
                openPlaylistBottomSheet(playlist);
            }
        });
        rvPlaylists.setAdapter(adapter);
    }

    public void showCreatePlaylistDialog() {
        if (!isAdded() || getContext() == null) return;

        DialogUtils.showInputDialog(
                requireContext(),
                "Новый плейлист",
                "Введите название нового плейлиста:",
                "",
                "Название плейлиста",
                "Создать",
                "Отмена",
                new DialogUtils.InputDialogListener() {
                    @Override
                    public void onInputConfirmed(String input) {
                        String name = input != null ? input.trim() : "";
                        if (name.isEmpty()) {
                            CustomToast.showWarning(requireContext(), "Название не может быть пустым");
                            return;
                        }
                        long playlistId = databaseManager.createPlaylist(name, new ArrayList<>());
                        if (playlistId > 0) {
                            CustomToast.showSuccess(requireContext(), "Плейлист «" + name + "» создан");
                            loadAndDisplayPlaylists();
                            PlaylistEntity newEntity = databaseManager.getPlaylistById(playlistId);
                            if (newEntity != null) {
                                openPlaylistBottomSheet(newEntity);
                            }
                        } else {
                            CustomToast.showWarning(requireContext(), "Не удалось создать плейлист");
                        }
                    }

                    @Override
                    public void onCancelled() {}
                }
        );
    }

    private void openPlaylistBottomSheet(PlaylistEntity playlist) {
        if (playlist == null || getContext() == null) return;
        PlaylistVideosBottomSheet bs = new PlaylistVideosBottomSheet(requireContext(), playlist);
        bs.setPlaylistActionListener(new PlaylistVideosBottomSheet.PlaylistActionListener() {
            @Override
            public void onPlaylistUpdated(PlaylistEntity updatedPlaylist) {
                PlaylistsAdapter.invalidatePlaylist(updatedPlaylist.getId());
                loadAndDisplayPlaylists();
            }

            @Override
            public void onPlaylistDeleted(PlaylistEntity deletedPlaylist) {
                PlaylistsAdapter.invalidatePlaylist(deletedPlaylist.getId());
                loadAndDisplayPlaylists();
            }
        });
        bs.setOnDismissListener(dialog -> {
            PlaylistsAdapter.invalidatePlaylist(playlist.getId());
            loadAndDisplayPlaylists();
        });
        bs.show();
    }

    private void loadAndPlaySavedPlaylist(PlaylistEntity playlist) {
        if (playlist == null) return;

        List<PlaylistVideoEntity> videoEntities = databaseManager.getVideosForPlaylist(playlist.getId());
        if (videoEntities == null || videoEntities.isEmpty()) {
            CustomToast.showWarning(requireContext(), "Плейлист пуст");
            return;
        }

        ArrayList<VideoItem> playlistVideos = new ArrayList<>();
        long tempId = 1;
        for (PlaylistVideoEntity entity : videoEntities) {
            String path = entity.getVideoPath();
            File file = new File(path);
            String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
            playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), "Плейлист"));
        }

        if (playlistVideos.isEmpty()) {
            CustomToast.showWarning(requireContext(), "Файлы плейлиста не найдены");
            return;
        }

        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.putExtra("VIDEO_LIST", playlistVideos);
        intent.putExtra("START_INDEX", 0);
        intent.putExtra("PLAYLIST_NAME", playlist.getName());
        intent.putExtra("EXTRA_PLAYLIST_ID", playlist.getId());
        startActivity(intent);
    }
}
