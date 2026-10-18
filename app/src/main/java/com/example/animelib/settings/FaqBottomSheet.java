package com.example.animelib.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FlexibleBottomSheetUtils;
import com.google.android.material.tabs.TabLayout;

public class FaqBottomSheet extends FlexibleBottomSheetDialog {

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    private OnBackPressedListener onBackPressedListener;

    public FaqBottomSheet(@NonNull Context context) {
        super(context, R.style.ThemeOverlay_AnimeLIB_BottomSheetDialog);
    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_faq, null);
        setContentView(view);

        FlexibleBottomSheetUtils.setupFlexibleStyle(this);

        ImageButton backButton = view.findViewById(R.id.bs_faq_back);
        TabLayout tabLayout = view.findViewById(R.id.faqTabLayout);
        LinearLayout layoutGestures = view.findViewById(R.id.layoutGestures);
        LinearLayout layoutCopyright = view.findViewById(R.id.layoutCopyright);

        // Add tabs
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("Жесты и управление"));
            tabLayout.addTab(tabLayout.newTab().setText("Правообладателям"));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        if (layoutGestures != null) layoutGestures.setVisibility(View.VISIBLE);
                        if (layoutCopyright != null) layoutCopyright.setVisibility(View.GONE);
                    } else {
                        if (layoutGestures != null) layoutGestures.setVisibility(View.GONE);
                        if (layoutCopyright != null) layoutCopyright.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        int accentColor = com.example.animelib.util.ThemeUtils.getAccentColorInt(getContext());
        if (tabLayout != null) {
            tabLayout.setSelectedTabIndicatorColor(accentColor);
            tabLayout.setTabTextColors(
                androidx.core.content.ContextCompat.getColor(getContext(), R.color.player_item_inactive_text),
                accentColor
            );
        }
        applyAccentToIcons(view, accentColor);

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                dismiss();
                if (onBackPressedListener != null) {
                    onBackPressedListener.onBackPressed();
                }
            });
        }

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private void applyAccentToIcons(View root, int accentColor) {
        if (root == null) return;
        if (root instanceof android.widget.ImageView && root.getId() != R.id.bs_faq_back) {
            ((android.widget.ImageView) root).setImageTintList(android.content.res.ColorStateList.valueOf(accentColor));
        } else if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyAccentToIcons(vg.getChildAt(i), accentColor);
            }
        }
    }
}
