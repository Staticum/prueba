package com.staticum.niagaralauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.util.SoundOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/** How the whole screen (Compose UI + native widget views) gets desaturated/tinted
 * via [android.view.View.setLayerType] in MainActivity. NONE leaves colors as-is,
 * GRAYSCALE is plain black & white, COLOR is a duotone tinted with the current
 * palette's accent color. */
enum class ScreenTintMode { NONE, GRAYSCALE, COLOR }

/** How each widget is framed on the home screen. Many widgets draw on a fully
 * transparent background and assume the launcher gives them contrast; over a photo
 * wallpaper (or even a light palette) they can become unreadable, so the user can
 * opt into a container behind them. */
enum class WidgetBackground { NONE, SUBTLE, SOLID }

data class LauncherPrefs(
    val paletteId: String = ColorPalette.MATTE_BLACK.id,
    val useWallpaper: Boolean = false,
    val wallpaperUri: String? = null,
    val iconSizeFactor: Float = 1.0f,
    val monochromeIcons: Boolean = false,
    val screenTintMode: ScreenTintMode = ScreenTintMode.NONE,
    val soundId: String = SoundOption.DEFAULT.id,
    val soundVolume: Float = 0.5f,
    val indexWaveOffsetDp: Float = 24f,
    val homeResetSeconds: Int = 10,
    val fontFamilyId: String = com.staticum.niagaralauncher.ui.theme.AppFonts.DEFAULT_ID,
    val customAccentArgb: Int? = null,
    val ambientLockEnabled: Boolean = false,
    val widgetBackground: WidgetBackground = WidgetBackground.NONE,
    val iconSilhouette: Boolean = false,
    val frequentsEnabled: Boolean = true,
    val frequentsCount: Int = 6,
    val useSystemUsageStats: Boolean = false,
    val hiddenApps: Set<String> = emptySet(),
    val gestureFavorites: Map<SwipeDirection, String> = emptyMap(),
    val favoriteAppKeys: List<String> = emptyList(),
) {
    val palette: ColorPalette get() {
        val base = ColorPalette.fromId(paletteId)
        return if (paletteId == ColorPalette.CUSTOM_ID && customAccentArgb != null) {
            base.copy(accent = Color(customAccentArgb))
        } else {
            base
        }
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val PALETTE_ID = stringPreferencesKey("palette_id")
        val USE_WALLPAPER = booleanPreferencesKey("use_wallpaper")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val ICON_SIZE = floatPreferencesKey("icon_size_factor")
        val MONOCHROME = booleanPreferencesKey("monochrome_icons")
        val SCREEN_TINT_MODE = stringPreferencesKey("screen_tint_mode")
        val SOUND_ID = stringPreferencesKey("sound_id")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val INDEX_WAVE_OFFSET = floatPreferencesKey("index_wave_offset")
        val HOME_RESET_SECONDS = intPreferencesKey("home_reset_seconds")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val CUSTOM_ACCENT = intPreferencesKey("custom_accent_argb")
        val AMBIENT_LOCK = booleanPreferencesKey("ambient_lock_enabled")
        val WIDGET_BACKGROUND = stringPreferencesKey("widget_background")
        val ICON_SILHOUETTE = booleanPreferencesKey("icon_silhouette")
        val FREQUENTS_ENABLED = booleanPreferencesKey("frequents_enabled")
        val FREQUENTS_COUNT = intPreferencesKey("frequents_count")
        val USE_SYSTEM_USAGE = booleanPreferencesKey("use_system_usage_stats")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val GESTURE_UP = stringPreferencesKey("gesture_up")
        val GESTURE_DOWN = stringPreferencesKey("gesture_down")
        val GESTURE_LEFT = stringPreferencesKey("gesture_left")
        val GESTURE_RIGHT = stringPreferencesKey("gesture_right")
        val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
    }

    val prefsFlow: Flow<LauncherPrefs> = context.dataStore.data.map { prefs ->
        val gestures = buildMap {
            prefs[Keys.GESTURE_UP]?.let { put(SwipeDirection.UP, it) }
            prefs[Keys.GESTURE_DOWN]?.let { put(SwipeDirection.DOWN, it) }
            prefs[Keys.GESTURE_LEFT]?.let { put(SwipeDirection.LEFT, it) }
            prefs[Keys.GESTURE_RIGHT]?.let { put(SwipeDirection.RIGHT, it) }
        }
        LauncherPrefs(
            paletteId = prefs[Keys.PALETTE_ID] ?: ColorPalette.MATTE_BLACK.id,
            useWallpaper = prefs[Keys.USE_WALLPAPER] ?: false,
            wallpaperUri = prefs[Keys.WALLPAPER_URI],
            iconSizeFactor = prefs[Keys.ICON_SIZE] ?: 1.0f,
            monochromeIcons = prefs[Keys.MONOCHROME] ?: false,
            screenTintMode = prefs[Keys.SCREEN_TINT_MODE]?.let { raw ->
                runCatching { ScreenTintMode.valueOf(raw) }.getOrNull()
            } ?: ScreenTintMode.NONE,
            soundId = prefs[Keys.SOUND_ID] ?: SoundOption.DEFAULT.id,
            soundVolume = prefs[Keys.SOUND_VOLUME] ?: 0.5f,
            indexWaveOffsetDp = prefs[Keys.INDEX_WAVE_OFFSET] ?: 24f,
            homeResetSeconds = (prefs[Keys.HOME_RESET_SECONDS] ?: 10).coerceIn(5, 60),
            fontFamilyId = prefs[Keys.FONT_FAMILY] ?: com.staticum.niagaralauncher.ui.theme.AppFonts.DEFAULT_ID,
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT],
            ambientLockEnabled = prefs[Keys.AMBIENT_LOCK] ?: false,
            widgetBackground = prefs[Keys.WIDGET_BACKGROUND]?.let { raw ->
                runCatching { WidgetBackground.valueOf(raw) }.getOrNull()
            } ?: WidgetBackground.NONE,
            iconSilhouette = prefs[Keys.ICON_SILHOUETTE] ?: false,
            frequentsEnabled = prefs[Keys.FREQUENTS_ENABLED] ?: true,
            frequentsCount = (prefs[Keys.FREQUENTS_COUNT] ?: 6).coerceIn(4, 10),
            useSystemUsageStats = prefs[Keys.USE_SYSTEM_USAGE] ?: false,
            hiddenApps = prefs[Keys.HIDDEN_APPS] ?: emptySet(),
            gestureFavorites = gestures,
            favoriteAppKeys = prefs[Keys.FAVORITE_APPS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    suspend fun setPalette(id: String) {
        context.dataStore.edit { it[Keys.PALETTE_ID] = id }
    }

    suspend fun setUseWallpaper(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_WALLPAPER] = enabled }
    }

    suspend fun setWallpaperUri(uri: String?) {
        context.dataStore.edit {
            if (uri == null) it.remove(Keys.WALLPAPER_URI) else it[Keys.WALLPAPER_URI] = uri
        }
    }

    suspend fun setIconSizeFactor(factor: Float) {
        context.dataStore.edit { it[Keys.ICON_SIZE] = factor }
    }

    suspend fun setMonochromeIcons(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONOCHROME] = enabled }
    }

    suspend fun setScreenTintMode(mode: ScreenTintMode) {
        context.dataStore.edit { it[Keys.SCREEN_TINT_MODE] = mode.name }
    }

    suspend fun setSoundId(id: String) {
        context.dataStore.edit { it[Keys.SOUND_ID] = id }
    }

    suspend fun setSoundVolume(volume: Float) {
        context.dataStore.edit { it[Keys.SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setIndexWaveOffset(offsetDp: Float) {
        context.dataStore.edit { it[Keys.INDEX_WAVE_OFFSET] = offsetDp.coerceIn(0f, 64f) }
    }

    suspend fun setHomeResetSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.HOME_RESET_SECONDS] = seconds.coerceIn(5, 60) }
    }

    suspend fun setFontFamily(id: String) {
        context.dataStore.edit { it[Keys.FONT_FAMILY] = id }
    }

    suspend fun setCustomAccentColor(argb: Int) {
        context.dataStore.edit {
            it[Keys.PALETTE_ID] = ColorPalette.CUSTOM_ID
            it[Keys.CUSTOM_ACCENT] = argb
        }
    }

    suspend fun setAmbientLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AMBIENT_LOCK] = enabled }
    }

    suspend fun setWidgetBackground(background: WidgetBackground) {
        context.dataStore.edit { it[Keys.WIDGET_BACKGROUND] = background.name }
    }

    suspend fun setIconSilhouette(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ICON_SILHOUETTE] = enabled }
    }

    suspend fun setFrequentsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FREQUENTS_ENABLED] = enabled }
    }

    suspend fun setFrequentsCount(count: Int) {
        context.dataStore.edit { it[Keys.FREQUENTS_COUNT] = count.coerceIn(4, 10) }
    }

    suspend fun setUseSystemUsageStats(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_SYSTEM_USAGE] = enabled }
    }

    suspend fun toggleHiddenApp(appKey: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_APPS] ?: emptySet()
            prefs[Keys.HIDDEN_APPS] = if (hidden) current + appKey else current - appKey
        }
    }

    suspend fun setGestureFavorite(direction: SwipeDirection, appKey: String?) {
        val key = when (direction) {
            SwipeDirection.UP -> Keys.GESTURE_UP
            SwipeDirection.DOWN -> Keys.GESTURE_DOWN
            SwipeDirection.LEFT -> Keys.GESTURE_LEFT
            SwipeDirection.RIGHT -> Keys.GESTURE_RIGHT
        }
        context.dataStore.edit { prefs ->
            if (appKey == null) prefs.remove(key) else prefs[key] = appKey
        }
    }

    suspend fun toggleFavoriteApp(appKey: String, favorite: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_APPS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = if (favorite) current + appKey else current - appKey
            prefs[Keys.FAVORITE_APPS] = updated.joinToString(",")
        }
    }

    suspend fun moveFavoriteApp(appKey: String, delta: Int) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.FAVORITE_APPS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
                .toMutableList()
            val index = current.indexOf(appKey)
            val target = index + delta
            if (index < 0 || target < 0 || target >= current.size) return@edit
            val tmp = current[index]
            current[index] = current[target]
            current[target] = tmp
            prefs[Keys.FAVORITE_APPS] = current.joinToString(",")
        }
    }
}
