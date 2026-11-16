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

package com.android.settings.fuelgauge

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.preference.PreferenceViewHolder
import com.android.settingslib.widget.UsageProgressBarPreference
import kotlin.random.Random

/**
 * Battery usage header. The fill inside the pill flows through a random RGB glow, and a rainbow
 * border rotates around the whole pill. Light mode uses brighter tones and dark mode uses richer
 * tones. The animation pauses when the bar leaves the window and is skipped when system
 * animations are turned off.
 */
class RainbowBatteryProgressPreference(context: Context, attrs: AttributeSet?) :
    UsageProgressBarPreference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val progressBar = holder.itemView.findProgressBar() ?: return
        applyRainbow(progressBar)
    }

    private fun applyRainbow(progressBar: ProgressBar) {
        // The bar keeps its animator as its tag, so each bar is wired up only once.
        if (progressBar.tag is ValueAnimator) return

        val uiMode = progressBar.context.resources.configuration.uiMode
        val isDark =
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val density = progressBar.resources.displayMetrics.density

        val overlay = RgbPillDrawable(
            borderWidthPx = BORDER_WIDTH_DP * density,
            glowRadiusPx = GLOW_RADIUS_DP * density,
            isDark = isDark,
            startHue = Random.nextFloat() * 360f,
        )
        progressBar.foreground = overlay

        if (!animationsEnabled(progressBar.context)) {
            overlay.setRotationFraction(0f)
            overlay.setProgressFraction(progressFraction(progressBar))
            return
        }

        val animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CYCLE_MS
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    overlay.setRotationFraction(it.animatedValue as Float)
                    overlay.setProgressFraction(progressFraction(progressBar))
                }
            }
        progressBar.tag = animator
        progressBar.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    if (!animator.isRunning) animator.start()
                }

                override fun onViewDetachedFromWindow(view: View) {
                    animator.cancel()
                }
            }
        )
        if (progressBar.isAttachedToWindow) animator.start()
    }

    private companion object {
        const val CYCLE_MS = 5000L
        const val BORDER_WIDTH_DP = 2.5f
        const val GLOW_RADIUS_DP = 6f

        fun progressFraction(progressBar: ProgressBar): Float =
            progressBar.progress.toFloat() / progressBar.max.coerceAtLeast(1)

        fun animationsEnabled(context: Context): Boolean =
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f

        fun View.findProgressBar(): ProgressBar? {
            if (this is ProgressBar) return this
            if (this is ViewGroup) {
                for (i in 0 until childCount) {
                    getChildAt(i).findProgressBar()?.let {
                        return it
                    }
                }
            }
            return null
        }
    }
}

/**
 * Drawn over the battery pill. It paints the filled part with a flowing RGB gradient and a soft
 * glow, and draws a rotating rainbow border around the whole pill.
 */
private class RgbPillDrawable(
    private val borderWidthPx: Float,
    private val glowRadiusPx: Float,
    private val isDark: Boolean,
    private val startHue: Float,
) : Drawable() {
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidthPx
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        alpha = GLOW_ALPHA
    }
    private val rect = RectF()
    private val fillRect = RectF()
    private val matrix = Matrix()
    private val borderColors = IntArray(RAINBOW_STOPS + 1) { index ->
        rgb(index * 360f / RAINBOW_STOPS)
    }
    private val fillColors = IntArray(RAINBOW_STOPS + 1) { index ->
        rgb(startHue + index * 360f / RAINBOW_STOPS)
    }
    private var borderShader: SweepGradient? = null
    private var fillShader: LinearGradient? = null
    private var rotationFraction = 0f
    private var progressFraction = 0f

    /** Turns the rainbow border and the fill colors, from 0 to 1 for one full cycle. */
    fun setRotationFraction(fraction: Float) {
        rotationFraction = fraction
        invalidateSelf()
    }

    /** Sets how much of the pill is filled, from 0 to 1. */
    fun setProgressFraction(fraction: Float) {
        progressFraction = fraction.coerceIn(0f, 1f)
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val inset = borderWidthPx / 2f
        rect.set(bounds)
        rect.inset(inset, inset)
        borderShader = SweepGradient(bounds.exactCenterX(), bounds.exactCenterY(), borderColors, null)
        fillShader = LinearGradient(
            0f,
            0f,
            rect.width(),
            0f,
            fillColors,
            null,
            Shader.TileMode.REPEAT,
        )
    }

    override fun draw(canvas: Canvas) {
        val radius = rect.height() / 2f

        // Filled part with a flowing RGB gradient and a glow behind it.
        val fill = fillShader
        val fillWidth = rect.width() * progressFraction
        if (fill != null && fillWidth > 0f) {
            fillRect.set(rect.left, rect.top, rect.left + fillWidth.coerceAtLeast(rect.height()), rect.bottom)
            matrix.setTranslate(
                rect.left - rotationFraction * rect.width(),
                rect.top,
            )
            fill.setLocalMatrix(matrix)
            fillPaint.shader = fill
            glowPaint.shader = fill
            canvas.drawRoundRect(fillRect, radius, radius, glowPaint)
            canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
        }

        // Rotating rainbow border around the whole pill.
        val sweep = borderShader ?: return
        matrix.setRotate(rotationFraction * 360f, bounds.exactCenterX(), bounds.exactCenterY())
        sweep.setLocalMatrix(matrix)
        borderPaint.shader = sweep
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        borderPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun rgb(hue: Float): Int {
        val saturation = if (isDark) 0.65f else 0.8f
        val brightness = if (isDark) 1f else 0.9f
        return Color.HSVToColor(floatArrayOf(hue % 360f, saturation, brightness))
    }

    private companion object {
        const val RAINBOW_STOPS = 12
        const val GLOW_ALPHA = 120
    }
}
