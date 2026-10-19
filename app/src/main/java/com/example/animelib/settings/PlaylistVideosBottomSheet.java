package com.example.animelib.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
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
import com.example.animelib.util.DialogUtils;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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
        ImageButton btnPlay = view.findViewById(R.id.btnBsPlaylistPlay);
        ImageButton btnMenu = view.findViewById(R.id.btnBsPlaylistMenu);
        tvTitle = view.findViewById(R.id.tvBsPlaylistTitle);
        tvCount = view.findViewById(R.id.tvBsPlaylistCount);
        layoutEmpty = view.findViewById(R.id.layoutBsPlaylistEmpty);
        rvEpisodes = view.findViewById(R.id.rvBsPlaylistEpisodes);

        btnClose.setOnClickListener(v -> dismiss());

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> playAllVideos());
        }

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
                ArrayList<VideoItem> playlistVideos = buildVideoItemList();
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
                openVideoOptionsBottomSheet(video);
            }
        });
        rvEpisodes.setAdapter(adapter);
    }

    private ArrayList<VideoItem> buildVideoItemList() {
        ArrayList<VideoItem> playlistVideos = new ArrayList<>();
        if (currentVideos == null) return playlistVideos;
        long tempId = 1;
        for (PlaylistVideoEntity entity : currentVideos) {
            String path = entity.getVideoPath();
            File file = new File(path);
            String title = entity.getVideoTitle() != null ? entity.getVideoTitle() : file.getName();
            playlistVideos.add(new VideoItem(tempId++, title, path, null, 0, file.length(), file.lastModified(), playlist.getName()));
        }
        return playlistVideos;
    }

    /**
     * Показывает выпадающий список действий с плейлистом (PopupWindow),
     * оформленный в стиле AnimeLib-Mobile (bg_sort_popup).
     */
    private void showPopupMenu(View anchorView) {
        if (anchorView == null || getContext() == null) return;
        try {
            View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_playlist_options, null);
            float density = getContext().getResources().getDisplayMetrics().density;
            int width = (int) (210 * density);
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setElevation(14 * density);
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            View itemPlayAll = popupView.findViewById(R.id.popup_playlist_play_all);
            View itemRename = popupView.findViewById(R.id.popup_playlist_rename);
            View itemClear = popupView.findViewById(R.id.popup_playlist_clear);
            View itemDelete = popupView.findViewById(R.id.popup_playlist_delete);

            if (itemPlayAll != null) {
                itemPlayAll.setOnClickListener(v -> {
                    popupWindow.dismiss();
                    playAllVideos();
                });
            }

            if (itemRename != null) {
                itemRename.setOnClickListener(v -> {
                    popupWindow.dismiss();
                    showRenamePlaylistDialog();
                });
            }

            if (itemClear != null) {
                itemClear.setOnClickListener(v -> {
                    popupWindow.dismiss();
                    showClearPlaylistDialog();
                });
            }

            if (itemDelete != null) {
                itemDelete.setOnClickListener(v -> {
                    popupWindow.dismiss();
                    showDeletePlaylistDialog();
                });
            }

            int xOffset = (int) (-175 * density);
            int yOffset = (int) (6 * density);
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playAllVideos() {
        if (currentVideos == null || currentVideos.isEmpty()) {
            CustomToast.showWarning(getContext(), "Плейлист пуст");
            return;
        }
        ArrayList<VideoItem> playlistVideos = buildVideoItemList();
        if (playlistVideos.isEmpty()) {
            CustomToast.showWarning(getContext(), "Видео не найдены");
            return;
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

    /**
     * Открывает стандартный VideoOptionsBottomSheet для видео из плейлиста,
     * с активной опцией «Удалить из плейлиста».
     */
    private void openVideoOptionsBottomSheet(VideoItem video) {
        if (video == null || getContext() == null) return;
        VideoOptionsBottomSheet bs = new VideoOptionsBottomSheet(getContext(), video, new VideoOptionsBottomSheet.VideoActionCallback() {
            @Override
            public void onShare(VideoItem v) {
                shareVideo(v);
            }

            @Override
            public void onAddToPlaylist(VideoItem v) {
                showAddToOtherPlaylistDialog(v);
            }

            @Override
            public void onRename(VideoItem v) {
                showRenameVideoDialog(v);
            }

            @Override
            public void onInfo(VideoItem v) {
                VideoInfoBottomSheet infoBs = new VideoInfoBottomSheet(getContext(), v);
                infoBs.show();
            }

            @Override
            public void onDelete(VideoItem v) {
                confirmDeleteVideo(v);
            }

            @Override
            public void onRemoveFromPlaylist(VideoItem v) {
                showRemoveVideoDialog(v);
            }
        });
        bs.setPlaylistContext(true);
        bs.show();
    }

    private void showRemoveVideoDialog(VideoItem video) {
        if (video == null || getContext() == null) return;
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

    private void shareVideo(VideoItem video) {
        if (video == null || getContext() == null) return;
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/*");
            Uri uri = video.getUri();
            if (uri == null && video.getPath() != null) {
                File file = new File(video.getPath());
                uri = androidx.core.content.FileProvider.getUriForFile(
                        getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        file
                );
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(Intent.createChooser(shareIntent, "Поделиться видео"));
        } catch (Exception e) {
            CustomToast.showWarning(getContext(), "Не удалось поделиться видео");
        }
    }

    private void showAddToOtherPlaylistDialog(VideoItem video) {
        if (video == null || getContext() == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PlaylistEntity> all = databaseManager.getAllPlaylists();
            if (all == null) all = new ArrayList<>();
            List<PlaylistEntity> filtered = new ArrayList<>();
            for (PlaylistEntity p : all) {
                if (p.getId() != playlist.getId()) {
                    filtered.add(p);
                }
            }
            ArrayList<String> options = new ArrayList<>();
            options.add("＋ Новый плейлист");
            for (PlaylistEntity p : filtered) {
                options.add(p.getName());
            }

            final List<PlaylistEntity> otherPlaylists = filtered;
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                DialogUtils.showListDialog(getContext(), "Добавить в плейлист", options.toArray(new String[0]), index -> {
                    if (index == 0) {
                        DialogUtils.showInputDialog(getContext(), "Новый плейлист", "Введите название плейлиста:", "", "Название", "Создать", "Отмена", new DialogUtils.InputDialogListener() {
                            @Override
                            public void onInputConfirmed(String input) {
                                if (input == null || input.trim().isEmpty()) return;
                                ArrayList<VideoItem> single = new ArrayList<>();
                                single.add(video);
                                databaseManager.createPlaylist(input.trim(), single);
                                CustomToast.showSuccess(getContext(), "Плейлист создан и видео добавлено");
                                if (actionListener != null) {
                                    actionListener.onPlaylistUpdated(playlist);
                                }
                            }

                            @Override
                            public void onCancelled() {}
                        });
                    } else {
                        PlaylistEntity targetPlaylist = otherPlaylists.get(index - 1);
                        databaseManager.addVideoToPlaylist(targetPlaylist.getId(), video.getPath(), video.getTitle());
                        CustomToast.showSuccess(getContext(), "Видео добавлено в «" + targetPlaylist.getName() + "»");
                    }
                });
            });
        });
    }

    private void showRenameVideoDialog(VideoItem video) {
        if (video == null || getContext() == null) return;
        DialogUtils.showInputDialog(
                getContext(),
                "Переименовать видео",
                "Введите новое название видео:",
                video.getDisplayTitle(),
                "Название видео",
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
                        video.setTitle(newName);
                        loadVideos();
                        CustomToast.showSuccess(getContext(), "Название обновлено");
                    }

                    @Override
                    public void onCancelled() {}
                }
        );
    }

    private void confirmDeleteVideo(VideoItem video) {
        if (video == null || getContext() == null) return;
        DialogUtils.showConfirmDialog(
                getContext(),
                "Удалить файл",
                "Вы уверены, что хотите удалить видео «" + video.getDisplayTitle() + "»? Файл будет удален с устройства.",
                "Удалить",
                "Отмена",
                true,
                new DialogUtils.DialogListener() {
                    @Override
                    public void onPositiveClick() {
                        try {
                            if (video.getPath() != null) {
                                File file = new File(video.getPath());
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                            databaseManager.deleteVideoEverywhere(video.getPath());
                            CustomToast.showSuccess(getContext(), "Видео удалено");
                            loadVideos();
                            if (actionListener != null) {
                                actionListener.onPlaylistUpdated(playlist);
                            }
                        } catch (Exception e) {
                            CustomToast.showWarning(getContext(), "Не удалось удалить файл");
                        }
                    }

                    @Override
                    public void onNegativeClick() {}
                }
        );
    }
}
