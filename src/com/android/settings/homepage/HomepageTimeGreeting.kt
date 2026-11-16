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

package com.android.settings.homepage

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import java.util.Calendar

private const val RGB_CYCLE_MS = 6000
private const val MOVE_HALF_CYCLE_MS = 2500
private const val GREETING_FONT_SIZE = 44
private const val RING_CYCLE_MS = 4000
private val RING_WIDTH = 3.dp

private val MORNING_GREETINGS = listOf(
    "Good morning",
    "Rise and shine",
    "Morning, sunshine",
    "Fresh start today",
    "Hello, early bird",
    "Let's make it great",
)
private val AFTERNOON_GREETINGS = listOf(
    "Good afternoon",
    "Hope your day is going well",
    "Keep going strong",
    "Hello again",
    "Time for a short break?",
    "Afternoon check-in",
)
private val EVENING_GREETINGS = listOf(
    "Good evening",
    "Relax, you earned it",
    "Winding down?",
    "Evening, friend",
    "Hope today was kind",
)
private val NIGHT_GREETINGS = listOf(
    "Good night",
    "Still up?",
    "Sleep well soon",
    "Quiet hours",
    "Late night, huh?",
)

/** Greetings that fit the current time of day. */
internal fun timeGreetingsFor(hour: Int): List<String> = when (hour) {
    in 5..11 -> MORNING_GREETINGS
    in 12..16 -> AFTERNOON_GREETINGS
    in 17..20 -> EVENING_GREETINGS
    else -> NIGHT_GREETINGS
}

private fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/** Human-readable name for the editor chips, such as "Serif bold". */
internal fun GreetingFont.label(): String =
    name.lowercase().split('_').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }

private data class Typeface(
    val family: FontFamily,
    val weight: FontWeight,
    val style: FontStyle,
    val spacing: TextUnit,
)

private fun GreetingFont.typeface(): Typeface = when (this) {
    GreetingFont.CLASSIC -> Typeface(FontFamily.SansSerif, FontWeight.Medium, FontStyle.Normal, 0.sp)
    GreetingFont.BOLD -> Typeface(FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal, 0.sp)
    GreetingFont.BLACK -> Typeface(FontFamily.SansSerif, FontWeight.Black, FontStyle.Normal, (-0.5).sp)
    GreetingFont.LIGHT -> Typeface(FontFamily.SansSerif, FontWeight.Light, FontStyle.Normal, 0.sp)
    GreetingFont.THIN -> Typeface(FontFamily.SansSerif, FontWeight.Thin, FontStyle.Normal, 0.sp)
    GreetingFont.ITALIC -> Typeface(FontFamily.SansSerif, FontWeight.Normal, FontStyle.Italic, 0.sp)
    GreetingFont.BOLD_ITALIC -> Typeface(FontFamily.SansSerif, FontWeight.Bold, FontStyle.Italic, 0.sp)
    GreetingFont.LIGHT_ITALIC -> Typeface(FontFamily.SansSerif, FontWeight.Light, FontStyle.Italic, 0.sp)
    GreetingFont.WIDE -> Typeface(FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal, 4.sp)
    GreetingFont.TIGHT -> Typeface(FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal, (-2).sp)
    GreetingFont.CAPS_SPACED -> Typeface(FontFamily.SansSerif, FontWeight.Medium, FontStyle.Normal, 6.sp)
    GreetingFont.SERIF -> Typeface(FontFamily.Serif, FontWeight.Normal, FontStyle.Normal, 0.sp)
    GreetingFont.SERIF_BOLD -> Typeface(FontFamily.Serif, FontWeight.Bold, FontStyle.Normal, 0.sp)
    GreetingFont.SERIF_ITALIC -> Typeface(FontFamily.Serif, FontWeight.Normal, FontStyle.Italic, 0.sp)
    GreetingFont.SERIF_LIGHT -> Typeface(FontFamily.Serif, FontWeight.Light, FontStyle.Normal, 0.sp)
    GreetingFont.MONO -> Typeface(FontFamily.Monospace, FontWeight.Normal, FontStyle.Normal, 0.sp)
    GreetingFont.MONO_BOLD -> Typeface(FontFamily.Monospace, FontWeight.Bold, FontStyle.Normal, 0.sp)
    GreetingFont.MONO_ITALIC -> Typeface(FontFamily.Monospace, FontWeight.Normal, FontStyle.Italic, 0.sp)
    GreetingFont.CURSIVE -> Typeface(FontFamily.Cursive, FontWeight.Normal, FontStyle.Normal, 0.sp)
    GreetingFont.CURSIVE_BOLD -> Typeface(FontFamily.Cursive, FontWeight.Bold, FontStyle.Normal, 0.sp)
}

