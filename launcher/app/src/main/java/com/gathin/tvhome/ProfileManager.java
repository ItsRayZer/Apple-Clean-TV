package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages user profiles on the TV launcher:
 * - Dynamic profiles (Family, Aben, Anan, custom added)
 * - Custom avatar pack (Batman, Luffy, Spider-Man, Goku, Naruto, Pikachu, Family)
 * - Auto-assign: Batman to Aben, Luffy to Anan, Family keeps default
 * - Isolated per-profile preferences (columns, clock format, icon theme)
 * - Auto-select toggle and delay in settings (off by default)
 * - Premium slowly moving ambient background color shifting
 */
public final class ProfileManager {
    public static final String PROFILE_FAMILY = "family";
    public static final String PROFILE_ABEN = "aben";
    public static final String PROFILE_ANAN = "anan";

    public static final String PREF_ACTIVE_PROFILE = "active_profile";
    public static final String PREF_PROFILES_LIST = "profiles_list";

    public static final String[] AVATAR_KEYS = {
            "batman", "spiderman", "luffy", "goku", "naruto", "pikachu"
    };

    public static String getAvatarDisplayName(String avatarId) {
        if (avatarId == null) return "Family";
        switch (avatarId.toLowerCase().trim().replace('-', '_')) {
            case "batman":
            case "avatar_batman":
                return "Batman";
            case "luffy":
            case "avatar_luffy":
                return "Luffy";
            case "spiderman":
            case "spider_man":
            case "avatar_spiderman":
                return "Spider-Man";
            case "goku":
            case "avatar_goku":
                return "Goku";
            case "naruto":
            case "avatar_naruto":
                return "Naruto";
            case "pikachu":
            case "avatar_pikachu":
                return "Pikachu";
            case "family":
            default:
                return "Family";
        }
    }

