package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Ensures Home activity is warm and ready on boot, so the live wallpaper and home UI
 * are active as soon as the TV starts up.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "TVHOME_BOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "BootReceiver received action: " + action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            try {
                context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean("just_booted", true).apply();
                Intent homeIntent = new Intent(context, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(homeIntent);
                Log.i(TAG, "MainActivity launched from BootReceiver");
            } catch (Exception e) {
                Log.w(TAG, "Failed launching MainActivity from BootReceiver", e);
            }
        }
    }
}