/** Label for the position chips in the editor. */
internal fun GreetingPosition.label(): String = when (this) {
    GreetingPosition.SWEEP -> "Sweep"
    GreetingPosition.LEFT -> "Left"
    GreetingPosition.CENTER -> "Center"
    GreetingPosition.RIGHT -> "Right"
}

/** One RGB color for [phase] in 0..1. Light mode is brighter, dark mode is richer. */
internal fun greetingRgb(phase: Float, isDark: Boolean): Color = Color.hsv(
    hue = (phase * 360f) % 360f,
    saturation = if (isDark) 0.6f else 0.85f,
    value = if (isDark) 1f else 0.75f,
)

/**
 * A large greeting for the current time of day. The text cycles through RGB and glows with its
 * own color. It sits left, center or right, or slides between them when [position] is SWEEP. A random greeting is picked for the time of
 * day and changes each time the screen resumes. Motion stops when animations are off.
 */
@Composable
internal fun TimeGreeting(
    font: GreetingFont,
    position: GreetingPosition,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    var hour by remember { mutableIntStateOf(currentHour()) }
    var greeting by remember { mutableStateOf(timeGreetingsFor(hour).random()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hour = currentHour()
                greeting = timeGreetingsFor(hour).random()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val transition = rememberInfiniteTransition(label = "timeGreetingFx")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RGB_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "timeGreetingRgb",
    )
    val animatedBias by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MOVE_HALF_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timeGreetingMove",
    )
    val isDark = MaterialTheme.colorScheme.surfaceBright.luminance() < 0.5f
    val phase = if (animationsEnabled) animatedPhase else 0f
    val bias = when (position) {
        GreetingPosition.SWEEP -> if (animationsEnabled) animatedBias else 0f
        GreetingPosition.LEFT -> -1f
        GreetingPosition.CENTER -> 0f
        GreetingPosition.RIGHT -> 1f
    }
    val color = greetingRgb(phase, isDark)
    val typeface = font.typeface()

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(BiasAlignment(horizontalBias = bias, verticalBias = 0f)),
            style = TextStyle(
                fontFamily = typeface.family,
                fontWeight = typeface.weight,
                fontStyle = typeface.style,
                letterSpacing = typeface.spacing,
                fontSize = GREETING_FONT_SIZE.sp,
                lineHeight = (GREETING_FONT_SIZE + 6).sp,
                shadow = Shadow(
                    color = color.copy(alpha = 0.6f),
                    offset = Offset.Zero,
                    blurRadius = 32f,
                ),
            ),
        )
    }
}

/**
 * An avatar with a smooth RGB ring around it. The ring is drawn in its own layer each frame,
 * so only the colour changes and the rest of the header is not recomposed. The ring follows
 * the avatar shape. It stays still when system animations are off.
 */
@Composable
internal fun RgbRingAvatar(
    shape: Shape,
    avatarSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val transition = rememberInfiniteTransition(label = "avatarRingFx")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RING_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avatarRingRgb",
    )
    val isDark = MaterialTheme.colorScheme.surfaceBright.luminance() < 0.5f

    Box(
        modifier = modifier
            .size(avatarSize + RING_WIDTH * 2)
            .drawBehind {
                // Read the animated phase here, in the draw phase, so frames do not recompose.
                val phase = if (animationsEnabled) animatedPhase else 0f
                val colors = listOf(
                    greetingRgb(phase, isDark),
                    greetingRgb((phase + 1f / 3f) % 1f, isDark),
                    greetingRgb((phase + 2f / 3f) % 1f, isDark),
                    greetingRgb(phase, isDark),
                )
                val outline = shape.createOutline(this.size, layoutDirection, this)
                drawPath(
                    path = Path().apply { addOutline(outline) },
                    brush = Brush.sweepGradient(colors, center),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
