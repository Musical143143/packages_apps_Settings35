/*
 * Copyright (C) 2024-2026 Lunaris AOSP
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

import android.content.Context
import android.util.Log
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "HomepageLayout"

const val SEPARATOR_PREFIX = "__sep__"

fun isSeparatorKey(key: String): Boolean = key.startsWith(SEPARATOR_PREFIX)

const val SEARCH_BAR_KEY = "__searchbar__"

fun isSearchBarKey(key: String): Boolean = key == SEARCH_BAR_KEY

enum class TileWidth { HALF, FULL }

enum class TileHeight { SHORT, TALL }

enum class TileSize(val width: TileWidth, val height: TileHeight) {
    SMALL(TileWidth.HALF, TileHeight.SHORT),
    MEDIUM(TileWidth.HALF, TileHeight.TALL),
    ROW(TileWidth.FULL, TileHeight.SHORT),
    LARGE(TileWidth.FULL, TileHeight.TALL);

    val span: Int get() = if (width == TileWidth.FULL) 2 else 1

    fun withWidth(target: TileWidth): TileSize = of(target, height)

    fun withHeight(target: TileHeight): TileSize = of(width, target)

    fun next(): TileSize = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> ROW
        ROW -> LARGE
        LARGE -> SMALL
    }

    companion object {
        fun of(width: TileWidth, height: TileHeight): TileSize =
            values().first { it.width == width && it.height == height }
    }
}

enum class SearchBarPosition { TOP, BOTTOM, INLINE, HIDDEN }
enum class IconStyle { ACCENT, PLAIN, FILLED }
enum class IconShape {
    CIRCLE, ARROW, PILL, TRIANGLE, GEM, COOKIE_4, COOKIE_6, CLOVER_4,
    SQUARE, DIAMOND, PENTAGON, SUNNY, COOKIE_7, COOKIE_9, COOKIE_12, CLOVER_8, BURST, FLOWER,
    // Avatar-only shapes: offered in the avatar picker, not on tiles.
    OVAL, ARCH, SLANTED, FAN, SEMI_CIRCLE, PUFFY, PUFFY_DIAMOND, BOOM, SOFT_BURST, GHOSTISH,
}

private val AVATAR_ONLY_ICON_SHAPES: Set<IconShape> = setOf(
    IconShape.OVAL, IconShape.ARCH, IconShape.SLANTED, IconShape.FAN, IconShape.SEMI_CIRCLE,
    IconShape.PUFFY, IconShape.PUFFY_DIAMOND, IconShape.BOOM, IconShape.SOFT_BURST,
    IconShape.GHOSTISH,
)

/** Shapes offered when picking a tile, page or item icon shape. */
val TILE_ICON_SHAPES: List<IconShape> = IconShape.values().filterNot { it in AVATAR_ONLY_ICON_SHAPES }

/** Shapes offered when picking the avatar shape: every shape, including avatar-only ones. */
val AVATAR_ICON_SHAPES: List<IconShape> = IconShape.values().toList()
enum class IconPosition { START, END }
enum class ChevronStyle {
    PLAIN,
    CONTAINED,
    HIDDEN,
    SQUARE,
    PILL,
    FILLED,
    TONAL,
    OUTLINED,
    ACCENT,
    DOUBLE,
    DOWN,
    UP,
    LARGE,
    SHADOW,
    DIAGONAL,
    TILT,
    SECONDARY,
    TERTIARY,
    MUTED,
    GHOST,
    SQUARE_OUTLINED,
    PILL_FILLED,
    HALO,
}

