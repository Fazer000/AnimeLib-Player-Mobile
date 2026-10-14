package com.example.animelib.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.animelib.R;
import com.example.animelib.util.CustomToast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateActivity extends AppCompatActivity {

    private static final String GITHUB_REPO = "Fazer000/AnimeLib-Player-Mobile";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String GITHUB_RELEASES_PAGE = "https://github.com/" + GITHUB_REPO + "/releases";

    private ImageView btnBack;
    private LinearLayout layoutLoading;
    private LinearLayout layoutUpToDate;
    private TextView tvUpToDateSub;
    private LinearLayout layoutUpdateAvailable;
    private TextView tvNewVersionTag;
    private TextView tvVersionComparison;
    private TextView tvReleaseDate;
    private TextView tvReleaseNotes;

    private LinearLayout layoutDownloadProgress;
    private TextView tvDownloadStatus;
    private TextView tvDownloadPercent;
    private LinearProgressIndicator downloadProgressBar;

    private MaterialButton btnDownloadUpdate;
    private MaterialButton btnOpenGithub;

    private LinearLayout layoutError;
    private TextView tvErrorText;
    private MaterialButton btnCheckAgain;

    private String directApkUrl = null;
    private String githubHtmlUrl = GITHUB_RELEASES_PAGE;
    private String latestTag = "";

    private boolean isDownloading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        initViews();
        setupListeners();

        checkForUpdates();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutUpToDate = findViewById(R.id.layoutUpToDate);
        tvUpToDateSub = findViewById(R.id.tvUpToDateSub);
        layoutUpdateAvailable = findViewById(R.id.layoutUpdateAvailable);
        tvNewVersionTag = findViewById(R.id.tvNewVersionTag);
        tvVersionComparison = findViewById(R.id.tvVersionComparison);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        tvReleaseNotes = findViewById(R.id.tvReleaseNotes);

        layoutDownloadProgress = findViewById(R.id.layoutDownloadProgress);
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);
        tvDownloadPercent = findViewById(R.id.tvDownloadPercent);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);

        btnDownloadUpdate = findViewById(R.id.btnDownloadUpdate);
        btnOpenGithub = findViewById(R.id.btnOpenGithub);

        layoutError = findViewById(R.id.layoutError);
        tvErrorText = findViewById(R.id.tvErrorText);
        btnCheckAgain = findViewById(R.id.btnCheckAgain);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCheckAgain.setOnClickListener(v -> checkForUpdates());

        btnOpenGithub.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubHtmlUrl));
                startActivity(intent);
            } catch (Exception e) {
                CustomToast.showError(this, "Не удалось открыть браузер");
            }
        });

        btnDownloadUpdate.setOnClickListener(v -> {
            if (!isDownloading) {
                startInAppDownload();
            }
        });
    }

    private String getInstalledVersion() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (versionName != null && !versionName.isEmpty()) {
                return versionName;
            }
        } catch (Exception ignored) {}
        return "1.8.8";
    }

    private void checkForUpdates() {
        showStateLoading();

        new Thread(() -> {
            boolean hasUpdate = false;
            String tag = "";
            String releaseBody = "";
            String publishedAt = "";
            String targetApkUrl = null;
            String targetHtmlUrl = GITHUB_RELEASES_PAGE;
            String errorMessage = null;

            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AnimeLib-Player-Android");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("tag_name")) {
                        tag = json.getString("tag_name");
                    }
                    if (json.has("body")) {
                        releaseBody = json.getString("body");
                    }
                    if (json.has("published_at")) {
                        publishedAt = json.getString("published_at");
                    }
                    if (json.has("html_url")) {
                        targetHtmlUrl = json.getString("html_url");
                    }

                    if (json.has("assets")) {
                        JSONArray assets = json.getJSONArray("assets");
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            if (asset.has("name") && asset.getString("name").toLowerCase().endsWith(".apk")) {
                                if (asset.has("browser_download_url")) {
                                    targetApkUrl = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                        }
                    }

                    if (targetApkUrl == null && tag != null && !tag.isEmpty()) {
                        targetApkUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + tag + "/app-release.apk";
                    }

                    String installedVer = getInstalledVersion();
                    hasUpdate = isVersionNewer(tag, installedVer);

                } else {
                    errorMessage = "Ошибка сервера GitHub (код " + responseCode + ")";
                }
            } catch (Exception e) {
                errorMessage = "Ошибка сети: " + e.getLocalizedMessage();
            }

            final boolean finalHasUpdate = hasUpdate;
            final String finalTag = tag;
            final String finalReleaseBody = releaseBody;
            final String finalPublishedAt = publishedAt;
            final String finalTargetApkUrl = targetApkUrl;
            final String finalTargetHtmlUrl = targetHtmlUrl;
            final String finalErrorMessage = errorMessage;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalErrorMessage != null) {
                    showStateError(finalErrorMessage);
                } else if (finalHasUpdate) {
                    latestTag = finalTag;
                    directApkUrl = finalTargetApkUrl;
                    githubHtmlUrl = finalTargetHtmlUrl;

                    showStateUpdateAvailable(finalTag, finalReleaseBody, finalPublishedAt);
                } else {
                    showStateUpToDate();
                }
            });
        }).start();
    }

    private void startInAppDownload() {
        if (directApkUrl == null || directApkUrl.isEmpty()) {
            CustomToast.showError(this, "Ссылка на APK файл не доступна");
            return;
        }

        isDownloading = true;
        btnDownloadUpdate.setEnabled(false);
        btnDownloadUpdate.setText("СКАЧИВАНИЕ...");
        layoutDownloadProgress.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(0);
        tvDownloadPercent.setText("0%");
        tvDownloadStatus.setText("Подключение...");

        // Download to internal cache directory (not permanent storage; if installation cancelled, user must download again)
        File targetFile = new File(getCacheDir(), "animelib_update_" + System.currentTimeMillis() + ".apk");

        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;

            try {
                URL url = new URL(directApkUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AnimeLib-Player-Android");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String redirectUrl = conn.getHeaderField("Location");
                    conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "AnimeLib-Player-Android");
                    responseCode = conn.getResponseCode();
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    int fileLength = conn.getContentLength();
                    InputStream input = conn.getInputStream();
                    FileOutputStream output = new FileOutputStream(targetFile);

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            final int progress = (int) (total * 100 / fileLength);
                            final long currentTotal = total;
                            final int totalLength = fileLength;

                            new Handler(Looper.getMainLooper()).post(() -> {
                                downloadProgressBar.setProgress(progress);
                                tvDownloadPercent.setText(progress + "%");
                                String formattedProgress = String.format("%.1f MB / %.1f MB",
                                        currentTotal / (1024.0 * 1024.0),
                                        totalLength / (1024.0 * 1024.0));
                                tvDownloadStatus.setText("Загрузка: " + formattedProgress);
                            });
                        }
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                    success = true;
                } else {
                    errorMsg = "Ошибка скачивания (код " + responseCode + ")";
                }
            } catch (Exception e) {
                errorMsg = "Ошибка скачивания: " + e.getLocalizedMessage();
            }

            final boolean finalSuccess = success;
            final String finalErrorMsg = errorMsg;

            new Handler(Looper.getMainLooper()).post(() -> {
                isDownloading = false;
                btnDownloadUpdate.setEnabled(true);
                btnDownloadUpdate.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                layoutDownloadProgress.setVisibility(View.GONE);

                if (finalSuccess && targetFile.exists()) {
                    installApk(targetFile);
                } else {
                    CustomToast.showError(UpdateActivity.this, finalErrorMsg != null ? finalErrorMsg : "Не удалось скачать обновление");
                }
            });
        }).start();
    }

    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            CustomToast.showError(this, "Файл обновления не найден");
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch (Exception e) {
            CustomToast.showError(this, "Ошибка установки: " + e.getLocalizedMessage());
        }
    }

    private void showStateLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutUpToDate.setVisibility(View.GONE);
        layoutUpdateAvailable.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        btnDownloadUpdate.setVisibility(View.GONE);
    }

    private void showStateUpToDate() {
        layoutLoading.setVisibility(View.GONE);
        layoutUpToDate.setVisibility(View.VISIBLE);
        layoutUpdateAvailable.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        btnDownloadUpdate.setVisibility(View.GONE);

        tvUpToDateSub.setText("Версия " + getInstalledVersion() + " является актуальной.");
    }

    private void showStateUpdateAvailable(String tag, String body, String publishedAt) {
        layoutLoading.setVisibility(View.GONE);
        layoutUpToDate.setVisibility(View.GONE);
        layoutUpdateAvailable.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        btnDownloadUpdate.setVisibility(View.VISIBLE);

        tvNewVersionTag.setText("Доступна версия " + tag);
        String currentVer = getInstalledVersion();
        tvVersionComparison.setText("У вас: v" + currentVer + " → Новая: " + tag);

        if (publishedAt != null && publishedAt.length() >= 10) {
            tvReleaseDate.setText("Дата публикации: " + publishedAt.substring(0, 10));
        } else {
            tvReleaseDate.setText("Дата публикации: 13.10.2026");
        }

        if (body != null && !body.trim().isEmpty()) {
            tvReleaseNotes.setText(body.trim());
        } else {
            tvReleaseNotes.setText("- Добавлена функция блокировки плеера\n- Добавлены настройки комментариев\n- Некоторые улучшения комментариев");
        }
    }

    private void showStateError(String errorMsg) {
        layoutLoading.setVisibility(View.GONE);
        layoutUpToDate.setVisibility(View.GONE);
        layoutUpdateAvailable.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        btnDownloadUpdate.setVisibility(View.GONE);

        tvErrorText.setText(errorMsg);
    }

    private boolean isVersionNewer(String latestTag, String currentVersion) {
        if (latestTag == null || latestTag.isEmpty()) return false;
        if (currentVersion == null || currentVersion.isEmpty()) return true;

        String cleanLatest = latestTag.replaceAll("(?i)^v", "").split("-")[0].trim();
        String cleanCurrent = currentVersion.replaceAll("(?i)^v", "").split("-")[0].trim();

        String[] latestParts = cleanLatest.split("\\.");
        String[] currentParts = cleanCurrent.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestNum = 0;
            int currentNum = 0;

            if (i < latestParts.length) {
                try {
                    latestNum = Integer.parseInt(latestParts[i].replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }

            if (i < currentParts.length) {
                try {
                    currentNum = Integer.parseInt(currentParts[i].replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }

            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }

        return false;
    }
}
