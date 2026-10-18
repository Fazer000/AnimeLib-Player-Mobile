package com.example.animelib.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.models.VideoItem;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;
import com.example.animelib.util.VideoThumbnailHelper;

import java.util.concurrent.Executors;

public class VideoOptionsBottomSheet extends FlexibleBottomSheetDialog {

    public interface VideoActionCallback {
        void onShare(VideoItem video);
        void onAddToPlaylist(VideoItem video);
        void onRename(VideoItem video);
        void onInfo(VideoItem video);
        void onDelete(VideoItem video);
    }

    private final VideoItem video;
    private final VideoActionCallback callback;

    public VideoOptionsBottomSheet(@NonNull Context context, @NonNull VideoItem video, VideoActionCallback callback) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.video = video;
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_video_options, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this, true, R.drawable.bs_background);

        ImageButton btnClose = view.findViewById(R.id.btnCloseVideoOptions);
        ImageView ivThumbnail = view.findViewById(R.id.ivBsVideoThumbnail);
        TextView tvTitle = view.findViewById(R.id.tvBsVideoTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvBsVideoSubtitle);

        View optShare = view.findViewById(R.id.optVideoShare);
        View optAddToPlaylist = view.findViewById(R.id.optVideoAddToPlaylist);
        View optRename = view.findViewById(R.id.optVideoRename);
        View optInfo = view.findViewById(R.id.optVideoInfo);
        View optDelete = view.findViewById(R.id.optVideoDelete);

        btnClose.setOnClickListener(v -> dismiss());

        tvTitle.setText(video.getDisplayTitle());

        StringBuilder sub = new StringBuilder();
        String ext = video.getFileExtension();
        if (!ext.isEmpty()) sub.append(ext);
        String size = video.getFormattedSize();
        if (!size.isEmpty()) {
            if (sub.length() > 0) sub.append(" • ");
            sub.append(size);
        }
        if (video.getFolderName() != null && !video.getFolderName().isEmpty()) {
            if (sub.length() > 0) sub.append(" • ");
            sub.append(video.getFolderName());
        }
        tvSubtitle.setText(sub.toString());

        // Load thumbnail
        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap thumb = VideoThumbnailHelper.createFrameThumbnail(getContext(), video.getPath(), video.getUri(), video.getDurationMs());
            if (thumb != null && ivThumbnail != null) {
                ivThumbnail.post(() -> ivThumbnail.setImageBitmap(thumb));
            }
        });

        optShare.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onShare(video);
        });

        optAddToPlaylist.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onAddToPlaylist(video);
        });

        optRename.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onRename(video);
        });

        optInfo.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onInfo(video);
        });

        optDelete.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onDelete(video);
        });
    }
}
