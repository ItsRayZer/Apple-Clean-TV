package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The clock screensaver's scene (layout dream_clock): the thin clock and date drifting over
 * a random MoeWalls video wallpaper (or local offline video pack, or ambient glow).
 * Shared by ClockDream (the system screensaver) and ClockActivity (Home left idle).
 */
final class ClockScene {

    private static final long MOVE_MS = 60_000L;
    /** The glow's colour change: 16 steps over 8 s. Animated per frame it redrew the full-screen gradient ~480 times. */
    private static final int GLOW_STEPS = 16;
    private static final long GLOW_STEP_MS = 500L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    private static final String[] MOEWALLS_ONLINE_URLS = {
            "https://moewalls.com/wp-content/uploads/preview/2026/arlecchino-crimson-moon-genshin-impact-preview-1.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/ichigo-bankai-blood-chains-eclipse-preview.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/sung-jinwoo-solo-leveling-preview.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/fern-purple-eyes-beyond-journeys-end-preview.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/plana-blue-hydrangea-blue-archive-preview.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/morning-light-forest-house-minecraft-preview.webm",
            "https://moewalls.com/wp-content/uploads/preview/2026/the-first-and-last-gift-wuthering-waves-preview.webm"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final ImageView ambient;
    private final TextureView videoView;
    private final GradientDrawable glow;
    private MediaPlayer videoPlayer;
    private Surface videoSurface;
    private int colorIndex;
    private int glowColor;
    /** clock_wallpaper or clock_glow false: something else (a picture, or plain black) replaced the coloured glow. */
    private boolean wallpaperShown;
    /** clock_wallpaper: the picture start() decodes, its darkening, and the resources to draw it with. */
    private File wallpaperFile;
    private int wallpaperShade;
    private Resources wallpaperRes;
    /** One number per decode request: a result whose number is no longer the current one is dropped. Main thread only. */
    private int wallpaperGeneration;
    /** clock_wallpaper with a collection: the pictures, how long each lasts, and how the next one is picked. */
    private List<File> rotation;
    private long rotationMs;
    private long shownMs;
    private String order;
    private Map<String, Set<String>> moods;
    private String latitude, longitude;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            advanceWallpaper();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** root is the inflated dream_clock layout. */
    ClockScene(View root) {
        content = root.findViewById(R.id.dreamContent);
        ambient = root.findViewById(R.id.dreamAmbient);
        videoView = root.findViewById(R.id.dreamVideo);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        colorIndex = random.nextInt(PALETTE.length);
        setGlow(PALETTE[colorIndex]);

        if (videoView != null) {
            videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    startScreensaverVideo(surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    stopVideo();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
        }
    }

    private static final long SCREENSAVER_ROTATION_INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes
    private int localVideoIndex = 0;

    private final Runnable rotateVideo = new Runnable() {
        @Override
        public void run() {
            if (videoView != null && videoView.isAvailable() && videoView.getSurfaceTexture() != null) {
                startScreensaverVideo(videoView.getSurfaceTexture());
            }
            handler.postDelayed(this, SCREENSAVER_ROTATION_INTERVAL_MS);
        }
    };

    void start() {
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MOVE_MS);
        handler.removeCallbacks(rotateVideo);
        handler.postDelayed(rotateVideo, SCREENSAVER_ROTATION_INTERVAL_MS);
        if (wallpaperFile != null && !wallpaperShown) loadWallpaper();

        if (videoView != null) {
            videoView.setVisibility(View.VISIBLE);
            if (videoView.isAvailable() && videoView.getSurfaceTexture() != null) {
                startScreensaverVideo(videoView.getSurfaceTexture());
            }
        }
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);   // the tick and any glow steps still queued
        handler.removeCallbacks(rotateVideo);
        content.animate().cancel();
        wallpaperGeneration++;   // a decode still running must not attach its picture to a stopped scene
        stopVideo();
    }

