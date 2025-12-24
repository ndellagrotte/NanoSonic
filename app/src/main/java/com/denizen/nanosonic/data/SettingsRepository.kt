package com.denizen.nanosonic.data

import android.content.Context
import android.content.SharedPreferences
import com.denizen.nanosonic.ui.screens.settings.GaplessMode
import com.denizen.nanosonic.ui.screens.settings.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

private const val PREFS_NAME = "settings_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_GAPLESS_MODE = "gapless_mode"
private const val KEY_APP_INITIALIZED = "app_initialized"

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val themeMode: Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) {
                trySend(getThemeMode())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getThemeMode()) // Send initial value
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    val gaplessMode: Flow<GaplessMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GAPLESS_MODE) {
                trySend(getGaplessMode())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getGaplessMode()) // Send initial value
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun getThemeMode(): ThemeMode {
        val themeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun getGaplessMode(): GaplessMode {
        val modeName = prefs.getString(KEY_GAPLESS_MODE, GaplessMode.ALBUMS_ONLY.name) ?: GaplessMode.ALBUMS_ONLY.name
        return try {
            GaplessMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            GaplessMode.ALBUMS_ONLY
        }
    }

    fun setGaplessMode(mode: GaplessMode) {
        prefs.edit { putString(KEY_GAPLESS_MODE, mode.name) }
    }

    /**
     * Check if the app has been initialized (user has completed initial setup)
     */
    fun isAppInitialized(): Boolean {
        return prefs.getBoolean(KEY_APP_INITIALIZED, false)
    }

    /**
     * Mark the app as initialized (called after wizard completion)
     */
    fun setAppInitialized(initialized: Boolean) {
        prefs.edit { putBoolean(KEY_APP_INITIALIZED, initialized) }
    }
}
