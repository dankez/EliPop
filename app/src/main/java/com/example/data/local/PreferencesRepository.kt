package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LiveEffectType
import com.example.data.model.RotationMode
import com.example.data.model.RotationPool
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pastelka_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val rotationMode: RotationMode = RotationMode.STATIC,
    val rotationTarget: WallpaperTarget = WallpaperTarget.BOTH,
    val rotationPool: RotationPool = RotationPool.ALL_SAVED,
    val rotationCategory: WallpaperCategory = WallpaperCategory.PETS,
    val liveEffectType: LiveEffectType = LiveEffectType.PETALS,
    val currentWallpaperId: String? = null,
    val lastRotatedAt: Long = 0L,
    val totalRotationsCount: Int = 0,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ROTATION_MODE = stringPreferencesKey("rotation_mode")
        val ROTATION_TARGET = stringPreferencesKey("rotation_target")
        val ROTATION_POOL = stringPreferencesKey("rotation_pool")
        val ROTATION_CATEGORY = stringPreferencesKey("rotation_category")
        val LIVE_EFFECT_TYPE = stringPreferencesKey("live_effect_type")
        val CURRENT_WALLPAPER_ID = stringPreferencesKey("current_wallpaper_id")
        val LAST_ROTATED_AT = longPreferencesKey("last_rotated_at")
        val TOTAL_ROTATIONS_COUNT = intPreferencesKey("total_rotations_count")
    }

    val preferencesFlow: Flow<UserPreferences> =
        context.dataStore.data.map { prefs ->
            UserPreferences(
                themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
                rotationMode = RotationMode.fromName(prefs[Keys.ROTATION_MODE]),
                rotationTarget = WallpaperTarget.fromName(prefs[Keys.ROTATION_TARGET]),
                rotationPool = RotationPool.fromName(prefs[Keys.ROTATION_POOL]),
                rotationCategory =
                    WallpaperCategory.fromName(prefs[Keys.ROTATION_CATEGORY]).let {
                        if (it == WallpaperCategory.ALL) WallpaperCategory.PETS else it
                    },
                liveEffectType = LiveEffectType.fromName(prefs[Keys.LIVE_EFFECT_TYPE]),
                currentWallpaperId = prefs[Keys.CURRENT_WALLPAPER_ID],
                lastRotatedAt = prefs[Keys.LAST_ROTATED_AT] ?: 0L,
                totalRotationsCount = prefs[Keys.TOTAL_ROTATIONS_COUNT] ?: 0,
            )
        }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setRotationMode(mode: RotationMode) {
        context.dataStore.edit { it[Keys.ROTATION_MODE] = mode.name }
    }

    suspend fun setRotationTarget(target: WallpaperTarget) {
        context.dataStore.edit { it[Keys.ROTATION_TARGET] = target.name }
    }

    suspend fun setRotationPool(pool: RotationPool) {
        context.dataStore.edit { it[Keys.ROTATION_POOL] = pool.name }
    }

    suspend fun setRotationCategory(category: WallpaperCategory) {
        context.dataStore.edit { it[Keys.ROTATION_CATEGORY] = category.name }
    }

    suspend fun setLiveEffectType(effect: LiveEffectType) {
        context.dataStore.edit { it[Keys.LIVE_EFFECT_TYPE] = effect.name }
    }

    suspend fun recordWallpaperApplied(wallpaperId: String, incrementRotationCount: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_WALLPAPER_ID] = wallpaperId
            prefs[Keys.LAST_ROTATED_AT] = System.currentTimeMillis()
            if (incrementRotationCount) {
                prefs[Keys.TOTAL_ROTATIONS_COUNT] = (prefs[Keys.TOTAL_ROTATIONS_COUNT] ?: 0) + 1
            }
        }
    }
}
