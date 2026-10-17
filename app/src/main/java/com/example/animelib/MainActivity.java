package com.example.animelib;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.animelib.fragments.ExplorerFragment;
import com.example.animelib.fragments.PlaylistsFragment;
import com.example.animelib.fragments.SettingsFragment;
import com.example.animelib.util.ThemeUtils;

public class MainActivity extends AppCompatActivity {

    private View tabExplorer, tabPlaylists, tabSettings;
    private TextView tvTabExplorerText, tvTabPlaylistsText, tvTabSettingsText;
    private ImageView ivTabExplorerIcon, ivTabPlaylistsIcon, ivTabSettingsIcon;
    private View vTabIndicator;

    private ExplorerFragment explorerFragment;
    private PlaylistsFragment playlistsFragment;
    private SettingsFragment settingsFragment;
    private Fragment activeFragment;

    private int currentTabIndex = 0;
    private int cachedBottomInset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme preference
        ThemeUtils.initTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupWindowInsets();
        initViews();
        initFragments(savedInstanceState);
        setupTabListeners();
    }

    private void setupWindowInsets() {
        View fragmentContainer = findViewById(R.id.fragmentContainer);
        if (fragmentContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        View layoutBottomBar = findViewById(R.id.layoutBottomBar);
        if (layoutBottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutBottomBar, (v, insets) -> {
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                cachedBottomInset = navBarInsets.bottom;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int defaultBottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
                lp.bottomMargin = cachedBottomInset + defaultBottomMargin;
                v.setLayoutParams(lp);

                dispatchBottomInsetToFragments(cachedBottomInset);
                return insets;
            });
        }
    }

    public void dispatchBottomInsetToFragments(int bottomInset) {
        this.cachedBottomInset = bottomInset;
        if (explorerFragment != null && explorerFragment.isAdded()) {
            explorerFragment.updateBottomInsets(bottomInset);
        }
        if (playlistsFragment != null && playlistsFragment.isAdded()) {
            playlistsFragment.updateBottomInsets(bottomInset);
        }
        if (settingsFragment != null && settingsFragment.isAdded()) {
            settingsFragment.updateBottomInsets(bottomInset);
        }
    }

    private void initViews() {
        tabExplorer = findViewById(R.id.tabExplorer);
        tabPlaylists = findViewById(R.id.tabPlaylists);
        tabSettings = findViewById(R.id.tabSettings);

        tvTabExplorerText = findViewById(R.id.tvTabExplorerText);
        tvTabPlaylistsText = findViewById(R.id.tvTabPlaylistsText);
        tvTabSettingsText = findViewById(R.id.tvTabSettingsText);

        ivTabExplorerIcon = findViewById(R.id.ivTabExplorerIcon);
        ivTabPlaylistsIcon = findViewById(R.id.ivTabPlaylistsIcon);
        ivTabSettingsIcon = findViewById(R.id.ivTabSettingsIcon);

        vTabIndicator = findViewById(R.id.vTabIndicator);
    }

    private void initFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            explorerFragment = new ExplorerFragment();
            playlistsFragment = new PlaylistsFragment();
            settingsFragment = new SettingsFragment();

            fm.beginTransaction()
                    .add(R.id.fragmentContainer, settingsFragment, "SETTINGS").hide(settingsFragment)
                    .add(R.id.fragmentContainer, playlistsFragment, "PLAYLISTS").hide(playlistsFragment)
                    .add(R.id.fragmentContainer, explorerFragment, "EXPLORER")
                    .commit();

            activeFragment = explorerFragment;
        } else {
            explorerFragment = (ExplorerFragment) fm.findFragmentByTag("EXPLORER");
            playlistsFragment = (PlaylistsFragment) fm.findFragmentByTag("PLAYLISTS");
            settingsFragment = (SettingsFragment) fm.findFragmentByTag("SETTINGS");

            if (explorerFragment == null) explorerFragment = new ExplorerFragment();
            if (playlistsFragment == null) playlistsFragment = new PlaylistsFragment();
            if (settingsFragment == null) settingsFragment = new SettingsFragment();

            activeFragment = explorerFragment;
        }

        updateTabSelection(0);
    }

    private void setupTabListeners() {
        if (tabExplorer != null) {
            tabExplorer.setOnClickListener(v -> selectTab(0));
        }
        if (tabPlaylists != null) {
            tabPlaylists.setOnClickListener(v -> selectTab(1));
        }
        if (tabSettings != null) {
            tabSettings.setOnClickListener(v -> selectTab(2));
        }
    }

    public void selectTab(int index) {
        if (currentTabIndex == index && activeFragment != null) return;

        FragmentManager fm = getSupportFragmentManager();
        Fragment targetFragment = null;

        switch (index) {
            case 0:
                targetFragment = explorerFragment;
                break;
            case 1:
                targetFragment = playlistsFragment;
                break;
            case 2:
                targetFragment = settingsFragment;
                break;
        }

        if (targetFragment != null) {
            fm.beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
            currentTabIndex = index;
            updateTabSelection(index);
            dispatchBottomInsetToFragments(cachedBottomInset);
        }
    }

    private void updateTabSelection(int targetIndex) {
        View[] tabs = {tabExplorer, tabPlaylists, tabSettings};
        TextView[] texts = {tvTabExplorerText, tvTabPlaylistsText, tvTabSettingsText};
        ImageView[] icons = {ivTabExplorerIcon, ivTabPlaylistsIcon, ivTabSettingsIcon};

        for (int i = 0; i < 3; i++) {
            if (tabs[i] != null && tabs[i].getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabs[i].getLayoutParams();
                lp.weight = (i == targetIndex) ? 1.5f : 0.75f;
                tabs[i].setLayoutParams(lp);
            }
            if (i == targetIndex) {
                if (texts[i] != null) {
                    texts[i].setVisibility(View.VISIBLE);
                    texts[i].setTextColor(ContextCompat.getColor(this, R.color.white_color));
                }
                if (icons[i] != null) {
                    icons[i].setColorFilter(ContextCompat.getColor(this, R.color.white_color));
                }
            } else {
                if (texts[i] != null) {
                    texts[i].setVisibility(View.GONE);
                }
                if (icons[i] != null) {
                    icons[i].setColorFilter(ContextCompat.getColor(this, R.color.player_item_inactive_text));
                }
            }
        }

        if (vTabIndicator != null && tabs[targetIndex] != null) {
            View target = tabs[targetIndex];
            target.post(() -> {
                if (vTabIndicator != null && target != null) {
                    int targetWidth = target.getWidth();
                    int targetLeft = target.getLeft();

                    ViewGroup.LayoutParams lp = vTabIndicator.getLayoutParams();
                    if (lp.width != targetWidth) {
                        lp.width = targetWidth;
                        vTabIndicator.setLayoutParams(lp);
                    }

                    vTabIndicator.animate()
                            .translationX(targetLeft)
                            .setDuration(200)
                            .start();
                }
            });
        }
    }
}
