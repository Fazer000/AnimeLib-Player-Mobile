package com.example.animelib.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.animelib.R;
import com.example.animelib.util.CustomToast;
import com.google.android.material.button.MaterialButton;

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

    private ImageButton btnBack;
    private TextView tvVersionTitle;
    private TextView tvCurrentVsNew;
    private TextView tvReleaseDate;
    private TextView tvChangelog;
    private M3LinearProgressIndicator progressDownload;
    private TextView tvDownloadStatus;
    private MaterialButton btnDownload;
    private MaterialButton btnOpenGithub;

    private String directApkUrl = null;
    private String githubHtmlUrl = GITHUB_RELEASES_PAGE;
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
        tvVersionTitle = findViewById(R.id.tvVersionTitle);
        tvCurrentVsNew = findViewById(R.id.tvCurrentVsNew);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        tvChangelog = findViewById(R.id.tvChangelog);
        progressDownload = findViewById(R.id.progressDownload);
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);
        btnDownload = findViewById(R.id.btnDownload);
        btnOpenGithub = findViewById(R.id.btnOpenGithub);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnOpenGithub != null) {
            btnOpenGithub.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubHtmlUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    CustomToast.showError(this, "Не удалось открыть браузер");
                }
            });
        }
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                if (!isDownloading) {
                    startInAppDownload();
                }
            });
        }
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
        if (progressDownload != null) progressDownload.setIndeterminate(true);
        if (tvDownloadStatus != null) {
            tvDownloadStatus.setVisibility(View.VISIBLE);
            tvDownloadStatus.setText("Проверка обновлений с GitHub...");
        }

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
                    if (json.has("tag_name")) tag = json.getString("tag_name");
                    if (json.has("body")) releaseBody = json.getString("body");
                    if (json.has("published_at")) publishedAt = json.getString("published_at");
                    if (json.has("html_url")) targetHtmlUrl = json.getString("html_url");

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
                if (progressDownload != null) progressDownload.setIndeterminate(false);
                if (finalErrorMessage != null) {
                    if (tvDownloadStatus != null) tvDownloadStatus.setText(finalErrorMessage);
                } else if (finalHasUpdate) {
                    directApkUrl = finalTargetApkUrl;
                    githubHtmlUrl = finalTargetHtmlUrl;

                    if (tvVersionTitle != null) tvVersionTitle.setText("Доступна версия " + finalTag);
                    if (tvCurrentVsNew != null) tvCurrentVsNew.setText("У вас: v" + getInstalledVersion() + "  ➔  Новая: " + finalTag);
                    if (tvReleaseDate != null) {
                        if (finalPublishedAt != null && finalPublishedAt.length() >= 10) {
                            tvReleaseDate.setText("Опубликовано: " + finalPublishedAt.substring(0, 10));
                        } else {
                            tvReleaseDate.setText("Опубликовано: 17.10.2026");
                        }
                    }
                    if (tvChangelog != null) {
                        if (finalReleaseBody != null && !finalReleaseBody.trim().isEmpty()) {
                            tvChangelog.setText(finalReleaseBody.trim());
                        } else {
                            tvChangelog.setText("- Исправлены ошибки плеера\n- Добавлены новые настройки\n- Оптимизация загрузок");
                        }
                    }
                    if (tvDownloadStatus != null) tvDownloadStatus.setVisibility(View.GONE);
                } else {
                    if (tvDownloadStatus != null) {
                        tvDownloadStatus.setVisibility(View.VISIBLE);
                        tvDownloadStatus.setText("У вас установлена последняя версия (" + getInstalledVersion() + ")");
                    }
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
        if (btnDownload != null) {
            btnDownload.setEnabled(false);
            btnDownload.setText("СКАЧИВАНИЕ...");
        }
        if (progressDownload != null) {
            progressDownload.setVisibility(View.VISIBLE);
            progressDownload.setProgress(0);
        }
        if (tvDownloadStatus != null) {
            tvDownloadStatus.setVisibility(View.VISIBLE);
            tvDownloadStatus.setText("Подключение...");
        }

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
                                if (progressDownload != null) progressDownload.setProgress(progress);
                                String formattedProgress = String.format("Загрузка: %d%% (%.1f MB / %.1f MB)",
                                        progress,
                                        currentTotal / (1024.0 * 1024.0),
                                        totalLength / (1024.0 * 1024.0));
                                if (tvDownloadStatus != null) tvDownloadStatus.setText(formattedProgress);
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
                if (btnDownload != null) {
                    btnDownload.setEnabled(true);
                    btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                }
                if (progressDownload != null) progressDownload.setVisibility(View.GONE);

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