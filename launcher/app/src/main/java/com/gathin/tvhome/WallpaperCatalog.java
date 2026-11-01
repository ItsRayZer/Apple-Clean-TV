package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class WallpaperCatalog {
    public static final class Item {
        public final String id;
        public final String name;
        public final int resId;
        public final boolean isVideo;

        public Item(String id, String name, int resId, boolean isVideo) {
            this.id = id;
            this.name = name;
            this.resId = resId;
            this.isVideo = isVideo;
        }
    }

    private static final List<Item> ITEMS = new ArrayList<>();
    static {
        ITEMS.add(new Item("blood_moon", "Blood Moon Live", 0, true));
        ITEMS.add(new Item("dusk_peaks", "Dusk Peaks", R.drawable.wp_dusk_peaks, false));
        ITEMS.add(new Item("golden_coast", "Golden Coast", R.drawable.wp_golden_coast, false));
        ITEMS.add(new Item("moon_lake", "Moon Lake", R.drawable.wp_moon_lake, false));
        ITEMS.add(new Item("aurora", "Aurora Borealis", R.drawable.wp_aurora, false));
        ITEMS.add(new Item("dunes", "Desert Dunes", R.drawable.wp_dunes, false));
        ITEMS.add(new Item("clouds", "Above The Clouds", R.drawable.wp_clouds, false));
        ITEMS.add(new Item("mirror_lake", "Mirror Lake", R.drawable.wp_mirror_lake, false));
        ITEMS.add(new Item("tea_hills", "Tea Hills", R.drawable.wp_tea_hills, false));
        ITEMS.add(new Item("city_lights", "City Lights", R.drawable.wp_city_lights, false));
        ITEMS.add(new Item("red_rain", "Red Rain", R.drawable.wp_red_rain, false));
    }

    public static List<Item> getItems() {
        return ITEMS;
    }

    public static int getSelectedWallpaperIndex(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        return Math.max(0, Math.min(ITEMS.size() - 1, prefs.getInt("selected_wallpaper_index", 0)));
    }

    public static void setSelectedWallpaperIndex(Context context, int index) {
        if (index < 0 || index >= ITEMS.size()) return;
        SharedPreferences prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt("selected_wallpaper_index", index).apply();
    }

    public static Item getSelectedItem(Context context) {
        return ITEMS.get(getSelectedWallpaperIndex(context));
    }
}
