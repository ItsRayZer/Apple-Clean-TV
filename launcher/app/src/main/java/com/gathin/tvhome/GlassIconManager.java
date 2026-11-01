package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages app icons with four distinct themes:
 * 1. DARK (default): iPhone dark mode style in 16:9 960x540 (.webp in res/drawable-nodpi/ or icons_dark folder).
 * 2. CLASSIC: each app's original real icon, exactly as the app ships it — full colour, unchanged.
 * 3. TV RATIO: full-colour icons in proper TV icon ratio (16:9 banner style, as Android TV uses).
 * 4. FLUID GLASS: the black liquid glass set (.webp first from storage or bundled in drawable-nodpi).
 */
public final class GlassIconManager {

    public static final String THEME_DARK = "dark"; // "Dark" - 16:9 iPhone dark mode style
    public static final String THEME_CLASSIC = "classic"; // "Icons" - OG original app icons (DEFAULT)
    public static final String THEME_TV_RATIO = "tv_ratio"; // "16:9 TV Icons"
    public static final String THEME_FLUID_GLASS = "fluid_glass"; // "Black liquid glass"

    private static final Map<String, Drawable> CACHE = new HashMap<>();

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    public static String getIconTheme(SharedPreferences prefs) {
        if (prefs == null) return THEME_CLASSIC;
        try {
            String theme = prefs.getString("icon_theme", THEME_CLASSIC);
            if (THEME_DARK.equals(theme)) return THEME_DARK;
            if (THEME_TV_RATIO.equals(theme)) return THEME_TV_RATIO;
            if (THEME_FLUID_GLASS.equals(theme)) return THEME_FLUID_GLASS;
            return THEME_CLASSIC; // DEFAULT
        } catch (Exception e) {
            return THEME_CLASSIC;
        }
    }

    public static synchronized Drawable getDarkIcon(Context ctx, String pkg) {
        if (pkg == null || ctx == null) return null;
        String cacheKey = "dark_" + pkg;
        Drawable cached = CACHE.get(cacheKey);
        if (cached != null) return cached;

        // 1. Check bundled drawable resource (resolves ic_dark_<pkg_with_underscores> in res/drawable-nodpi/)
        String resName = "ic_dark_" + pkg.replace('.', '_');
        int resId = ctx.getResources().getIdentifier(resName, "drawable", ctx.getPackageName());
        if (resId != 0) {
            try {
                Drawable resDrawable = ctx.getDrawable(resId);
                if (resDrawable != null) {
                    CACHE.put(cacheKey, resDrawable);
                    return resDrawable;
                }
            } catch (Exception ignored) {}
        }

        // 2. Check external storage icons_dark folder
        File iconsDir = new File(ctx.getExternalFilesDir(null), "icons_dark");
        File webpFile = new File(iconsDir, pkg + ".webp");
        File pngFile = new File(iconsDir, pkg + ".png");
        File customFile = (webpFile.exists() && webpFile.isFile()) ? webpFile
                : ((pngFile.exists() && pngFile.isFile()) ? pngFile : null);
        if (customFile != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(customFile.getAbsolutePath());
                if (bmp != null) {
                    Drawable customDrawable = new BitmapDrawable(ctx.getResources(), bmp);
                    CACHE.put(cacheKey, customDrawable);
                    return customDrawable;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static synchronized Drawable getGlassIcon(Context ctx, String pkg, Drawable rawIcon, float density) {
        if (pkg == null) pkg = "";
        Drawable cached = CACHE.get(pkg);
        if (cached != null) return cached;

        SharedPreferences prefs = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        String theme = getIconTheme(prefs);

        // If FLUID GLASS is selected, load the liquid glass WebP (or PNG) icon
        if (THEME_FLUID_GLASS.equals(theme)) {
            // 1. Check user-provided custom icon file on storage: .webp FIRST, then .png
            File iconsDir = new File(ctx.getExternalFilesDir(null), "icons");
            File webpFile = new File(iconsDir, pkg + ".webp");
            File pngFile = new File(iconsDir, pkg + ".png");
            File customFile = (webpFile.exists() && webpFile.isFile()) ? webpFile
                    : ((pngFile.exists() && pngFile.isFile()) ? pngFile : null);

            if (customFile != null) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(customFile.getAbsolutePath());
                    if (bmp != null) {
                        Drawable customDrawable = new BitmapDrawable(ctx.getResources(), bmp);
                        CACHE.put(pkg, customDrawable);
                        return customDrawable;
                    }
                } catch (Exception ignored) {
                }
            }

            // 2. Check bundled drawable resource (resolves .webp or .png in res/drawable-nodpi/)
            String resName = "ic_" + pkg.replace('.', '_');
            int resId = ctx.getResources().getIdentifier(resName, "drawable", ctx.getPackageName());
            if (resId != 0) {
                try {
                    Drawable resDrawable = ctx.getDrawable(resId);
                    if (resDrawable != null) {
                        CACHE.put(pkg, resDrawable);
                        return resDrawable;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Fallback or CLASSIC theme: each app's raw icon
        if (rawIcon != null) {
            CACHE.put(pkg, rawIcon);
            return rawIcon;
        }

        return new BitmapDrawable(ctx.getResources(), Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
    }
}