    private boolean isOnline(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<File> getLocalScreensaverVideos(Context context) {
        List<File> files = new ArrayList<>();
        File extDir = context != null ? context.getExternalFilesDir(null) : null;
        if (extDir != null) {
            File ssDir = new File(extDir, "screensavers");
            if (ssDir.exists() && ssDir.isDirectory()) {
                File[] list = ssDir.listFiles((dir, name) -> name.endsWith(".mp4"));
                if (list != null) {
                    for (File f : list) {
                        if (f.isFile() && f.length() > 1000) files.add(f);
                    }
                }
            }
            File wpDir = new File(extDir, "wallpapers");
            if (wpDir.exists() && wpDir.isDirectory()) {
                File[] list = wpDir.listFiles((dir, name) -> name.endsWith(".mp4"));
                if (list != null) {
                    for (File f : list) {
                        if (f.isFile() && f.length() > 1000) files.add(f);
                    }
                }
            }
        }
        return files;
    }

    private void startScreensaverVideo(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) return;
        Context ctx = content != null ? content.getContext() : null;
        List<File> localFiles = ctx != null ? getLocalScreensaverVideos(ctx) : new ArrayList<>();
        boolean online = isOnline(ctx);

        android.util.Log.i("TVHOME_SCREENSAVER", "startScreensaverVideo: online=" + online + ", localFiles=" + localFiles.size());

        if (online) {
            int idx = random.nextInt(MOEWALLS_ONLINE_URLS.length);
            String url = MOEWALLS_ONLINE_URLS[idx];
            android.util.Log.i("TVHOME_SCREENSAVER", "Attempting online stream: " + url);
            playVideo(surfaceTexture, url, null, localFiles);
        } else {
            playLocalFallback(surfaceTexture, localFiles);
        }
    }

    private void playVideo(SurfaceTexture surfaceTexture, String url, File localFile, List<File> localFallbacks) {
        stopVideo();
        try {
            videoSurface = new Surface(surfaceTexture);
            videoPlayer = new MediaPlayer();
            videoPlayer.setSurface(videoSurface);

            if (url != null) {
                videoPlayer.setDataSource(url);
            } else if (localFile != null) {
                videoPlayer.setDataSource(localFile.getAbsolutePath());
            } else {
                Context ctx = content != null ? content.getContext() : null;
                if (ctx != null) {
                    int rawId = ctx.getResources().getIdentifier("blood_moon_clouds", "raw", ctx.getPackageName());
                    if (rawId != 0) {
                        android.content.res.AssetFileDescriptor afd = ctx.getResources().openRawResourceFd(rawId);
                        if (afd != null) {
                            videoPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                            afd.close();
                        }
                    }
                }
            }

            videoPlayer.setLooping(true);
            videoPlayer.setVolume(0f, 0f);
            videoPlayer.setOnPreparedListener(mp -> {
                try {
                    if (ambient != null) ambient.setVisibility(View.GONE);
                    if (videoView != null) videoView.setVisibility(View.VISIBLE);
                    mp.start();
                    android.util.Log.i("TVHOME_SCREENSAVER", "Video screensaver successfully playing!");
                } catch (Exception e) {
                    android.util.Log.w("TVHOME_SCREENSAVER", "Error starting mp on prepared", e);
                }
            });

            videoPlayer.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.w("TVHOME_SCREENSAVER", "Video error: " + what + " " + extra + ", seamlessly switching to local fallback");
                stopVideo();
                playLocalFallback(surfaceTexture, localFallbacks);
                return true;
            });