enum class CornerStyle(
    val radius: Dp,
    private val factory: (Dp) -> Shape = { RoundedCornerShape(it) },
) {
    SOFT(16.dp),
    ROUNDED(24.dp),
    PILL(32.dp),
    SQUARE(0.dp),
    HAIRLINE(2.dp),
    TINY(4.dp),
    SUBTLE(6.dp),
    SMALL(8.dp),
    COMPACT(10.dp),
    MEDIUM(12.dp),
    COMFORTABLE(14.dp),
    RELAXED(18.dp),
    LARGE(20.dp),
    GENEROUS(22.dp),
    SOFT_LARGE(26.dp),
    EXTRA_SOFT(28.dp),
    BUBBLE(36.dp),
    SUPER(40.dp),
    ROUNDER(44.dp),
    ROUND(48.dp),
    HUGE(56.dp),
    BLOB(64.dp),
    MAX(72.dp),

    // Premium corners: asymmetric, cut and beveled outlines.
    CUT(24.dp, { CutCornerShape(it) }),
    BEVEL(12.dp, { CutCornerShape(it) }),
    LEAF(28.dp, { RoundedCornerShape(topStart = it, topEnd = 4.dp, bottomEnd = it, bottomStart = 4.dp) }),
    LEAF_REVERSE(28.dp, { RoundedCornerShape(topStart = 4.dp, topEnd = it, bottomEnd = 4.dp, bottomStart = it) }),
    TOP_ROUND(28.dp, { RoundedCornerShape(topStart = it, topEnd = it, bottomEnd = 4.dp, bottomStart = 4.dp) }),
    BOTTOM_ROUND(28.dp, { RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = it, bottomStart = it) }),
    SPLIT(28.dp, { RoundedCornerShape(topStart = it, topEnd = 0.dp, bottomEnd = it, bottomStart = 0.dp) });

    /** The outline a tile or panel uses for this corner style. */
    val shape: Shape
        get() = factory(radius)
}

enum class DividerStyle {
    NONE,
    GAP,
    INSET,
    FULL,
    THICK,
    SPLIT,
    ACCENT,
    DASHED,
    DOUBLE,
    FADE,
    DOTTED,
    DASH_DOT,
    WAVY,
    ZIGZAG,
    GRADIENT,
    SHORT,
    TRIPLE,
    ROUNDED,
    SHADOW,
    RAINBOW,
}

/**
 * Press feedback for a tile. [ripple] shows the Material ripple, [glow] a soft primary glow,
 * and [squish] a small press-down scale.
 */
enum class TapAnimation(val ripple: Boolean, val glow: Boolean, val squish: Boolean) {
    RIPPLE(ripple = true, glow = false, squish = false),
    RIPPLE_GLOW(ripple = true, glow = true, squish = false),
    GLOW(ripple = false, glow = true, squish = false),
    SQUISH(ripple = true, glow = false, squish = true),
    PULSE(ripple = true, glow = true, squish = true),
}

enum class HeaderSize { LARGE, COMPACT }

/** Typeface preset for the large time-based greeting in the header. */
enum class GreetingFont {
    CLASSIC, BOLD, BLACK, LIGHT, THIN, ITALIC, BOLD_ITALIC, LIGHT_ITALIC, WIDE, TIGHT, CAPS_SPACED,
    SERIF, SERIF_BOLD, SERIF_ITALIC, SERIF_LIGHT, MONO, MONO_BOLD, MONO_ITALIC, CURSIVE, CURSIVE_BOLD,
}

/** How large the time-based greeting is drawn. */
enum class GreetingSize { HUGE, COMPACT }

/** Where the time-based greeting sits: a fixed side, or sweeping left to right. */
enum class GreetingPosition { SWEEP, LEFT, CENTER, RIGHT }

data class HeaderConfig(
    val showTitle: Boolean = true,
    val customTitle: String? = null,
    val showAvatar: Boolean = true,
    val showGreeting: Boolean = false,
    val size: HeaderSize = HeaderSize.LARGE,
    val avatarShape: IconShape = IconShape.COOKIE_4,
    val showTimeGreeting: Boolean = false,
    val greetingFont: GreetingFont = GreetingFont.CLASSIC,
    val greetingPosition: GreetingPosition = GreetingPosition.SWEEP,
    val greetingSize: GreetingSize = GreetingSize.HUGE,
)

