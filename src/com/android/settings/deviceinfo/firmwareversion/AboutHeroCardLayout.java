/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

/**
 * Hero card for the About phone screen. The background is a diagonal gradient whose hue cycles
 * through the RGB spectrum. Light mode uses soft pastel tones and dark mode uses deep, rich tones.
 * The animation stops when the view leaves the window, and it is not run when system animations
 * are turned off (animator duration scale is 0).
 */
public class AboutHeroCardLayout extends LinearLayout {

    private static final long CYCLE_MS = 9000L;
    private static final float CORNER_DP = 28f;

    private static final float LIGHT_SATURATION = 0.42f;
    private static final float LIGHT_BRIGHTNESS = 1f;
    private static final float DARK_SATURATION = 0.7f;
    private static final float DARK_BRIGHTNESS = 0.42f;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final float mCornerRadius;
    private final float[] mHsv = new float[3];
    private ValueAnimator mAnimator;
    private float mFraction;

    public AboutHeroCardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Draw our own background before the children.
        setWillNotDraw(false);
        mCornerRadius = CORNER_DP * context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimationIfEnabled();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width > 0 && height > 0) {
            final boolean dark = isNightMode();
            final float saturation = dark ? DARK_SATURATION : LIGHT_SATURATION;
            final float brightness = dark ? DARK_BRIGHTNESS : LIGHT_BRIGHTNESS;
            final float hue = mFraction * 360f;

            final int start = hsvColor(hue, saturation, brightness);
            final int middle = hsvColor(hue + 120f, saturation, brightness);
            final int end = hsvColor(hue + 240f, saturation, brightness);

            mPaint.setShader(new LinearGradient(
                    0f, 0f, width, height,
                    new int[] {start, middle, end},
                    new float[] {0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            mBounds.set(0f, 0f, width, height);
            canvas.drawRoundRect(mBounds, mCornerRadius, mCornerRadius, mPaint);
        }
        super.onDraw(canvas);
    }

    private int hsvColor(float hue, float saturation, float brightness) {
        mHsv[0] = hue % 360f;
        mHsv[1] = saturation;
        mHsv[2] = brightness;
        return Color.HSVToColor(mHsv);
    }

    private boolean isNightMode() {
        final int uiMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean areAnimationsEnabled() {
        try {
            final float scale = Settings.Global.getFloat(
                    getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return scale > 0f;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private void startAnimationIfEnabled() {
        if (mAnimator != null && mAnimator.isRunning()) {
            return;
        }
        if (!areAnimationsEnabled()) {
            mFraction = 0f;
            invalidate();
            return;
        }
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofFloat(0f, 1f);
            mAnimator.setDuration(CYCLE_MS);
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(animation -> {
                mFraction = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        mAnimator.start();
    }

    private void stopAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }
}