    public static Drawable loadAvatarDrawable(Context context, String avatarId) {
        if (context == null || avatarId == null) return null;
        String id = avatarId.toLowerCase().trim().replace('-', '_');

        int resId = 0;
        switch (id) {
            case "batman":
            case "avatar_batman":
            case "aben":
                resId = R.drawable.avatar_batman;
                break;
            case "luffy":
            case "avatar_luffy":
            case "anan":
                resId = R.drawable.avatar_luffy;
                break;
            case "spiderman":
            case "spider_man":
            case "avatar_spiderman":
                resId = R.drawable.avatar_spiderman;
                break;
            case "goku":
            case "avatar_goku":
                resId = R.drawable.avatar_goku;
                break;
            case "naruto":
            case "avatar_naruto":
                resId = R.drawable.avatar_naruto;
                break;
            case "pikachu":
            case "avatar_pikachu":
                resId = R.drawable.avatar_pikachu;
                break;
            case "family":
                resId = R.drawable.ic_profile_family;
                break;
            default:
                // Check external storage or custom path first
                File f = new File(context.getFilesDir(), "avatars/" + id + ".png");
                if (f.exists()) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                        if (bmp != null) return new BitmapDrawable(context.getResources(), bmp);
                    } catch (Throwable ignored) {}
                }
                try {
                    resId = context.getResources().getIdentifier("avatar_" + id, "drawable", context.getPackageName());
                    if (resId == 0) {
                        resId = context.getResources().getIdentifier(id, "drawable", context.getPackageName());
                    }
                } catch (Throwable ignored) {}
                break;
        }

        if (resId != 0) {
            try {
                return ContextCompat.getDrawable(context, resId);
            } catch (Throwable ignored) {}
        }
        return ContextCompat.getDrawable(context, R.drawable.ic_profile_family);
    }

    public static int getAvatarDrawableRes(String avatarId) {
        if (avatarId == null) return R.drawable.ic_profile_family;
        switch (avatarId.toLowerCase().trim().replace('-', '_')) {
            case "batman":
            case "avatar_batman":
            case "aben":
                return R.drawable.avatar_batman;
            case "luffy":
            case "avatar_luffy":
            case "anan":
                return R.drawable.avatar_luffy;
            case "spiderman":
            case "spider_man":
            case "avatar_spiderman":
                return R.drawable.avatar_spiderman;
            case "goku":
            case "avatar_goku":
                return R.drawable.avatar_goku;
            case "naruto":
            case "avatar_naruto":
                return R.drawable.avatar_naruto;
            case "pikachu":
            case "avatar_pikachu":
                return R.drawable.avatar_pikachu;
            case "family":
            default:
                return R.drawable.ic_profile_family;
        }
    }

    public static int getProfileAccentColor(String avatarOrProfileId) {
        if (avatarOrProfileId == null) return 0xFF142036;
        switch (avatarOrProfileId.toLowerCase().trim().replace('-', '_')) {
            case "batman":
            case "avatar_batman":
            case "aben":
                return 0xFF181D28; // Deep Dark Knight Obsidian Slate
            case "luffy":
            case "avatar_luffy":
            case "anan":
                return 0xFF162334; // Deep Maritime Navy
            case "spiderman":
            case "avatar_spiderman":
                return 0xFF251622; // Deep Obsidian Crimson
            case "goku":
            case "avatar_goku":
                return 0xFF261D16; // Deep Charcoal Bronze
            case "naruto":
            case "avatar_naruto":
                return 0xFF241A14; // Deep Burnt Umber
            case "pikachu":
            case "avatar_pikachu":
                return 0xFF242014; // Deep Antique Amber
            case "family":
            default:
                return 0xFF142036; // Luxurious Midnight Sapphire
        }
    }

    public static List<String> getProfileList(Context context) {
        if (context == null) return new ArrayList<>(Arrays.asList(PROFILE_FAMILY, PROFILE_ABEN, PROFILE_ANAN));
        SharedPreferences mainPrefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        String raw = mainPrefs.getString(PREF_PROFILES_LIST, "family,aben,anan");
        String[] parts = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim().toLowerCase();
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                list.add(trimmed);
            }
        }
        if (list.isEmpty()) {
            list.add(PROFILE_FAMILY);
            list.add(PROFILE_ABEN);
            list.add(PROFILE_ANAN);
        }
        return list;
    }

    public static void saveProfileList(Context context, List<String> list) {
        if (context == null || list == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_PROFILES_LIST, sb.toString()).apply();
    }

    public static String getActiveProfile(Context context) {
        if (context == null) return PROFILE_FAMILY;
        SharedPreferences mainPrefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        try {
            return mainPrefs.getString(PREF_ACTIVE_PROFILE, PROFILE_FAMILY);
        } catch (Exception e) {
            return PROFILE_FAMILY;
        }
    }

    public static String getProfileDisplayName(Context context, String profileId) {
        if (context != null) {
            SharedPreferences p = getProfilePrefs(context, profileId);
            String custom = p.getString("display_name", null);
            if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        }
        if (PROFILE_ABEN.equalsIgnoreCase(profileId)) return "Aben";
        if (PROFILE_ANAN.equalsIgnoreCase(profileId)) return "Anan";
        if (PROFILE_FAMILY.equalsIgnoreCase(profileId)) return "Family";
        return profileId.substring(0, 1).toUpperCase() + profileId.substring(1);
    }

    public static void setProfileDisplayName(Context context, String profileId, String name) {
        if (context == null || profileId == null || name == null) return;
        getProfilePrefs(context, profileId).edit().putString("display_name", name.trim()).apply();
    }

    public static String getProfileAvatar(Context context, String profileId) {
        if (context != null) {
            SharedPreferences p = getProfilePrefs(context, profileId);
            String custom = p.getString("avatar_id", null);
            if (custom != null && !custom.trim().isEmpty()) return custom;
        }
        if (PROFILE_ABEN.equalsIgnoreCase(profileId)) return "batman";
        if (PROFILE_ANAN.equalsIgnoreCase(profileId)) return "luffy";
        if (PROFILE_FAMILY.equalsIgnoreCase(profileId)) {
            String[] pool = {"spiderman", "goku", "naruto", "pikachu"};
            String chosen = pool[Math.abs(profileId.hashCode()) % pool.length];
            if (context != null) {
                getProfilePrefs(context, profileId).edit().putString("avatar_id", chosen).apply();
            }
            return chosen;
        }
        return "batman";
    }

    public static void setProfileAvatar(Context context, String profileId, String avatarId) {
        if (context == null || profileId == null || avatarId == null) return;
        getProfilePrefs(context, profileId).edit().putString("avatar_id", avatarId).apply();
    }

    public static String getDefaultIconThemeForProfile(String profileId) {
        return GlassIconManager.THEME_CLASSIC; // "Icons" OG original app icons is default for all profiles
    }

    public static int getDefaultColumnsForProfile(String profileId) {
        if (PROFILE_ABEN.equalsIgnoreCase(profileId)) return 6;
        if (PROFILE_ANAN.equalsIgnoreCase(profileId)) return 4;
        return 5;
    }

    public static String getDefaultClockFormatForProfile(String profileId) {
        if (PROFILE_ABEN.equalsIgnoreCase(profileId)) return "12";
        if (PROFILE_ANAN.equalsIgnoreCase(profileId)) return "24";
        return "system";
    }

    public static SharedPreferences getProfilePrefs(Context context, String profileId) {
        if (profileId == null) profileId = PROFILE_FAMILY;
        return context.getSharedPreferences("profile_" + profileId.toLowerCase().trim(), Context.MODE_PRIVATE);
    }

    public static void applyProfile(Context context, String profileId) {
        if (context == null) return;
        if (profileId == null) profileId = PROFILE_FAMILY;
        profileId = profileId.toLowerCase().trim();

        SharedPreferences mainPrefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences profilePrefs = getProfilePrefs(context, profileId);

        // Ensure defaults exist for this profile
        if (!profilePrefs.contains("icon_theme")) {
            profilePrefs.edit().putString("icon_theme", getDefaultIconThemeForProfile(profileId)).apply();
        }
        if (!profilePrefs.contains("columns")) {
            profilePrefs.edit().putInt("columns", getDefaultColumnsForProfile(profileId)).apply();
        }
        if (!profilePrefs.contains(Config.CLOCK_FORMAT_KEY)) {
            profilePrefs.edit().putString(Config.CLOCK_FORMAT_KEY, getDefaultClockFormatForProfile(profileId)).apply();
        }

        String iconTheme = profilePrefs.getString("icon_theme", getDefaultIconThemeForProfile(profileId));
        int columns = profilePrefs.getInt("columns", getDefaultColumnsForProfile(profileId));
        String clockFormat = profilePrefs.getString(Config.CLOCK_FORMAT_KEY, getDefaultClockFormatForProfile(profileId));

        long rev = System.currentTimeMillis();
        try {
            rev = mainPrefs.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1;
        } catch (Exception e) {
            try {
                rev = mainPrefs.getInt(Config.SETTINGS_REVISION_KEY, 0) + 1;
            } catch (Exception ignored) {}
        }

        // Sync isolated profile preferences into main active settings store
        mainPrefs.edit()
                .putString(PREF_ACTIVE_PROFILE, profileId)
                .putString("icon_theme", iconTheme)
                .putInt("columns", columns)
                .putString(Config.CLOCK_FORMAT_KEY, clockFormat)
                .putLong(Config.SETTINGS_REVISION_KEY, rev)
                .apply();

        GlassIconManager.clearCache();
    }

    public static void setIconThemeForActiveProfile(Context context, String theme) {
        if (context == null) return;
        String active = getActiveProfile(context);
        getProfilePrefs(context, active).edit().putString("icon_theme", theme).apply();
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString("icon_theme", theme).apply();
        GlassIconManager.clearCache();
    }

    public static void setColumnsForActiveProfile(Context context, int columns) {
        if (context == null) return;
        String active = getActiveProfile(context);
        getProfilePrefs(context, active).edit().putInt("columns", columns).apply();
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putInt("columns", columns).apply();
    }

    public static void setClockFormatForActiveProfile(Context context, String format) {
        if (context == null) return;
        String active = getActiveProfile(context);
        getProfilePrefs(context, active).edit().putString(Config.CLOCK_FORMAT_KEY, format).apply();
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString(Config.CLOCK_FORMAT_KEY, format).apply();
    }
}