data class HomepageLayout(
    val order: List<String>,
    val sizes: Map<String, TileSize> = emptyMap(),
    val titles: Map<String, String> = emptyMap(),
    val summaries: Map<String, String> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val themedIcons: Set<String> = emptySet(),
    val wallpaperCards: Set<String> = emptySet(),
    val iconColor: Int? = null,
    val iconColors: Map<String, Int> = emptyMap(),
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconShapes: Map<String, IconShape> = emptyMap(),
    val iconPosition: IconPosition = IconPosition.START,
    val iconPositions: Map<String, IconPosition> = emptyMap(),
    val header: HeaderConfig = HeaderConfig(),
    val searchBar: SearchBarPosition = SearchBarPosition.BOTTOM,
    val searchBarBreak: Boolean = false,
    val bottomBarGlass: Boolean = false,
    val showSummaries: Boolean = true,
    val iconStyle: IconStyle = IconStyle.ACCENT,
    val chevronStyle: ChevronStyle = ChevronStyle.PLAIN,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val dividerStyle: DividerStyle = DividerStyle.INSET,
    val tapAnimation: TapAnimation = TapAnimation.RIPPLE,
) {

    fun sizeOf(key: String, default: TileSize): TileSize = sizes[key] ?: default

    fun titleOf(key: String, default: String): String =
        titles[key]?.takeIf { it.isNotBlank() } ?: default

    fun summaryOf(key: String, default: String?): String? =
        if (key in summaries) summaries[key]?.takeIf { it.isNotEmpty() } else default

    fun titleDraft(key: String, default: String): String = titles[key] ?: default

    fun summaryDraft(key: String, default: String?): String = summaries[key] ?: default.orEmpty()

    fun hasLabelOverride(key: String): Boolean = key in titles || key in summaries

    fun isHidden(key: String): Boolean = key in hidden

    fun isThemedIcon(key: String): Boolean = key in themedIcons

    fun usesWallpaper(key: String): Boolean = key in wallpaperCards

    fun iconColorOf(key: String): Int? = iconColors[key] ?: iconColor

    fun hasIconColorOverride(key: String): Boolean = key in iconColors

    fun iconShapeOf(key: String): IconShape = iconShapes[key] ?: iconShape

    fun hasIconShapeOverride(key: String): Boolean = key in iconShapes

    fun iconPositionOf(key: String): IconPosition = iconPositions[key] ?: iconPosition

    fun hasIconPositionOverride(key: String): Boolean = key in iconPositions

    fun move(fromKey: String, toKey: String): HomepageLayout {
        if (fromKey == toKey) return this
        val from = order.indexOf(fromKey)
        val to = order.indexOf(toKey)
        if (from < 0 || to < 0) return this
        val next = order.toMutableList()
        next.removeAt(from)
        val target = next.indexOf(toKey)
        if (target < 0) return this
        val insertAt = if (from < to) target + 1 else target
        next.add(insertAt.coerceIn(0, next.size), fromKey)
        return copy(order = next)
    }

    fun moveToTop(key: String): HomepageLayout {
        if (order.firstOrNull() == key || key !in order) return this
        return copy(order = listOf(key) + order.filterNot { it == key })
    }

    fun setHidden(key: String, hide: Boolean): HomepageLayout =
        copy(hidden = if (hide) hidden + key else hidden - key)

    fun setThemedIcon(key: String, themed: Boolean): HomepageLayout =
        copy(themedIcons = if (themed) themedIcons + key else themedIcons - key)

    fun setWallpaperCard(key: String, wallpaper: Boolean): HomepageLayout =
        copy(wallpaperCards = if (wallpaper) wallpaperCards + key else wallpaperCards - key)

    fun withIconColor(argb: Int): HomepageLayout = copy(iconColor = argb.opaque())

    fun withIconColor(key: String, argb: Int): HomepageLayout =
        copy(iconColors = iconColors + (key to argb.opaque()))

    fun resetIconColor(): HomepageLayout = if (iconColor != null) copy(iconColor = null) else this

    /**
     * Applies the One UI style: large soft corners, separated cards, filled icons, a top search
     * bar and a subtle press squish. Only these visual fields change. Items, their order and
     * hidden tiles are kept.
     */
    fun withOneUiStyle(): HomepageLayout = copy(
        cornerStyle = CornerStyle.SOFT_LARGE,
        dividerStyle = DividerStyle.SPLIT,
        iconStyle = IconStyle.FILLED,
        searchBar = SearchBarPosition.TOP,
        showSummaries = true,
        chevronStyle = ChevronStyle.PLAIN,
        tapAnimation = TapAnimation.SQUISH,
    )

    fun resetIconColor(key: String): HomepageLayout =
        if (key in iconColors) copy(iconColors = iconColors - key) else this

    fun resetEntryIconColors(): HomepageLayout =
        if (iconColors.isEmpty()) this else copy(iconColors = emptyMap())

    fun withIconShape(shape: IconShape): HomepageLayout = copy(iconShape = shape)

    fun withIconShape(key: String, shape: IconShape): HomepageLayout =
        copy(iconShapes = iconShapes + (key to shape))

    fun resetIconShape(): HomepageLayout =
        if (iconShape != IconShape.CIRCLE) copy(iconShape = IconShape.CIRCLE) else this

    fun resetIconShape(key: String): HomepageLayout =
        if (key in iconShapes) copy(iconShapes = iconShapes - key) else this

    fun resetEntryIconShapes(): HomepageLayout =
        if (iconShapes.isEmpty()) this else copy(iconShapes = emptyMap())

    fun withIconPosition(position: IconPosition): HomepageLayout = copy(iconPosition = position)

    fun withIconPosition(key: String, position: IconPosition): HomepageLayout =
        copy(iconPositions = iconPositions + (key to position))

    fun resetIconPosition(): HomepageLayout =
        if (iconPosition != IconPosition.START) copy(iconPosition = IconPosition.START) else this

    fun resetIconPosition(key: String): HomepageLayout =
        if (key in iconPositions) copy(iconPositions = iconPositions - key) else this

    fun resetEntryIconPositions(): HomepageLayout =
        if (iconPositions.isEmpty()) this else copy(iconPositions = emptyMap())

    fun withSize(key: String, size: TileSize): HomepageLayout =
        copy(sizes = sizes + (key to size))

    fun withTitle(key: String, title: String): HomepageLayout =
        copy(titles = titles + (key to title))

    fun withSummary(key: String, summary: String): HomepageLayout =
        copy(summaries = summaries + (key to summary))

    fun resetLabel(key: String): HomepageLayout =
        if (hasLabelOverride(key)) copy(titles = titles - key, summaries = summaries - key) else this

    fun resetSize(key: String): HomepageLayout =
        if (key in sizes) copy(sizes = sizes - key) else this

    fun visibleOrder(): List<String> = order.filterNot { it in hidden }

    fun hiddenOrder(): List<String> = order.filter { it in hidden }

    fun insertSeparatorAfter(key: String): HomepageLayout {
        val index = order.indexOf(key)
        if (index < 0) return this
        val next = order.toMutableList()
        next.add(index + 1, nextSeparatorKey())
        return copy(order = next)
    }

    fun appendSeparator(): HomepageLayout = copy(order = order + nextSeparatorKey())

    fun removeEntry(key: String): HomepageLayout = copy(
        order = order.filterNot { it == key },
        sizes = sizes - key,
        titles = titles - key,
        summaries = summaries - key,
        hidden = hidden - key,
        themedIcons = themedIcons - key,
        wallpaperCards = wallpaperCards - key,
        iconColors = iconColors - key,
        iconShapes = iconShapes - key,
        iconPositions = iconPositions - key,
    )

    fun mergedWith(
        defaultOrder: List<String>,
        defaultHidden: Set<String> = emptySet(),
    ): HomepageLayout {
        if (defaultOrder.isEmpty()) return this
        val merged = order.toMutableList()
        val present = merged.toMutableSet()
        val hide = mutableSetOf<String>()
        var anchor: String? = null
        for (key in defaultOrder) {
            if (key in present) {
                anchor = key
                continue
            }
            if (isSeparatorKey(key)) continue
            val anchorIndex = anchor?.let { merged.indexOf(it) } ?: -1
            merged.add(if (anchorIndex >= 0) anchorIndex + 1 else 0, key)
            present.add(key)
            if (key in defaultHidden) hide.add(key)
            anchor = key
        }
        return if (merged.size == order.size) {
            this
        } else {
            copy(order = merged, hidden = if (hide.isEmpty()) hidden else hidden + hide)
        }
    }

    private fun nextSeparatorKey(): String {
        val highest = order.asSequence()
            .filter { isSeparatorKey(it) }
            .mapNotNull { it.removePrefix(SEPARATOR_PREFIX).toIntOrNull() }
            .maxOrNull() ?: -1
        return "$SEPARATOR_PREFIX${highest + 1}"
    }
}

