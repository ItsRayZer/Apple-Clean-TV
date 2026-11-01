package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

/**
 * 7 Minimalistic Apple-style Clock Widget Designs:
 * 1. Ultra Minimal (Light, wide negative kerning, floating)
 * 2. Bold Classic (iOS 16 Heavy, confident geometry)
 * 3. Cupertino Stacked (Modular compact)
 * 4. Elegant Serif (California luxury editorial)
 * 5. Liquid Glass Capsule (Pill glass backdrop)
 * 6. Monospace Chrono (Tabular precision)
 * 7. Horizon Inline (Clean horizontal alignment)
 */
public final class AppleClockStyles {
    public static final String PREF_CLOCK_STYLE = "apple_clock_style";
    public static final int STYLE_MINIMAL = 1;
    public static final int STYLE_BOLD = 2;
    public static final int STYLE_STACKED = 3;
    public static final int STYLE_SERIF = 4;
    public static final int STYLE_GLASS_CARD = 5;
    public static final int STYLE_MONO = 6;
    public static final int STYLE_HORIZON = 7;

    public static final String[] STYLE_NAMES = {
            "1. Ultra Minimal",
            "2. Bold Classic",
            "3. Cupertino Stacked",
            "4. Elegant Serif",
            "5. Liquid Glass Capsule",
            "6. Monospace Chrono",
            "7. Horizon Inline"
    };

    public static int getClockStyle(Context context) {
        if (context == null) return STYLE_MINIMAL;
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_CLOCK_STYLE, STYLE_MINIMAL);
    }

    public static void setClockStyle(Context context, int style) {
        if (context == null || style < 1 || style > 7) return;
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(PREF_CLOCK_STYLE, style).apply();
    }

    public static void applyStyleToViews(Context context, View root, int style) {
        if (root == null || context == null) return;
        TextClock clock = root.findViewById(R.id.clock);
        if (clock == null) clock = root.findViewById(R.id.zenClock);
        if (clock == null) {
            // Check dream_clock TextClock
            View dreamContent = root.findViewById(R.id.dreamContent);
            if (dreamContent != null && dreamContent instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) dreamContent;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof TextClock) {
                        clock = (TextClock) child;
                        break;
                    } else if (child instanceof android.view.ViewGroup) {
                        android.view.ViewGroup row = (android.view.ViewGroup) child;
                        if (row.getChildCount() > 0 && row.getChildAt(0) instanceof TextClock) {
                            clock = (TextClock) row.getChildAt(0);
                            break;
                        }
                    }
                }
            }
        }
        if (clock == null) return;

        float density = context.getResources().getDisplayMetrics().density;
        switch (style) {
            case STYLE_BOLD: // 2. Bold Classic
                clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                clock.setLetterSpacing(-0.01f);
                clock.setTextColor(0xFFFFFFFF);
                clock.setBackground(null);
                break;
            case STYLE_STACKED: // 3. Cupertino Stacked
                clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                clock.setLetterSpacing(-0.02f);
                clock.setTextColor(0xFFF1F5F9);
                clock.setBackground(null);
                break;
            case STYLE_SERIF: // 4. California Elegant Serif
                clock.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                clock.setLetterSpacing(0.01f);
                clock.setTextColor(0xFFF8FAFC);
                clock.setBackground(null);
                break;
            case STYLE_GLASS_CARD: // 5. Liquid Glass Capsule
                clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                clock.setLetterSpacing(-0.02f);
                clock.setTextColor(0xFFFFFFFF);
                clock.setBackgroundResource(R.drawable.glass_card_bg);
                int p = (int) (12 * density);
                clock.setPadding(p * 2, p, p * 2, p);
                break;
            case STYLE_MONO: // 6. Monospace Chrono
                clock.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clock.setLetterSpacing(0.04f);
                clock.setTextColor(0xFFE2E8F0);
                clock.setBackground(null);
                break;
            case STYLE_HORIZON: // 7. Horizon Inline
                clock.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                clock.setLetterSpacing(0.02f);
                clock.setTextColor(0xFFFFFFFF);
                clock.setBackground(null);
                break;
            case STYLE_MINIMAL: // 1. Ultra Minimal
            default:
                clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                clock.setLetterSpacing(-0.04f);
                clock.setTextColor(0xE6FFFFFF);
                clock.setBackground(null);
                break;
        }
    }
}
