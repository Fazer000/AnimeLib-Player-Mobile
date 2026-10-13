package com.example.animelib.util;

import com.example.animelib.R;
import com.example.animelib.AnimeLibApplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static volatile ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context appContext;

    private ImageLoader() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // 1/8 памяти под кэш
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static ImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) instance = new ImageLoader();
            }
        }
        return instance;
    }

    public static void init(Context context) {
        if (context != null) {
            getInstance().appContext = context.getApplicationContext();
        }
    }

    private Context getAppContext() {
        if (appContext == null) {
            try {
                appContext = AnimeLibApplication.getInstance();
            } catch (Exception ignored) {}
        }
        return appContext;
    }

    public void loadInto(ImageView imageView, String url, int placeholderResId) {
        if (imageView == null) return;
        int placeholder = placeholderResId != 0 ? placeholderResId : R.drawable.skeleton_placeholder;

        // Check memory cache first
        if (url != null && !url.isEmpty()) {
            Bitmap cached = memoryCache.get(url);
            if (cached != null) {
                stopShimmerIfPresent(imageView);
                imageView.setImageBitmap(cached);
                return;
            }
        }

        if (url == null || url.isEmpty()) {
            stopShimmerIfPresent(imageView);
            imageView.setImageResource(placeholder);
            return;
        }

        // Show animated rounded skeleton shimmer while loading
        try {
            boolean isDark = CustomToast.isDarkTheme(imageView.getContext());
            SkeletonHelper.SkeletonShimmerDrawable shimmer = new SkeletonHelper.SkeletonShimmerDrawable(8f, isDark);
            imageView.setImageDrawable(shimmer);
        } catch (Exception e) {
            imageView.setImageResource(placeholder);
        }

        imageView.setTag(url);

        executor.submit(() -> {
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap != null) memoryCache.put(url, bitmap);
            mainHandler.post(() -> {
                Object tag = imageView.getTag();
                if (tag != null && tag.equals(url)) {
                    stopShimmerIfPresent(imageView);
                    if (bitmap != null) {
                        imageView.setAlpha(0f);
                        imageView.setImageBitmap(bitmap);
                        imageView.animate().alpha(1f).setDuration(200).start();
                    } else {
                        imageView.setImageResource(placeholder);
                    }
                }
            });
        });
    }

    private void stopShimmerIfPresent(ImageView imageView) {
        if (imageView != null && imageView.getDrawable() instanceof SkeletonHelper.SkeletonShimmerDrawable) {
            ((SkeletonHelper.SkeletonShimmerDrawable) imageView.getDrawable()).stopAnimation();
        }
    }

    private Bitmap downloadBitmap(String src) {
        if (src == null || src.trim().isEmpty()) return null;

        // 1. Direct local file check
        if (src.startsWith("/")) {
            try {
                File f = new File(src);
                if (f.exists() && f.length() > 0) {
                    Bitmap bm = BitmapFactory.decodeFile(src);
                    if (bm != null) return bm;
                }
            } catch (Exception ignored) {}
        }
        if (src.startsWith("file://")) {
            try {
                String path = src.substring(7);
                File f = new File(path);
                if (f.exists() && f.length() > 0) {
                    Bitmap bm = BitmapFactory.decodeFile(path);
                    if (bm != null) return bm;
                }
            } catch (Exception ignored) {}
        }

        Context context = getAppContext();

        // 2. Poster directory check (by animeId / filename)
        if (context != null) {
            File posterFile = findPosterFileInDirs(context, src);
            if (posterFile != null) {
                try {
                    Bitmap bm = BitmapFactory.decodeFile(posterFile.getAbsolutePath());
                    if (bm != null) return bm;
                } catch (Exception ignored) {}
            }
        }

        // 3. Disk Cache check (by URL MD5 hash)
        File diskCacheFile = null;
        if (context != null) {
            try {
                File diskCacheDir = new File(context.getCacheDir(), "image_disk_cache");
                if (!diskCacheDir.exists()) diskCacheDir.mkdirs();
                diskCacheFile = new File(diskCacheDir, hashKeyForDisk(src));
                if (diskCacheFile.exists() && diskCacheFile.length() > 0) {
                    Bitmap bm = BitmapFactory.decodeFile(diskCacheFile.getAbsolutePath());
                    if (bm != null) return bm;
                }
            } catch (Exception ignored) {}
        }

        // 4. HTTP Download (if online)
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream fos = null;
        try {
            String urlStr = src;
            if (urlStr.startsWith("//")) {
                urlStr = "https:" + urlStr;
            } else if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                urlStr = "https://animelib.me" + (urlStr.startsWith("/") ? "" : "/") + urlStr;
            }
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            input = connection.getInputStream();

            if (diskCacheFile != null) {
                fos = new FileOutputStream(diskCacheFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                return BitmapFactory.decodeFile(diskCacheFile.getAbsolutePath());
            } else {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    private File findPosterFileInDirs(Context context, String src) {
        if (context == null || src == null) return null;
        try {
            String filename = src;
            int lastSlash = src.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < src.length() - 1) {
                filename = src.substring(lastSlash + 1);
            }
            int qIdx = filename.indexOf('?');
            if (qIdx > 0) filename = filename.substring(0, qIdx);

            File internalDir = new File(context.getFilesDir(), "cached_posters");
            File externalDir = context.getExternalFilesDir("cached_posters");

            List<File> dirs = new ArrayList<>();
            if (internalDir.exists()) dirs.add(internalDir);
            if (externalDir != null && externalDir.exists()) dirs.add(externalDir);

            for (File dir : dirs) {
                File exact = new File(dir, filename);
                if (exact.exists() && exact.length() > 0) return exact;

                if (!filename.endsWith(".jpg")) {
                    File jpg = new File(dir, filename + ".jpg");
                    if (jpg.exists() && jpg.length() > 0) return jpg;
                }

                String digits = src.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    File byId = new File(dir, digits + ".jpg");
                    if (byId.exists() && byId.length() > 0) return byId;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String hashKeyForDisk(String key) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(key.getBytes("UTF-8"));
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(key.hashCode());
        }
    }
}