internal fun Int.opaque(): Int = this or 0xFF000000.toInt()

private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

private inline fun <reified T : Enum<T>> JSONObject?.toEnumMap(): Map<String, T> {
    if (this == null) return emptyMap()
    val out = HashMap<String, T>(length())
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = enumValues<T>().firstOrNull { it.name == optString(key) }
        if (value != null) out[key] = value
    }
    return out
}

internal object HomepageLayoutJson {

    private const val VERSION = 1

    private const val KEY_VERSION = "version"
    private const val KEY_ORDER = "order"
    private const val KEY_SIZES = "sizes"
    private const val KEY_TITLES = "titles"
    private const val KEY_SUMMARIES = "summaries"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_THEMED_ICONS = "themedIcons"
    private const val KEY_WALLPAPER_CARDS = "wallpaperCards"
    private const val KEY_HEADER = "header"
    private const val KEY_SEARCH_BAR = "searchBar"
    private const val KEY_SEARCH_BAR_BREAK = "searchBarBreak"
    private const val KEY_BOTTOM_BAR_GLASS = "bottomBarGlass"
    private const val KEY_SHOW_SUMMARIES = "showSummaries"
    private const val KEY_ICON_STYLE = "iconStyle"
    private const val KEY_ICON_COLOR = "iconColor"
    private const val KEY_ICON_COLORS = "iconColors"
    private const val KEY_ICON_SHAPE = "iconShape"
    private const val KEY_ICON_SHAPES = "iconShapes"
    private const val KEY_ICON_POSITION = "iconPosition"
    private const val KEY_ICON_POSITIONS = "iconPositions"
    private const val KEY_CHEVRON_STYLE = "chevronStyle"
    private const val KEY_CORNER_STYLE = "cornerStyle"
    private const val KEY_DIVIDER_STYLE = "dividerStyle"
    private const val KEY_TAP_ANIMATION = "tapAnimation"

