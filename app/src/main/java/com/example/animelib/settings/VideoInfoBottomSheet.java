package com.example.animelib.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.models.VideoItem;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class VideoInfoBottomSheet extends FlexibleBottomSheetDialog {

    private final VideoItem video;

    public VideoInfoBottomSheet(@NonNull Context context, @NonNull VideoItem video) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
        this.video = video;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_video_info, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this, true, R.drawable.bs_background);

        ImageButton btnClose = view.findViewById(R.id.btnCloseVideoInfo);
        TextView tvFileName = view.findViewById(R.id.tvInfoFileName);
        TextView tvResolution = view.findViewById(R.id.tvInfoResolution);
        TextView tvDuration = view.findViewById(R.id.tvInfoDuration);
        TextView tvSize = view.findViewById(R.id.tvInfoSize);
        TextView tvFormat = view.findViewById(R.id.tvInfoFormat);
        TextView tvDate = view.findViewById(R.id.tvInfoDate);
        TextView tvPath = view.findViewById(R.id.tvInfoPath);
        View layoutCopyPath = view.findViewById(R.id.layoutCopyPath);

        int accentColor = com.example.animelib.util.ThemeUtils.getAccentColorInt(getContext());
        if (tvFormat != null) {
            tvFormat.setTextColor(accentColor);
        }
        android.widget.ImageView ivCopy = view.findViewById(R.id.ivInfoCopyPath);
        if (ivCopy != null) {
            ivCopy.setImageTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }

        btnClose.setOnClickListener(v -> dismiss());

        String rawTitle = video.getTitle();
        if (rawTitle == null || rawTitle.isEmpty()) {
            if (video.getPath() != null) {
                rawTitle = new File(video.getPath()).getName();
            } else {
                rawTitle = "Видео";
            }
        }
        tvFileName.setText(rawTitle);

        tvDuration.setText(video.getFormattedDuration());
        tvSize.setText(video.getFormattedSize());

        String ext = video.getFileExtension();
        tvFormat.setText(!ext.isEmpty() ? ext : "VIDEO");

        String displayPath = video.getPath() != null && !video.getPath().isEmpty()
                ? video.getPath() : (video.getUriString() != null ? video.getUriString() : "—");
        tvPath.setText(displayPath);

        if (video.getDateModified() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(video.getDateModified() * 1000L)));
            } catch (Exception e) {
                tvDate.setText(video.getFormattedDate());
            }
        } else {
            tvDate.setText("—");
        }

        // Copy path listener
        layoutCopyPath.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Путь к видео", displayPath);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    CustomToast.show(getContext(), "Путь скопирован в буфер", CustomToast.TYPE_SUCCESS);
                }
            } catch (Exception ignored) {}
        });

        // Retrieve resolution in background
        Executors.newSingleThreadExecutor().execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                if (video.getPath() != null && new File(video.getPath()).exists()) {
                    retriever.setDataSource(video.getPath());
                } else if (video.getUri() != null) {
                    retriever.setDataSource(getContext(), video.getUri());
                }
                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (width != null && height != null) {
                    String resStr = width + " × " + height;
                    tvResolution.post(() -> tvResolution.setText(resStr));
                } else {
                    tvResolution.post(() -> tvResolution.setText("Стандартное"));
                }
            } catch (Exception e) {
                tvResolution.post(() -> tvResolution.setText("—"));
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        });
    }
}
