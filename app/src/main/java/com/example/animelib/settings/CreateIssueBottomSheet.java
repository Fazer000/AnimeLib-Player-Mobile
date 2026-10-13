package com.example.animelib.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.net.URLEncoder;

public class CreateIssueBottomSheet extends FlexibleBottomSheetDialog {

    private Runnable onBackPressedListener;

    public CreateIssueBottomSheet(@NonNull Context context) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
    }

    public void setOnBackPressedListener(Runnable listener) {
        this.onBackPressedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_create_issue, null);
        setContentView(view);

        ImageButton closeBtn = view.findViewById(R.id.bs_close);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dismiss());

        ImageButton backBtn = view.findViewById(R.id.bs_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                dismiss();
                if (onBackPressedListener != null) {
                    onBackPressedListener.run();
                }
            });
        }

        EditText etTitle = view.findViewById(R.id.etIssueTitle);
        EditText etBody = view.findViewById(R.id.etIssueBody);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitIssue);

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                String body = etBody != null ? etBody.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    CustomToast.showWarning(getContext(), "Введите заголовок проблемы");
                    return;
                }

                try {
                    String url = "https://github.com/Fazer000/AnimeLib-Player-Mobile/issues/new?title="
                            + URLEncoder.encode(title, "UTF-8")
                            + "&body=" + URLEncoder.encode(body, "UTF-8");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    getContext().startActivity(intent);
                    dismiss();
                } catch (Exception e) {
                    CustomToast.showWarning(getContext(), "Не удалось открыть браузер");
                }
            });
        }

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }
}
