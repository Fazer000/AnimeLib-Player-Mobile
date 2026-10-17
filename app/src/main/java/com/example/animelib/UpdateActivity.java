package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.animelib.models.UpdateInfo;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.util.UpdateManager;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateActivity extends AppCompatActivity {
    private static final String TAG = "UpdateActivity";
    public static final String EXTRA_UPDATE_INFO = "extra_update_info";

    private UpdateInfo updateInfo;
    private com.example.animelib.ui.M3LinearProgressIndicator progressDownload;
    private TextView tvDownloadStatus;
    private TextView tvVersionTitle;
    private TextView tvCurrentVsNew;
    private TextView tvReleaseDate;
    private TextView tvChangelog;
    private MaterialButton btnDownload;
    private MaterialButton btnOpenGithub;
    private boolean isDownloading = false;

    public static void start(Context context, UpdateInfo updateInfo) {
        Intent intent = new Intent(context, UpdateActivity.class);
        intent.putExtra(EXTRA_UPDATE_INFO, updateInfo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applyTheme(ThemeUtils.getSavedThemePreference(this));
        setContentView(R.layout.activity_update);

        if (getWindow() != null) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.toolbar_header_bg);
            getWindow().setStatusBarColor(headerColor);
            boolean isDark = ThemeUtils.getSavedThemePreference(this) == ThemeUtils.THEME_DARK ||
                    (ThemeUtils.getSavedThemePreference(this) == ThemeUtils.THEME_SYSTEM &&
                            (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES);
            androidx.core.view.WindowInsetsControllerCompat insetsController =
                    androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (insetsController != null) {
                insetsController.setAppearanceLightStatusBars(!isDark);
            }
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        android.widget.ImageView ivUpdateLogo = findViewById(R.id.ivUpdateLogo);
        if (ivUpdateLogo != null) {
            boolean isDark = ThemeUtils.isDarkTheme(this);
            ivUpdateLogo.setImageResource(isDark ? R.drawable.ic_logo_non_back_night : R.drawable.ic_logo_non_back_light);
        }

        tvVersionTitle = findViewById(R.id.tvVersionTitle);
        tvCurrentVsNew = findViewById(R.id.tvCurrentVsNew);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        tvChangelog = findViewById(R.id.tvChangelog);

        progressDownload = findViewById(R.id.progressDownload);
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);
        btnDownload = findViewById(R.id.btnDownload);
        btnOpenGithub = findViewById(R.id.btnOpenGithub);

        btnBack.setOnClickListener(v -> finish());

        btnOpenGithub.setOnClickListener(v -> {
            try {
                String url = (updateInfo != null && updateInfo.getHtmlUrl() != null && !updateInfo.getHtmlUrl().isEmpty())
                        ? updateInfo.getHtmlUrl()
                        : UpdateManager.GITHUB_REPO_URL;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                CustomToast.showWarning(this, "Не удалось открыть браузер");
            }
        });

        btnDownload.setOnClickListener(v -> {
            if (isDownloading) return;
            if (updateInfo == null) {
                fetchUpdateInfo();
            } else {
                checkInstallPermissionAndDownload();
            }
        });

        if (getIntent() != null && getIntent().hasExtra(EXTRA_UPDATE_INFO)) {
            updateInfo = (UpdateInfo) getIntent().getSerializableExtra(EXTRA_UPDATE_INFO);
        }

        if (updateInfo != null) {
            bindUpdateInfo(updateInfo);
        } else {
            fetchUpdateInfo();
        }
    }

    private void bindUpdateInfo(UpdateInfo info) {
        if (info == null) return;
        this.updateInfo = info;

        String currentVer = UpdateManager.getCurrentVersion(this);
        String latestTag = info.getTagName() != null ? info.getTagName() : "v0.0.0";
        boolean hasUpdate = UpdateManager.isUpdateAvailable(currentVer, latestTag);

        if (hasUpdate) {
            tvVersionTitle.setText("Доступна версия " + latestTag);
            tvCurrentVsNew.setText("У вас: v" + currentVer + "  ➔  Новая: " + latestTag);
            btnDownload.setEnabled(true);
            btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
        } else {
            tvVersionTitle.setText("У вас актуальная версия (" + latestTag + ")");
            tvCurrentVsNew.setText("Текущая версия v" + currentVer + " — обновлений не требуется");
            btnDownload.setEnabled(false);
            btnDownload.setText("ОБНОВЛЕНИЕ НЕ ТРЕБУЕТСЯ");
        }

        String pubDate = formatDate(info.getPublishedAt());
        if (!pubDate.isEmpty()) {
            tvReleaseDate.setText("Дата публикации: " + pubDate);
            tvReleaseDate.setVisibility(View.VISIBLE);
        } else {
            tvReleaseDate.setVisibility(View.GONE);
        }

        String changelogText = info.getChangelog();
        if (changelogText != null && !changelogText.trim().isEmpty()) {
            changelogText = changelogText.replaceAll("(?m)^#+\\s*", "• ");
            tvChangelog.setText(changelogText.trim());
        } else {
            tvChangelog.setText("Описание изменений отсутствует.");
        }
    }

    private void fetchUpdateInfo() {
        tvVersionTitle.setText("Проверка обновлений...");
        tvCurrentVsNew.setText("Подключение к GitHub...");
        progressDownload.setVisibility(View.VISIBLE);
        progressDownload.setIndeterminate(true);
        btnDownload.setEnabled(false);
        btnDownload.setText("ПРОВЕРКА...");

        UpdateManager.checkForUpdates(this, null, new UpdateManager.CheckUpdateCallback() {
            @Override
            public void onUpdateCheckResult(boolean hasUpdate, UpdateInfo info, String currentVersion) {
                runOnUiThread(() -> {
                    progressDownload.setVisibility(View.GONE);
                    if (info != null) {
                        bindUpdateInfo(info);
                        if (hasUpdate) {
                            CustomToast.showSuccess(UpdateActivity.this, "Найдено обновление " + info.getTagName());
                        } else {
                            CustomToast.showInfo(UpdateActivity.this, "У вас установлена последняя версия");
                        }
                    } else {
                        tvVersionTitle.setText("Информация об обновлении не найдена");
                        tvCurrentVsNew.setText("Текущая версия v" + currentVersion);
                        btnDownload.setEnabled(true);
                        btnDownload.setText("ПРОВЕРИТЬ СНОВА");
                        CustomToast.showWarning(UpdateActivity.this, "Релизы не найдены на GitHub");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressDownload.setVisibility(View.GONE);
                    tvVersionTitle.setText("Ошибка проверки обновлений");
                    tvCurrentVsNew.setText(errorMessage);
                    btnDownload.setEnabled(true);
                    btnDownload.setText("ПОВТОРИТЬ ПРОВЕРКУ");
                    CustomToast.showError(UpdateActivity.this, errorMessage);
                });
            }
        });
    }

    private void checkInstallPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                CustomToast.showInfo(this, "Разрешите установку из неизвестных источников");
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening install permission settings", e);
                }
                return;
            }
        }
        startApkDownload();
    }

    @SuppressLint("SetTextI18n")
    private void startApkDownload() {
        if (updateInfo == null) return;
        String apkUrl = updateInfo.getApkUrl();
        if (apkUrl == null || apkUrl.isEmpty() || apkUrl.equalsIgnoreCase(updateInfo.getHtmlUrl())) {
            CustomToast.showInfo(this, "Открываем страницу релиза на GitHub...");
            btnOpenGithub.performClick();
            return;
        }

        isDownloading = true;
        btnDownload.setEnabled(false);
        btnDownload.setText("ЗАГРУЗКА...");
        progressDownload.setVisibility(View.VISIBLE);
        progressDownload.setIndeterminate(true);
        tvDownloadStatus.setVisibility(View.VISIBLE);
        tvDownloadStatus.setText("Подключение к GitHub...");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "AnimeLib-App")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                runOnUiThread(() -> {
                    isDownloading = false;
                    btnDownload.setEnabled(true);
                    btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                    progressDownload.setVisibility(View.GONE);
                    tvDownloadStatus.setText("Ошибка скачивания");
                    CustomToast.showWarning(UpdateActivity.this, "Ошибка скачивания обновления");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isDownloading = false;
                        btnDownload.setEnabled(true);
                        btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                        progressDownload.setVisibility(View.GONE);
                        tvDownloadStatus.setText("Ошибка сервера HTTP " + response.code());
                        CustomToast.showWarning(UpdateActivity.this, "Ошибка загрузки обновления");
                    });
                    return;
                }

                try {
                    File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (downloadsDir != null && !downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File apkFile = new File(downloadsDir, "app-update.apk");
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }

                    long contentLength = response.body() != null ? response.body().contentLength() : 0;
                    InputStream inputStream = response.body() != null ? response.body().byteStream() : null;

                    if (inputStream == null) {
                        runOnUiThread(() -> {
                            isDownloading = false;
                            btnDownload.setEnabled(true);
                            btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                            progressDownload.setVisibility(View.GONE);
                            CustomToast.showWarning(UpdateActivity.this, "Не удалось получить файл");
                        });
                        return;
                    }

                    OutputStream outputStream = new FileOutputStream(apkFile);
                    byte[] buffer = new byte[8192];
                    long bytesReadTotal = 0;
                    int read;

                    runOnUiThread(() -> {
                        progressDownload.setIndeterminate(false);
                        progressDownload.setMax(100);
                    });

                    long lastProgressUpdate = 0;

                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                        bytesReadTotal += read;

                        long now = System.currentTimeMillis();
                        if (contentLength > 0 && (now - lastProgressUpdate > 200)) {
                            lastProgressUpdate = now;
                            final int progress = (int) ((bytesReadTotal * 100) / contentLength);
                            final long currentMB = bytesReadTotal / (1024 * 1024);
                            final long totalMB = contentLength / (1024 * 1024);

                            runOnUiThread(() -> {
                                progressDownload.setProgress(progress);
                                tvDownloadStatus.setText("Загрузка: " + progress + "% (" + currentMB + " / " + totalMB + " МБ)");
                            });
                        }
                    }

                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    runOnUiThread(() -> {
                        isDownloading = false;
                        btnDownload.setEnabled(true);
                        btnDownload.setText("УСТАНОВИТЬ ПОВТОРНО");
                        progressDownload.setProgress(100);
                        tvDownloadStatus.setText("Загрузка завершена!");
                        CustomToast.showSuccess(UpdateActivity.this, "Файл загружен! Запуск установки...");
                        installApk(apkFile);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error saving APK", e);
                    runOnUiThread(() -> {
                        isDownloading = false;
                        btnDownload.setEnabled(true);
                        btnDownload.setText("СКАЧАТЬ И УСТАНОВИТЬ APK");
                        progressDownload.setVisibility(View.GONE);
                        tvDownloadStatus.setText("Ошибка сохранения файла");
                        CustomToast.showWarning(UpdateActivity.this, "Ошибка сохранения файла обновления");
                    });
                }
            }
        });
    }

    private void installApk(File apkFile) {
        try {
            if (!apkFile.exists()) {
                CustomToast.showWarning(this, "Файл APK не найден");
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package installer", e);
            CustomToast.showWarning(this, "Не удалось запустить установку обновления");
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            if (isoDate.contains("T")) {
                String datePart = isoDate.split("T")[0];
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    return parts[2] + "." + parts[1] + "." + parts[0];
                }
            }
            return isoDate;
        } catch (Exception e) {
            return isoDate;
        }
    }
}
