package com.example.animelib.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.example.animelib.R;

import java.io.File;
import java.util.Locale;

/**
 * Resolver for ASS/SSA subtitle fonts. Converts ASS font names (e.g. Arial, Trebuchet MS, Impact)
 * into styled Android Typeface instances with caching.
 */
public class FontResolver {
    private static final LruCache<String, Typeface> TYPEFACE_CACHE = new LruCache<>(64);

    @NonNull
    public static Typeface resolveTypeface(Context context, String fontName, boolean isBold, boolean isItalic) {
        if (fontName == null || fontName.trim().isEmpty()) {
            return getTypefaceForStyle(Typeface.DEFAULT, isBold, isItalic);
        }

        String cleanFontName = fontName.trim();
        String cacheKey = cleanFontName.toLowerCase(Locale.ROOT) + "_" + isBold + "_" + isItalic;
        Typeface cached = TYPEFACE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Typeface resolved = null;
        String lower = cleanFontName.toLowerCase(Locale.ROOT);

        // 1. Check bundled app fonts in R.font
        if (lower.contains("open sans") || lower.contains("opensans")) {
            try {
                if (isBold && isItalic) {
                    resolved = ResourcesCompat.getFont(context, R.font.open_sans_bold);
                } else if (isBold) {
                    resolved = ResourcesCompat.getFont(context, R.font.open_sans_bold);
                } else if (isItalic) {
                    resolved = ResourcesCompat.getFont(context, R.font.open_sans_italic);
                } else {
                    resolved = ResourcesCompat.getFont(context, R.font.open_sans);
                }
            } catch (Exception ignored) {}
        }

        // 2. Search assets/fonts directory if assets exist
        if (resolved == null && context != null) {
            try {
                String[] assetFiles = context.getAssets().list("fonts");
                if (assetFiles != null) {
                    for (String file : assetFiles) {
                        String fileLower = file.toLowerCase(Locale.ROOT);
                        if (fileLower.startsWith(lower) || fileLower.contains(lower)) {
                            resolved = Typeface.createFromAsset(context.getAssets(), "fonts/" + file);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Search local app files font directory
        if (resolved == null && context != null) {
            try {
                File fontDir = new File(context.getFilesDir(), "fonts");
                if (fontDir.exists() && fontDir.isDirectory()) {
                    File[] files = fontDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            String fLower = f.getName().toLowerCase(Locale.ROOT);
                            if (fLower.contains(lower)) {
                                resolved = Typeface.createFromFile(f);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 4. Map font family aliases and common ASS subtitle fonts
        if (resolved == null) {
            if (lower.contains("sans") || lower.contains("arial") || lower.contains("helvetica")
                    || lower.contains("roboto") || lower.contains("tahoma") || lower.contains("verdana")
                    || lower.contains("dejavu")) {
                resolved = Typeface.create("sans-serif", Typeface.NORMAL);
            } else if (lower.contains("serif") || lower.contains("times") || lower.contains("georgia")
                    || lower.contains("palatino")) {
                resolved = Typeface.create("serif", Typeface.NORMAL);
            } else if (lower.contains("mono") || lower.contains("courier") || lower.contains("consolas")) {
                resolved = Typeface.create("monospace", Typeface.NORMAL);
            } else if (lower.contains("impact") || lower.contains("black") || lower.contains("trebuchet")
                    || lower.contains("heavy") || lower.contains("bebas")) {
                resolved = Typeface.create("sans-serif-black", Typeface.BOLD);
            } else if (lower.contains("comic") || lower.contains("casual")) {
                resolved = Typeface.create("casual", Typeface.NORMAL);
            } else if (lower.contains("condensed") || lower.contains("narrow")) {
                resolved = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            } else {
                try {
                    resolved = Typeface.create(cleanFontName, Typeface.NORMAL);
                } catch (Exception ignored) {}
            }
        }

        if (resolved == null) {
            resolved = Typeface.DEFAULT;
        }

        resolved = getTypefaceForStyle(resolved, isBold, isItalic);
        TYPEFACE_CACHE.put(cacheKey, resolved);
        return resolved;
    }

    private static Typeface getTypefaceForStyle(Typeface base, boolean isBold, boolean isItalic) {
        int style = Typeface.NORMAL;
        if (isBold && isItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (isBold) {
            style = Typeface.BOLD;
        } else if (isItalic) {
            style = Typeface.ITALIC;
        }
        return Typeface.create(base, style);
    }
}
