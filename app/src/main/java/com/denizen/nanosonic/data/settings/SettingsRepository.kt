package com.denizen.nanosonic.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.core.model.ThemeMode
import com.denizen.nanosonic.di.AppScope
import com.denizen.nanosonic.di.SettingsPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gaplessMode: GaplessMode = GaplessMode.ALBUMS_ONLY,
    val appInitialized: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    @SettingsPrefs private val prefs: SharedPreferences,
    @AppScope scope: CoroutineScope,
) {
    /** Current settings. The initial value is read synchronously so the first frame has the right theme. */
    val settings: StateFlow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, read())

    fun setThemeMode(mode: ThemeMode) = prefs.edit { putString(KEY_THEME_MODE, mode.name) }

    fun setGaplessMode(mode: GaplessMode) = prefs.edit { putString(KEY_GAPLESS_MODE, mode.name) }

    fun setAppInitialized(initialized: Boolean) = prefs.edit { putBoolean(KEY_APP_INITIALIZED, initialized) }

    private fun read() = AppSettings(
        themeMode = ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, null)),
        gaplessMode = GaplessMode.fromStorage(prefs.getString(KEY_GAPLESS_MODE, null)),
        appInitialized = prefs.getBoolean(KEY_APP_INITIALIZED, false),
    )

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_GAPLESS_MODE = "gapless_mode"
        const val KEY_APP_INITIALIZED = "app_initialized"
    }
}
