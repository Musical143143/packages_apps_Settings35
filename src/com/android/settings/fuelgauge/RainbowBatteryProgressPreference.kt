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
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.preference.PreferenceViewHolder
import com.android.settingslib.widget.UsageProgressBarPreference

/**
 * Battery usage header whose fill bar cycles through the RGB spectrum. Only the progress fill
 * changes color; the track and text stay as they are. Light mode uses brighter tones and dark
 * mode uses richer tones. The animation pauses when the bar leaves the window and is skipped
 * when system animations are turned off.
 */
class RainbowBatteryProgressPreference(context: Context) : UsageProgressBarPreference(context) {

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

        if (!animationsEnabled(progressBar.context)) {
            progressBar.progressTintList = ColorStateList.valueOf(rainbowColor(0f, isDark))
            return
        }

        val animator =
            ValueAnimator.ofFloat(0f, 360f).apply {
                duration = CYCLE_MS
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val hue = it.animatedValue as Float
                    progressBar.progressTintList =
                        ColorStateList.valueOf(rainbowColor(hue, isDark))
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
        const val CYCLE_MS = 6000L

        fun rainbowColor(hue: Float, isDark: Boolean): Int {
            val saturation = if (isDark) 0.65f else 0.8f
            val brightness = if (isDark) 1f else 0.85f
            return Color.HSVToColor(floatArrayOf(hue % 360f, saturation, brightness))
        }

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
