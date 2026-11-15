/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.settings.SettingsEnums.ACTION_BATTERY_LEVEL
import android.content.Context
import android.provider.Settings
import android.view.animation.DecelerateInterpolator
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import java.util.WeakHashMap
import com.android.settings.R
import com.android.settings.contract.KEY_BATTERY_LEVEL
import com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.Utils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.fuelgauge.BatteryUtils
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.UsageProgressBarPreference

// LINT.IfChange
class BatteryHeaderPreference :
    IntRangeValuePreference,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceActionMetricsProvider,
    PreferenceLifecycleProvider {

    @VisibleForTesting var batteryBroadcastReceiver: BatteryBroadcastReceiver? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.summary_placeholder

    override val indexable
        get() = false

    override val preferenceActionMetrics: Int
        get() = ACTION_BATTERY_LEVEL

    override fun tags(context: Context) = arrayOf(KEY_BATTERY_LEVEL)

    override fun createWidget(context: Context) = RainbowBatteryProgressPreference(context, null)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        if (preference is UsageProgressBarPreference) {
            quickUpdateHeaderPreference(preference)
        }
    }

    override fun isAvailable(context: Context) =
        com.android.settings.Utils.isBatteryPresent(context)

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        batteryBroadcastReceiver =
            BatteryBroadcastReceiver(context).apply {
                setBatteryChangedListener {
                    if (it != BATTERY_NOT_PRESENT) {
                        context.notifyPreferenceChange(KEY)
                    }
                }
            }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        batteryBroadcastReceiver?.register()
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        batteryBroadcastReceiver?.unRegister()
    }

    override fun storage(context: Context): KeyValueStore =
        object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = BatteryUtils.getBatteryIntent(context) != null

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
                val batteryIntent = BatteryUtils.getBatteryIntent(context) ?: return null
                return Utils.getBatteryLevel(batteryIntent) as T
            }

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
                throw UnsupportedOperationException()
        }

    override fun getMinValue(context: Context): Int = 0

    override fun getMaxValue(context: Context): Int = 100

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        private const val KEY = "battery_header"
        private const val BATTERY_MAX_LEVEL: Long = 100L
        private const val ANIMATION_DURATION_MS = 900L

        /** Preference extra holding the level currently drawn, so changes animate from it. */
        private const val EXTRA_SHOWN_LEVEL = "battery_header_shown_level"

        /** Running fill animations, so a newer level replaces an animation still in progress. */
        private val runningAnimations = WeakHashMap<Preference, ValueAnimator>()

        private fun quickUpdateHeaderPreference(preference: UsageProgressBarPreference) {
            val batteryIntent = BatteryUtils.getBatteryIntent(preference.context) ?: return
            val batteryLevel: Int = Utils.getBatteryLevel(batteryIntent)
            preference.setBottomSummary("")

            val shownLevel = preference.extras.getInt(EXTRA_SHOWN_LEVEL, -1)
            runningAnimations.remove(preference)?.cancel()
            if (shownLevel == batteryLevel) {
                applyLevel(preference, batteryLevel)
                return
            }
            // First draw counts up from empty; later changes start from the level on screen.
            val startLevel = if (shownLevel < 0) 0 else shownLevel
            if (!animationsEnabled(preference.context)) {
                applyLevel(preference, batteryLevel)
                return
            }
            val animator =
                ValueAnimator.ofInt(startLevel, batteryLevel).apply {
                    duration = ANIMATION_DURATION_MS
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { applyLevel(preference, it.animatedValue as Int) }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                runningAnimations.remove(preference)
                            }
                        }
                    )
                }
            runningAnimations[preference] = animator
            animator.start()
        }

        private fun applyLevel(preference: UsageProgressBarPreference, level: Int) {
            preference.extras.putInt(EXTRA_SHOWN_LEVEL, level)
            preference.setUsageSummary(com.android.settings.Utils.formatPercentage(level))
            preference.setPercent(level.toLong(), BATTERY_MAX_LEVEL)
        }

        /** Skip the animation when the user has turned system animations off. */
        private fun animationsEnabled(context: Context): Boolean =
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
    }
}
// LINT.ThenChange(BatteryHeaderPreferenceController.java)