            videoPlayer.prepareAsync();
        } catch (Exception e) {
            android.util.Log.w("TVHOME_SCREENSAVER", "Exception in playVideo", e);
            stopVideo();
            playLocalFallback(surfaceTexture, localFallbacks);
        }
    }

    private void playLocalFallback(SurfaceTexture surfaceTexture, List<File> localFiles) {
        if (localFiles == null || localFiles.isEmpty()) {
            Context ctx = content != null ? content.getContext() : null;
            localFiles = ctx != null ? getLocalScreensaverVideos(ctx) : new ArrayList<>();
        }
        if (localFiles != null && !localFiles.isEmpty()) {
            int idx = random.nextInt(localFiles.size());
            File f = localFiles.get(idx);
            android.util.Log.i("TVHOME_SCREENSAVER", "Playing offline MoeWalls video: " + f.getName());
            List<File> remaining = new ArrayList<>(localFiles);
            remaining.remove(f);
            playVideo(surfaceTexture, null, f, remaining);
        } else {
            Context ctx = content != null ? content.getContext() : null;
            if (ctx != null) {
                int rawId = ctx.getResources().getIdentifier("blood_moon_clouds", "raw", ctx.getPackageName());
                if (rawId != 0) {
                    android.util.Log.i("TVHOME_SCREENSAVER", "Playing bundled raw video fallback");
                    playVideo(surfaceTexture, null, null, null);
                    return;
                }
            }
            if (ambient != null) ambient.setVisibility(View.VISIBLE);
        }
    }

    void stopVideo() {
        if (videoPlayer != null) {
            try { videoPlayer.stop(); } catch (Exception ignored) {}
            try { videoPlayer.reset(); } catch (Exception ignored) {}
            try { videoPlayer.release(); } catch (Exception ignored) {}
            videoPlayer = null;
        }
        if (videoSurface != null) {
            try { videoSurface.release(); } catch (Exception ignored) {}
            videoSurface = null;
        }
    }

    /**
     * clock_wallpaper: put the picture Home is showing behind the clock, darkened by clock_wallpaper_dim, in
     * place of the glow, and carry a collection's rotation on while the screensaver is up. Decoded off the main
     * thread at screen size in RGB_565 (4 MB at 1080p), like Home's own; until then, or if it cannot be read,
     * the glow stays.
     * Call before start(), which starts the decode (again after a stop() that dropped an unfinished one).
     */
    void useHomeWallpaper(Context ctx) {
        Config cfg;
        try {
            cfg = Config.load(ctx);
        } catch (RuntimeException e) {
            return;
        }
        if (!cfg.clockWallpaper) {
            if (!cfg.clockGlow) {
                wallpaperShown = true;
                ambient.setBackground(null);
            }
            return;
        }
        if (cfg.wallpapers.isEmpty()) {
            wallpaperFile = null;
        } else {
            rotation = new ArrayList<>(cfg.wallpapers);
            String shown = ctx.getSharedPreferences("wallpaper_rotation", Context.MODE_PRIVATE)
                    .getString("image", "");
            File current = WallpaperRotation.find(rotation, shown);
            wallpaperFile = current != null ? current : rotation.get(0);
            rotationMs = cfg.wallpaperRotationMinutes * 60_000L;
            order = cfg.wallpaperOrder;
            moods = cfg.wallpaperMoods;
            latitude = moods.isEmpty() ? "" : ctx.getString(R.string.latitude);
            longitude = moods.isEmpty() ? "" : ctx.getString(R.string.longitude);
        }
        wallpaperShade = 0xFF000000 | (Math.round((1f - cfg.clockWallpaperDim) * 0xFF) * 0x010101);
        wallpaperRes = ctx.getResources();
    }

    private void loadWallpaper() {
        int gen = ++wallpaperGeneration;
        File f = wallpaperFile;
        DisplayMetrics dm = ambient.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        new Thread(() -> {
            Bitmap bm = decodeWallpaper(f, width, height);
            ambient.post(() -> {
                if (gen != wallpaperGeneration || bm == null) return;
                wallpaperShown = true;
                LayerDrawable ld = new LayerDrawable(new Drawable[]{
                        new BitmapDrawable(wallpaperRes, bm),
                        new ColorDrawable(wallpaperShade)
                });
                ambient.setImageDrawable(ld);
                ambient.setBackground(null);
            });
        }, "clock-wallpaper").start();
    }

    private static Bitmap decodeWallpaper(File f, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
        opts.inSampleSize = Math.max(1, Math.min(opts.outWidth / width, opts.outHeight / height));
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inJustDecodeBounds = false;
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /** Step to the next picture once its turn is up; the tick already runs every minute. */
    private void advanceWallpaper() {
        if (rotation == null || rotationMs <= 0 || rotation.size() < 2) return;
        shownMs += MOVE_MS;
        if (shownMs < rotationMs) return;
        shownMs = 0;
        File next = WallpaperRotation.pick(playable(), wallpaperFile.getName(), order, random);
        if (next.equals(wallpaperFile)) return;
        wallpaperFile = next;
        wallpaperShown = false;
        loadWallpaper();
    }

    private List<File> playable() {
        if (moods.isEmpty()) return rotation;
        LocalDateTime now = LocalDateTime.now();
        int[] sun = WallpaperMood.sun(now.toLocalDate(), latitude, longitude, ZoneId.systemDefault());
        return WallpaperMood.select(rotation, moods,
                WallpaperMood.timeOfDay(now.getHour() * 60 + now.getMinute(), sun[0], sun[1]));
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withLayer().withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).withLayer().start();
        }).start();
    }

    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        if (wallpaperShown) return;
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        int to = PALETTE[colorIndex];
        ArgbEvaluator mix = new ArgbEvaluator();
        for (int i = 1; i <= GLOW_STEPS; i++) {
            float f = (float) i / GLOW_STEPS;
            handler.postDelayed(() -> setGlow((int) mix.evaluate(f, from, to)), i * GLOW_STEP_MS);
        }
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
    }
}
