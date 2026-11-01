package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Premium slowly moving gradient ambient background:
 * - AMOLED Black base (#05070A)
 * - Slowly moving soft radial gradient in luxurious, dark tones
 * - Smooth color interpolation on profile card focus
 * - 0% CPU strain, lightweight hardware accelerated drawing
 */
public class ProfileAmbientView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private int currentAccentColor = 0xFF142036; // Midnight sapphire default
    private int targetAccentColor = 0xFF142036;
    private ValueAnimator colorAnimator;
    private ValueAnimator driftAnimator;
    private float driftPhase = 0f;

    public ProfileAmbientView(Context context) {
        super(context);
        init();
    }

    public ProfileAmbientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProfileAmbientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        driftAnimator = ValueAnimator.ofFloat(0f, 1f);
        driftAnimator.setDuration(14000);
        driftAnimator.setRepeatCount(ValueAnimator.INFINITE);
        driftAnimator.setRepeatMode(ValueAnimator.REVERSE);
        driftAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        driftAnimator.addUpdateListener(anim -> {
            driftPhase = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (driftAnimator != null && !driftAnimator.isRunning()) {
            driftAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (driftAnimator != null) {
            driftAnimator.cancel();
        }
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
    }

    public void setAccentColor(int newColor) {
        if (targetAccentColor == newColor) return;
        targetAccentColor = newColor;
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentAccentColor, targetAccentColor);
        colorAnimator.setDuration(350);
        colorAnimator.addUpdateListener(anim -> {
            currentAccentColor = (int) anim.getAnimatedValue();
            invalidate();
        });
        colorAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. AMOLED base background
        canvas.drawColor(0xFF05070A);

        // 2. Slowly moving smooth radial gradient
        double angle = driftPhase * Math.PI;
        float cx = w * (0.42f + 0.16f * (float) Math.sin(angle));
        float cy = h * (0.38f + 0.14f * (float) Math.cos(angle));
        float radius = Math.max(w, h) * 0.70f;

        // Dark, rich luxurious tones:
        int[] colors = new int[] { currentAccentColor, 0xFF0A0E17, 0xFF05070A };
        float[] stops = new float[] { 0.0f, 0.50f, 1.0f };

        paint.setShader(new RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
    }
}
