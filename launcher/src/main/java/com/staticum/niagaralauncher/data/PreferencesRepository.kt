package com.staticum.niagaralauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

data class LauncherPrefs(
    val paletteId: String = ColorPalette.MATTE_BLACK.id,
    val useWallpaper: Boolean = false,
    val wallpaperUri: String? = null,
    val iconSizeFactor: Float = 1.0f,
    val monochromeIcons: Boolean = false,
    val hiddenApps: Set<String> = emptySet(),
    val gestureFavorites: Map<SwipeDirection, String> = emptyMap(),
    val favoriteAppKeys: List<String> = emptyList(),
) {
    val palette: ColorPalette get() = ColorPalette.fromId(paletteId)
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val PALETTE_ID = stringPreferencesKey("palette_id")
        val USE_WALLPAPER = booleanPreferencesKey("use_wallpaper")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val ICON_SIZE = floatPreferencesKey("icon_size_factor")
        val MONOCHROME = booleanPreferencesKey("monochrome_icons")
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