    private const val KEY_SHOW_TITLE = "showTitle"
    private const val KEY_CUSTOM_TITLE = "customTitle"
    private const val KEY_SHOW_AVATAR = "showAvatar"
    private const val KEY_SHOW_GREETING = "showGreeting"
    private const val KEY_HEADER_SIZE = "size"
    private const val KEY_AVATAR_SHAPE = "avatarShape"
    private const val KEY_SHOW_TIME_GREETING = "showTimeGreeting"
    private const val KEY_GREETING_FONT = "greetingFont"
    private const val KEY_GREETING_POSITION = "greetingPosition"
    private const val KEY_GREETING_SIZE = "greetingSize"

    fun encode(layout: HomepageLayout): String = JSONObject().apply {
        put(KEY_VERSION, VERSION)
        put(KEY_ORDER, JSONArray(layout.order))
        put(KEY_SIZES, JSONObject().apply {
            layout.sizes.forEach { (key, size) -> put(key, size.name) }
        })
        put(KEY_TITLES, JSONObject().apply {
            layout.titles.forEach { (key, title) -> put(key, title) }
        })
        put(KEY_SUMMARIES, JSONObject().apply {
            layout.summaries.forEach { (key, summary) -> put(key, summary) }
        })
        put(KEY_HIDDEN, JSONArray(layout.hidden.toList()))
        put(KEY_THEMED_ICONS, JSONArray(layout.themedIcons.toList()))
        put(KEY_WALLPAPER_CARDS, JSONArray(layout.wallpaperCards.toList()))
        put(KEY_HEADER, JSONObject().apply {
            put(KEY_SHOW_TITLE, layout.header.showTitle)
            layout.header.customTitle?.let { put(KEY_CUSTOM_TITLE, it) }
            put(KEY_SHOW_AVATAR, layout.header.showAvatar)
            put(KEY_SHOW_GREETING, layout.header.showGreeting)
            put(KEY_HEADER_SIZE, layout.header.size.name)
            put(KEY_AVATAR_SHAPE, layout.header.avatarShape.name)
            put(KEY_SHOW_TIME_GREETING, layout.header.showTimeGreeting)
            put(KEY_GREETING_FONT, layout.header.greetingFont.name)
            put(KEY_GREETING_POSITION, layout.header.greetingPosition.name)
            put(KEY_GREETING_SIZE, layout.header.greetingSize.name)
        })
        put(KEY_SEARCH_BAR, layout.searchBar.name)
        put(KEY_SEARCH_BAR_BREAK, layout.searchBarBreak)
        put(KEY_BOTTOM_BAR_GLASS, layout.bottomBarGlass)
        put(KEY_SHOW_SUMMARIES, layout.showSummaries)
        put(KEY_ICON_STYLE, layout.iconStyle.name)
        layout.iconColor?.let { put(KEY_ICON_COLOR, it) }
        put(KEY_ICON_COLORS, JSONObject().apply {
            layout.iconColors.forEach { (key, argb) -> put(key, argb) }
        })
        put(KEY_ICON_SHAPE, layout.iconShape.name)
        put(KEY_ICON_SHAPES, JSONObject().apply {
            layout.iconShapes.forEach { (key, shape) -> put(key, shape.name) }
        })
        put(KEY_ICON_POSITION, layout.iconPosition.name)
        put(KEY_ICON_POSITIONS, JSONObject().apply {
            layout.iconPositions.forEach { (key, position) -> put(key, position.name) }
        })
        put(KEY_CHEVRON_STYLE, layout.chevronStyle.name)
        put(KEY_CORNER_STYLE, layout.cornerStyle.name)
        put(KEY_DIVIDER_STYLE, layout.dividerStyle.name)
        put(KEY_TAP_ANIMATION, layout.tapAnimation.name)
    }.toString()

