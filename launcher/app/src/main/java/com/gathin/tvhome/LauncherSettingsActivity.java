package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * AMOLED Black Apple TV-style split settings replicating ALL TV Settings:
 * Left side: Menu options list.
 * Right side: Large content area showing the currently selected page.
 * Navigation: UP/DOWN on left menu switches pages; RIGHT enters page; LEFT returns to menu.
 */
public final class LauncherSettingsActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";

    private SharedPreferences choices;

    // 16 Left Menu Items
    private TextView[] menuItems = new TextView[16];

    // 16 Right Pages
    private View[] pages = new View[16];

    // Controls
    private TextView tvWifiStatus;
    private TextView tvActiveProfileLabel;
    private TextView btnThemeDark, btnThemeClassic, btnThemeFluidGlass;
    private TextView clockFormatSystem, clockFormat12, clockFormat24;
    private TextView[] btnClockStyles = new TextView[7];
    private TextView[] columnChoices = new TextView[4];

    private int columns;
    private String clockFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_settings);

        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        clockFormat = config.clockFormat;

        initViews();
        setupNavigation();
        setupPageActions();
        refreshAll();
    }

    private void initViews() {
        // Left Menu items (16 categories)
        menuItems[0] = findViewById(R.id.menuNetwork);
        menuItems[1] = findViewById(R.id.menuDisplay);
        menuItems[2] = findViewById(R.id.menuSound);
        menuItems[3] = findViewById(R.id.menuApps);
        menuItems[4] = findViewById(R.id.menuStorage);
        menuItems[5] = findViewById(R.id.menuRemotes);
        menuItems[6] = findViewById(R.id.menuAccounts);
        menuItems[7] = findViewById(R.id.menuPrefs);
        menuItems[8] = findViewById(R.id.menuPrivacy);
        menuItems[9] = findViewById(R.id.menuAccessibility);
        menuItems[10] = findViewById(R.id.menuProfiles);
        menuItems[11] = findViewById(R.id.menuIcons);
        menuItems[12] = findViewById(R.id.menuWallpaper);
        menuItems[13] = findViewById(R.id.menuClock);
        menuItems[14] = findViewById(R.id.menuLayout);
        menuItems[15] = findViewById(R.id.menuSystem);

        // Right Content Pages (16 pages)
        pages[0] = findViewById(R.id.pageNetwork);
        pages[1] = findViewById(R.id.pageDisplay);
        pages[2] = findViewById(R.id.pageSound);
        pages[3] = findViewById(R.id.pageApps);
        pages[4] = findViewById(R.id.pageStorage);
        pages[5] = findViewById(R.id.pageRemotes);
        pages[6] = findViewById(R.id.pageAccounts);
        pages[7] = findViewById(R.id.pagePrefs);
        pages[8] = findViewById(R.id.pagePrivacy);
        pages[9] = findViewById(R.id.pageAccessibility);
        pages[10] = findViewById(R.id.pageProfiles);
        pages[11] = findViewById(R.id.pageIcons);
        pages[12] = findViewById(R.id.pageWallpaper);
        pages[13] = findViewById(R.id.pageClock);
        pages[14] = findViewById(R.id.pageLayout);
        pages[15] = findViewById(R.id.pageSystem);

        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        tvActiveProfileLabel = findViewById(R.id.tvActiveProfileLabel);

        btnThemeDark = findViewById(R.id.btnThemeDark);
        btnThemeClassic = findViewById(R.id.btnThemeClassic);
        btnThemeFluidGlass = findViewById(R.id.btnThemeFluidGlass);

        clockFormatSystem = findViewById(R.id.clockFormatSystem);
        for (int i = 0; i < 7; i++) {
            int resId = getResources().getIdentifier("btnClockStyle" + (i + 1), "id", getPackageName());
            btnClockStyles[i] = findViewById(resId);
            final int styleVal = i + 1;
            if (btnClockStyles[i] != null) {
                btnClockStyles[i].setOnClickListener(v -> {
                    AppleClockStyles.setClockStyle(this, styleVal);
                    notifyChanged();
                    refreshClockUI();
                    Toast.makeText(this, "Clock Style: " + AppleClockStyles.STYLE_NAMES[styleVal - 1], Toast.LENGTH_SHORT).show();
                });
            }
        }
        clockFormat12 = findViewById(R.id.clockFormat12);
        clockFormat24 = findViewById(R.id.clockFormat24);

        columnChoices[0] = findViewById(R.id.columns4);
        columnChoices[1] = findViewById(R.id.columns5);
        columnChoices[2] = findViewById(R.id.columns6);
        columnChoices[3] = findViewById(R.id.columns7);
    }

    private void setupNavigation() {
        for (int i = 0; i < menuItems.length; i++) {
            final int index = i;
            TextView item = menuItems[i];
            if (item == null) continue;

            item.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showPage(index);
                    v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).start();
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            });
            item.setOnClickListener(v -> showPage(index));

            // Press RIGHT on left menu item to move focus into the active right page
            item.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    focusRightPage(index);
                    return true;
                }
                return false;
            });
        }
    }

    private void showPage(int pageIndex) {
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] != null) {
                pages[i].setVisibility(i == pageIndex ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void focusRightPage(int pageIndex) {
        View page = pages[pageIndex];
        if (page != null) {
            View first = page.focusSearch(View.FOCUS_RIGHT);
            if (first != null) first.requestFocus();
            else page.requestFocus();
        }
    }

    private void setupPageActions() {
        // 1. Network Actions
        setupAction(R.id.btnOpenWifiSettings, Settings.ACTION_WIFI_SETTINGS);
        setupAction(R.id.btnOpenWirelessSettings, Settings.ACTION_WIRELESS_SETTINGS);
        setupAction(R.id.btnOpenDataUsage, "android.settings.DATA_ALERT_SETTINGS");

        // 2. Display & Picture
        setupAction(R.id.btnOpenDisplaySettings, "com.android.tv.settings.device.displaysound.DisplaySoundActivity", Settings.ACTION_DISPLAY_SETTINGS);
        setupAction(R.id.btnOpenScreenResolution, Settings.ACTION_DISPLAY_SETTINGS);
        setupAction(R.id.btnOpenColorSpace, Settings.ACTION_DISPLAY_SETTINGS);

        // 3. Sound & Audio
        setupAction(R.id.btnOpenSoundSettings, Settings.ACTION_SOUND_SETTINGS);
        setupAction(R.id.btnOpenVolumePanel, "android.settings.panel.action.VOLUME", Settings.ACTION_SOUND_SETTINGS);
        setupAction(R.id.btnOpenDolbyAudio, Settings.ACTION_SOUND_SETTINGS);

        // 4. Apps & Permissions
        setupAction(R.id.btnOpenAllApps, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        setupAction(R.id.btnOpenDefaultApps, "android.settings.MANAGE_DEFAULT_APPS_SETTINGS", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        setupAction(R.id.btnOpenUnknownSources, Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        setupAction(R.id.btnOpenSpecialAppAccess, "android.settings.USAGE_ACCESS_SETTINGS", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);

        // 5. Storage & Reset
        setupAction(R.id.btnOpenStorageSettings, Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        setupAction(R.id.btnOpenFactoryReset, "android.settings.FACTORY_RESET_TV_OEM_LINK", Settings.ACTION_PRIVACY_SETTINGS);

        // 6. Remotes & Accessories
        setupAction(R.id.btnAddAccessory, "com.google.android.intent.action.CONNECT_INPUT", Settings.ACTION_BLUETOOTH_SETTINGS);
        setupAction(R.id.btnOpenBluetoothSettings, Settings.ACTION_BLUETOOTH_SETTINGS);

        // 7. Accounts & Sign-in
        setupAction(R.id.btnAddAccount, "android.settings.ADD_ACCOUNT_SETTINGS", Settings.ACTION_SYNC_SETTINGS);
        setupAction(R.id.btnAccountSync, Settings.ACTION_SYNC_SETTINGS);

        // 8. Device Preferences
        setupAction(R.id.btnOpenDateSettings, Settings.ACTION_DATE_SETTINGS);
        setupAction(R.id.btnOpenLanguageSettings, Settings.ACTION_LOCALE_SETTINGS);
        setupAction(R.id.btnOpenKeyboardSettings, Settings.ACTION_INPUT_METHOD_SETTINGS);
        setupAction(R.id.btnOpenAmbientSettings, "com.google.android.tv.settings.ambient", Settings.ACTION_DISPLAY_SETTINGS);
        setupAction(R.id.btnOpenPowerSettings, "com.google.android.pano.action.SLEEP", Settings.ACTION_DISPLAY_SETTINGS);

        // 9. Privacy & Security
        setupAction(R.id.btnOpenSecuritySettings, Settings.ACTION_SECURITY_SETTINGS);
        setupAction(R.id.btnOpenLocationSettings, Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        setupAction(R.id.btnOpenPrivacySettings, Settings.ACTION_PRIVACY_SETTINGS);

        // 10. Accessibility
        setupAction(R.id.btnOpenCaptionSettings, Settings.ACTION_CAPTIONING_SETTINGS);
        setupAction(R.id.btnOpenAccessibilitySettings, Settings.ACTION_ACCESSIBILITY_SETTINGS);

        // 11. Profile Selection Actions
        findViewById(R.id.btnSelectProfileFamily).setOnClickListener(v -> {
            ProfileManager.applyProfile(this, ProfileManager.PROFILE_FAMILY);
            refreshAll();
            Toast.makeText(this, "Profile switched to Family", Toast.LENGTH_SHORT).show();
            notifyChanged();
        });
        findViewById(R.id.btnSelectProfileAben).setOnClickListener(v -> {
            ProfileManager.applyProfile(this, ProfileManager.PROFILE_ABEN);
            refreshAll();
            Toast.makeText(this, "Profile switched to Aben", Toast.LENGTH_SHORT).show();
            notifyChanged();
        });
        findViewById(R.id.btnSelectProfileAnan).setOnClickListener(v -> {
            ProfileManager.applyProfile(this, ProfileManager.PROFILE_ANAN);
            refreshAll();
            Toast.makeText(this, "Profile switched to Anan", Toast.LENGTH_SHORT).show();
            notifyChanged();
        });

        // 12. Icon Theme Choices (Dark Mode 16:9, OG Icons, Black Liquid Glass)
        if (btnThemeDark != null) {
            btnThemeDark.setOnClickListener(v -> {
                ProfileManager.setIconThemeForActiveProfile(this, GlassIconManager.THEME_DARK);
                GlassIconManager.clearCache();
                notifyChanged();
                refreshIconsUI();
                Toast.makeText(this, "Icons: Dark Mode 16:9", Toast.LENGTH_SHORT).show();
            });
        }
        btnThemeClassic.setOnClickListener(v -> {
            ProfileManager.setIconThemeForActiveProfile(this, GlassIconManager.THEME_CLASSIC);
            GlassIconManager.clearCache();
            notifyChanged();
            refreshIconsUI();
            Toast.makeText(this, "Icons: OG Original Icons", Toast.LENGTH_SHORT).show();
        });
        btnThemeFluidGlass.setOnClickListener(v -> {
            ProfileManager.setIconThemeForActiveProfile(this, GlassIconManager.THEME_FLUID_GLASS);
            GlassIconManager.clearCache();
            notifyChanged();
            refreshIconsUI();
            Toast.makeText(this, "Icons: Black Liquid Glass", Toast.LENGTH_SHORT).show();
        });

        // 13. Wallpaper & Screensaver Actions
        findViewById(R.id.btnLaunchScreensaverNow).setOnClickListener(v -> {
            try {
                Intent dream = new Intent("android.intent.action.MAIN");
                dream.setClassName("com.android.systemui", "com.android.systemui.Somnambulator");
                startActivity(dream);
            } catch (Exception e) {
                Toast.makeText(this, "Screensaver preview", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        findViewById(R.id.btnScreensaverRotationToggle).setOnClickListener(v -> {
            Toast.makeText(this, "Screensaver auto-rotates every 5 minutes", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnWallpaperPicker).setOnClickListener(v -> {
            Toast.makeText(this, "Live Blood Moon wallpaper is active", Toast.LENGTH_SHORT).show();
        });

        // 14. Clock Format
        clockFormatSystem.setOnClickListener(v -> setClockFormat("system"));
        clockFormat12.setOnClickListener(v -> setClockFormat("12"));
        clockFormat24.setOnClickListener(v -> setClockFormat("24"));

        // 15. Column Choices
        for (int i = 0; i < columnChoices.length; i++) {
            final int colVal = i + 4;
            columnChoices[i].setOnClickListener(v -> {
                if (colVal == columns) return;
                columns = colVal;
                ProfileManager.setColumnsForActiveProfile(this, colVal);
                notifyChanged();
                refreshLayoutUI();
            });
        }

        findViewById(R.id.btnReorderApps).setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra(EXTRA_REORDER, true);
            setResult(RESULT_OK, result);
            finish();
        });

        View btnManageWidgets = findViewById(R.id.btnManageWidgets);
        if (btnManageWidgets != null) {
            btnManageWidgets.setOnClickListener(v -> showManageWidgetsDialog());
        }
        View btnResetDefaultWidgets = findViewById(R.id.btnResetDefaultWidgets);
        if (btnResetDefaultWidgets != null) {
            btnResetDefaultWidgets.setOnClickListener(v -> {
                try {
                    File file = Cards.storeFile(this);
                    if (file.exists()) file.delete();
                    notifyChanged();
                    Toast.makeText(this, "Restored default widgets (clean dock)", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Default widgets active", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 16. System & About Actions
        setupAction(R.id.btnOpenDeviceInfo, Settings.ACTION_DEVICE_INFO_SETTINGS);
        setupAction(R.id.btnOpenSystemUpdate, "android.settings.SYSTEM_UPDATE_SETTINGS", Settings.ACTION_DEVICE_INFO_SETTINGS);
        setupAction(R.id.btnOpenDeveloperSettings, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        findViewById(R.id.btnRestartTV).setOnClickListener(v -> {
            try {
                startActivity(new Intent("com.android.tv.settings.REBOOT_DEVICE"));
            } catch (Exception e) {
                open(new Intent(Settings.ACTION_SETTINGS));
            }
        });
    }

    private void setupAction(int viewId, String action) {
        setupAction(viewId, action, Settings.ACTION_SETTINGS);
    }

    private void setupAction(int viewId, String action, String fallbackAction) {
        View view = findViewById(viewId);
        if (view == null) return;
        view.setOnClickListener(v -> {
            try {
                open(new Intent(action));
            } catch (Exception e) {
                try {
                    open(new Intent(fallbackAction));
                } catch (Exception ex) {
                    open(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
    }

    private void setClockFormat(String format) {
        if (format.equals(clockFormat)) return;
        clockFormat = format;
        ProfileManager.setClockFormatForActiveProfile(this, format);
        notifyChanged();
        refreshClockUI();
    }

    private void notifyChanged() {
        long rev = System.currentTimeMillis();
        try {
            rev = choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1;
        } catch (Exception e) {
            try {
                rev = choices.getInt(Config.SETTINGS_REVISION_KEY, 0) + 1;
            } catch (Exception ignored) {}
        }
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY, rev).apply();
        setResult(RESULT_OK);
    }

    private void refreshAll() {
        Config config = Config.load(this);
        columns = config.columns;
        clockFormat = config.clockFormat;
        refreshNetworkUI();
        refreshProfilesUI();
        refreshIconsUI();
        refreshClockUI();
        refreshLayoutUI();
    }

    private void refreshNetworkUI() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps = cm != null ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
            boolean connected = caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            if (connected) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String ssid = (info != null && info.getSSID() != null && !info.getSSID().contains("unknown"))
                        ? info.getSSID().replace("\"", "") : "Connected";
                tvWifiStatus.setText("Connected: " + ssid);
                tvWifiStatus.setTextColor(0xFF4DFF88);
            } else {
                tvWifiStatus.setText("Not connected to Wi-Fi");
                tvWifiStatus.setTextColor(0xFFFF5555);
            }
        } catch (Exception e) {
            tvWifiStatus.setText("Wi-Fi: Ready");
        }
    }

    private void refreshProfilesUI() {
        String active = ProfileManager.getActiveProfile(this);
        tvActiveProfileLabel.setText("Active Profile: " + ProfileManager.getProfileDisplayName(this, active));
    }

    private void refreshIconsUI() {
        String theme = GlassIconManager.getIconTheme(choices);
        boolean isDark = GlassIconManager.THEME_DARK.equals(theme);
        boolean isClassic = GlassIconManager.THEME_CLASSIC.equals(theme);
        boolean isFluid = GlassIconManager.THEME_FLUID_GLASS.equals(theme);

        if (btnThemeDark != null) {
            btnThemeDark.setCompoundDrawablesWithIntrinsicBounds(
                    isDark ? R.drawable.ic_radio_on : R.drawable.ic_radio_off, 0, 0, 0);
            btnThemeDark.setAlpha(isDark ? 1.0f : 0.7f);
        }
        btnThemeClassic.setCompoundDrawablesWithIntrinsicBounds(
                isClassic ? R.drawable.ic_radio_on : R.drawable.ic_radio_off, 0, 0, 0);
        btnThemeFluidGlass.setCompoundDrawablesWithIntrinsicBounds(
                isFluid ? R.drawable.ic_radio_on : R.drawable.ic_radio_off, 0, 0, 0);

        btnThemeClassic.setAlpha(isClassic ? 1.0f : 0.7f);
        btnThemeFluidGlass.setAlpha(isFluid ? 1.0f : 0.7f);
    }

    private void refreshClockUI() {
        clockFormatSystem.setAlpha("system".equals(clockFormat) ? 1.0f : 0.6f);
        clockFormat12.setAlpha("12".equals(clockFormat) ? 1.0f : 0.6f);
        clockFormat24.setAlpha("24".equals(clockFormat) ? 1.0f : 0.6f);

        int currentStyle = AppleClockStyles.getClockStyle(this);
        for (int i = 0; i < 7; i++) {
            if (btnClockStyles[i] != null) {
                boolean active = (i + 1 == currentStyle);
                btnClockStyles[i].setAlpha(active ? 1.0f : 0.65f);
                btnClockStyles[i].setCompoundDrawablesWithIntrinsicBounds(
                        active ? R.drawable.ic_radio_on : R.drawable.ic_radio_off, 0, 0, 0);
            }
        }
    }

    private void refreshLayoutUI() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setAlpha((i + 4 == columns) ? 1.0f : 0.6f);
        }
    }

    private void open(Intent intent) {
        if (intent == null) return;
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            } catch (Throwable ignored) {
                Toast.makeText(this, "Settings unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showManageWidgetsDialog() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> declared;
        try {
            declared = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Cards.CATEGORY),
                    PackageManager.GET_META_DATA);
        } catch (Exception e) {
            declared = Collections.emptyList();
        }
        if (declared.isEmpty()) {
            Toast.makeText(this, "No add-on widgets installed", Toast.LENGTH_SHORT).show();
            return;
        }
        File store = Cards.storeFile(this);
        Map<String, Cards.Record> records = Cards.load(store);

        List<String> labels = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        List<Boolean> isShown = new ArrayList<>();

        for (ResolveInfo ri : declared) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = String.valueOf(ri.loadLabel(pm));
            Cards.Record rec = records.get(pkg);
            boolean shown = rec != null && Cards.DECISION_SHOW.equals(rec.decision);
            pkgs.add(pkg);
            isShown.add(shown);
            labels.add(label + (shown ? "  [Selected / Shown]" : "  [Removed / Hidden]"));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select & Remove Widgets");
        String[] items = labels.toArray(new String[0]);
        builder.setItems(items, (dialog, which) -> {
            String pkg = pkgs.get(which);
            boolean currentlyShown = isShown.get(which);
            boolean newDecision = !currentlyShown;
            try {
                String digest = Cards.digest(pm, pkg);
                int bracketIdx = labels.get(which).indexOf("  [");
                String cleanLbl = bracketIdx != -1 ? labels.get(which).substring(0, bracketIdx) : labels.get(which);
                Cards.Card card = new Cards.Card(pkg, null, cleanLbl, cleanLbl, null, 0, digest,
                        newDecision ? Cards.State.SHOWN : Cards.State.HIDDEN);
                Cards.decide(store, card, newDecision);
                notifyChanged();
                Toast.makeText(this, (newDecision ? "Selected: " : "Removed: ") + cleanLbl, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Done", null);
        builder.show();
    }
}