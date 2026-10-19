package com.example.animelib.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
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
import com.example.animelib.util.DialogUtils;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistVideosBottomSheet extends FlexibleBottomSheetDialog {

    public interface PlaylistActionListener {
        void onPlaylistUpdated(PlaylistEntity playlist);
        void onPlaylistDeleted(PlaylistEntity playlist);
    }

    private final PlaylistEntity playlist;
    private final DatabaseManager databaseManager;
    private PlaylistActionListener actionListener;

    private TextView tvTitle;
    private TextView tvCount;
    private View layoutEmpty;
    private RecyclerView rvEpisodes;
    private List<PlaylistVideoEntity> currentVideos = new ArrayList<>();

    public PlaylistVideosBottomSheet(@NonNull Context context, @NonNull PlaylistEntity playlist) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.playlist = playlist;
        this.databaseManager = DatabaseManager.getInstance(context);
    }

    public void setPlaylistActionListener(PlaylistActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_playlist_videos, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this, true, R.drawable.bs_background);

        ImageButton btnClose = view.findViewById(R.id.btnClosePlaylistBs);
        ImageButton btnMenu = view.findViewById(R.id.btnBsPlaylistMenu);
        tvTitle = view.findViewById(R.id.tvBsPlaylistTitle);
        tvCount = view.findViewById(R.id.tvBsPlaylistCount);
        layoutEmpty = view.findViewById(R.id.layoutBsPlaylistEmpty);
        rvEpisodes = view.findViewById(R.id.rvBsPlaylistEpisodes);

        btnClose.setOnClickListener(v -> dismiss());

        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showPopupMenu);
        }

        String playlistName = playlist.getName() != null ? playlist.getName() : "Плейлист";
        tvTitle.setText(playlistName);

        rvEpisodes.setLayoutManager(new LinearLayoutManager(getContext()));
        loadVideos();
    }

    private void loadVideos() {
        currentVideos = databaseManager.getVideosForPlaylist(playlist.getId());
        if (currentVideos == null || currentVideos.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvEpisodes.setVisibility(View.GONE);
            tvCount.setText("0 видео");
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        rvEpisodes.setVisibility(View.VISIBLE);
        tvCount.setText(currentVideos.size() + " видео");

        List<String> titles = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < currentVideos.size(); i++) {
            PlaylistVideoEntity entity = currentVideos.get(i);
            String path = entity.getVideoPath();
            String title = entity.getVideoTitle();
            if (title == null || title.trim().isEmpty()) {
                title = new File(path).getName();
            }
            titles.add(title);
            paths.add(path);
        }

        PlaylistEpisodesAdapter adapter = new PlaylistEpisodesAdapter(titles, paths, -1, new PlaylistEpisodesAdapter.OnEpisodeClickListener() {
            @Override
            public void onEpisodeClick(int position) {
                ArrayList<VideoItem> playlistVideos = new ArrayList<>();
                long tempId = 1;
                for (PlaylistVideoEntity entity : currentVideos) {
                    String path = entity.getVideoPath();
                    File file = new File(path);
                    String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
                    playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), playlist.getName()));
                }

                if (playlistVideos.isEmpty()) {
                    CustomToast.showWarning(getContext(), "Видео не найдены");
                    return;
                }

                Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                intent.putExtra("VIDEO_LIST", playlistVideos);
                intent.putExtra("START_INDEX", position);
                intent.putExtra("PLAYLIST_NAME", playlist.getName());
                intent.putExtra("EXTRA_PLAYLIST_ID", playlist.getId());
                getContext().startActivity(intent);
                dismiss();
            }

            @Override
            public void onEpisodeMoreClick(VideoItem video, int position) {
                showRemoveVideoDialog(video);
            }
        });
        rvEpisodes.setAdapter(adapter);
    }

    private void showPopupMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_playlist_options, popup.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true);
        } else {
            try {
                java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popup);
                if (menuPopupHelper != null) {
                    java.lang.reflect.Method setForceIcons = menuPopupHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                }
            } catch (Exception ignored) {}
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_playlist_play_all) {
                playAllVideos();
                return true;
            } else if (id == R.id.action_playlist_rename) {
                showRenamePlaylistDialog();
                return true;
            } else if (id == R.id.action_playlist_clear) {
                showClearPlaylistDialog();
                return true;
            } else if (id == R.id.action_playlist_delete) {
                showDeletePlaylistDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void playAllVideos() {
        if (currentVideos == null || currentVideos.isEmpty()) {
            CustomToast.showWarning(getContext(), "Плейлист пуст");
            return;
        }
        ArrayList<VideoItem> playlistVideos = new ArrayList<>();
        long tempId = 1;
        for (PlaylistVideoEntity entity : currentVideos) {
            String path = entity.getVideoPath();
            File file = new File(path);
            String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
            playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), playlist.getName()));
        }

        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        intent.putExtra("VIDEO_LIST", playlistVideos);
        intent.putExtra("START_INDEX", 0);
        intent.putExtra("PLAYLIST_NAME", playlist.getName());
        intent.putExtra("EXTRA_PLAYLIST_ID", playlist.getId());
        getContext().startActivity(intent);
        dismiss();
    }

    private void showRenamePlaylistDialog() {
        DialogUtils.showInputDialog(
                getContext(),
                "Переименовать плейлист",
                "Введите новое название плейлиста:",
                playlist.getName(),
                "Название плейлиста",
                "Сохранить",
                "Отмена",
                new DialogUtils.InputDialogListener() {
                    @Override
                    public void onInputConfirmed(String input) {
                        String newName = input != null ? input.trim() : "";
                        if (newName.isEmpty()) {
                            CustomToast.showWarning(getContext(), "Название не может быть пустым");
                            return;
                        }
                        databaseManager.renamePlaylist(playlist.getId(), newName);
                        playlist.setName(newName);
                        if (tvTitle != null) {
                            tvTitle.setText(newName);
                        }
                        CustomToast.showSuccess(getContext(), "Плейлист переименован");
                        if (actionListener != null) {
                            actionListener.onPlaylistUpdated(playlist);
                        }
                    }

                    @Override
                    public void onCancelled() {}
                }
        );
    }

    private void showClearPlaylistDialog() {
        if (currentVideos == null || currentVideos.isEmpty()) {
            CustomToast.showInfo(getContext(), "Плейлист уже пуст");
            return;
        }
        DialogUtils.showConfirmDialog(
                getContext(),
                "Очистить плейлист",
                "Удалить все видео из плейлиста «" + playlist.getName() + "»?",
                "Очистить",
                "Отмена",
                true,
                new DialogUtils.DialogListener() {
                    @Override
                    public void onPositiveClick() {
                        databaseManager.clearPlaylistVideos(playlist.getId());
                        currentVideos.clear();
                        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                        if (rvEpisodes != null) rvEpisodes.setVisibility(View.GONE);
                        if (tvCount != null) tvCount.setText("0 видео");
                        CustomToast.showSuccess(getContext(), "Плейлист очищен");
                        if (actionListener != null) {
                            actionListener.onPlaylistUpdated(playlist);
                        }
                    }

                    @Override
                    public void onNegativeClick() {}
                }
        );
    }

    private void showDeletePlaylistDialog() {
        DialogUtils.showConfirmDialog(
                getContext(),
                "Удалить плейлист",
                "Вы действительно хотите удалить плейлист «" + playlist.getName() + "»?",
                "Удалить",
                "Отмена",
                true,
                new DialogUtils.DialogListener() {
                    @Override
                    public void onPositiveClick() {
                        databaseManager.deletePlaylist(playlist.getId());
                        CustomToast.showSuccess(getContext(), "Плейлист удален");
                        if (actionListener != null) {
                            actionListener.onPlaylistDeleted(playlist);
                        }
                        dismiss();
                    }

                    @Override
                    public void onNegativeClick() {}
                }
        );
    }

    private void showRemoveVideoDialog(VideoItem video) {
        if (video == null) return;
        DialogUtils.showConfirmDialog(
                getContext(),
                "Удалить из плейлиста",
                "Удалить видео «" + video.getDisplayTitle() + "» из этого плейлиста?",
                "Удалить",
                "Отмена",
                true,
                new DialogUtils.DialogListener() {
                    @Override
                    public void onPositiveClick() {
                        databaseManager.removeVideoFromPlaylist(playlist.getId(), video.getPath());
                        CustomToast.showSuccess(getContext(), "Видео удалено из плейлиста");
                        loadVideos();
                        if (actionListener != null) {
                            actionListener.onPlaylistUpdated(playlist);
                        }
                    }

                    @Override
                    public void onNegativeClick() {}
                }
        );
    }
}