    fun decode(raw: String?, fallback: HomepageLayout): HomepageLayout {
        if (raw.isNullOrEmpty()) return fallback
        return try {
            val json = JSONObject(raw)
            HomepageLayout(
                order = json.optJSONArray(KEY_ORDER).toStringList().ifEmpty { fallback.order },
                sizes = json.optJSONObject(KEY_SIZES).toEnumMap<TileSize>(),
                titles = json.optJSONObject(KEY_TITLES).toStringMap(),
                summaries = json.optJSONObject(KEY_SUMMARIES).toStringMap(),
                hidden = json.optJSONArray(KEY_HIDDEN).toStringList().toSet(),
                themedIcons = json.optJSONArray(KEY_THEMED_ICONS).toStringList().toSet(),
                wallpaperCards = json.optJSONArray(KEY_WALLPAPER_CARDS).toStringList().toSet(),
                header = json.optJSONObject(KEY_HEADER).toHeaderConfig(fallback.header),
                searchBar = enumOr(json.optString(KEY_SEARCH_BAR), fallback.searchBar),
                searchBarBreak = json.optBoolean(KEY_SEARCH_BAR_BREAK, fallback.searchBarBreak),
                bottomBarGlass = json.optBoolean(KEY_BOTTOM_BAR_GLASS, fallback.bottomBarGlass),
                showSummaries = json.optBoolean(KEY_SHOW_SUMMARIES, fallback.showSummaries),
                iconStyle = enumOr(json.optString(KEY_ICON_STYLE), fallback.iconStyle),
                iconColor = json.optArgb(KEY_ICON_COLOR, fallback.iconColor),
                iconColors = json.optJSONObject(KEY_ICON_COLORS).toArgbMap(),
                iconShape = enumOr(json.optString(KEY_ICON_SHAPE), fallback.iconShape),
                iconShapes = json.optJSONObject(KEY_ICON_SHAPES).toEnumMap<IconShape>(),
                iconPosition = enumOr(json.optString(KEY_ICON_POSITION), fallback.iconPosition),
                iconPositions = json.optJSONObject(KEY_ICON_POSITIONS).toEnumMap<IconPosition>(),
                chevronStyle = enumOr(json.optString(KEY_CHEVRON_STYLE), fallback.chevronStyle),
                cornerStyle = enumOr(json.optString(KEY_CORNER_STYLE), fallback.cornerStyle),
                dividerStyle = enumOr(json.optString(KEY_DIVIDER_STYLE), fallback.dividerStyle),
                tapAnimation = enumOr(json.optString(KEY_TAP_ANIMATION), fallback.tapAnimation),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Discarding unreadable homepage layout", e)
            fallback
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            val value = optString(i)
            if (!value.isNullOrEmpty()) out.add(value)
        }
        return out
    }

    private fun JSONObject?.toSizeMap(): Map<String, TileSize> = toEnumMap()

    private fun JSONObject.optArgb(name: String, fallback: Int?): Int? =
        (opt(name) as? Number)?.toInt()?.opaque() ?: fallback

    private fun JSONObject?.toArgbMap(): Map<String, Int> {
        if (this == null) return emptyMap()
        val out = HashMap<String, Int>(length())
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key) as? Number ?: continue
            out[key] = value.toInt().opaque()
        }
        return out
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        val out = HashMap<String, String>(length())
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!isNull(key)) out[key] = optString(key)
        }
        return out
    }

    private fun JSONObject?.toHeaderConfig(fallback: HeaderConfig): HeaderConfig {
        if (this == null) return fallback
        val title = if (isNull(KEY_CUSTOM_TITLE)) {
            null
        } else {
            optString(KEY_CUSTOM_TITLE).takeIf { it.isNotBlank() }
        }
        return HeaderConfig(
            showTitle = optBoolean(KEY_SHOW_TITLE, fallback.showTitle),
            customTitle = title,
            showAvatar = optBoolean(KEY_SHOW_AVATAR, fallback.showAvatar),
            showGreeting = optBoolean(KEY_SHOW_GREETING, fallback.showGreeting),
            size = enumOr(optString(KEY_HEADER_SIZE), fallback.size),
            avatarShape = enumOr(optString(KEY_AVATAR_SHAPE), fallback.avatarShape),
            showTimeGreeting = optBoolean(KEY_SHOW_TIME_GREETING, fallback.showTimeGreeting),
            greetingFont = enumOr(optString(KEY_GREETING_FONT), fallback.greetingFont),
            greetingPosition = enumOr(optString(KEY_GREETING_POSITION), fallback.greetingPosition),
            greetingSize = enumOr(optString(KEY_GREETING_SIZE), fallback.greetingSize),
        )
    }
}

class HomepageLayoutStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(default: HomepageLayout): HomepageLayout {
        val raw = try {
            prefs.getString(KEY_LAYOUT, null)
        } catch (e: ClassCastException) {
            Log.w(TAG, "Unexpected type stored for homepage layout", e)
            null
        }
        return HomepageLayoutJson.decode(raw, default).mergedWith(default.order, default.hidden)
    }

    fun save(layout: HomepageLayout) {
        prefs.edit().putString(KEY_LAYOUT, HomepageLayoutJson.encode(layout)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_LAYOUT).apply()
    }

    private companion object {
        const val PREFS_NAME = "settings_homepage_layout"
        const val KEY_LAYOUT = "layout"
    }
}
