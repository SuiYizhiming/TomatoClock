package com.xuweikai.tomatoclock.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.xuweikai.tomatoclock.core.datastore.AppSettingsKeys
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.settings.SettingsValidator
import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.core.model.DarkMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map(::toAppSettings)
    }

    override suspend fun getSettings(): AppSettings = observeSettings().first()

    override suspend fun saveSettings(settings: AppSettings) {
        SettingsValidator.requireValid(settings)
        dataStore.edit { preferences ->
            preferences[AppSettingsKeys.FOCUS_DURATION_MIN] = settings.focusDurationMin
            preferences[AppSettingsKeys.SHORT_BREAK_DURATION_MIN] = settings.shortBreakDurationMin
            preferences[AppSettingsKeys.LONG_BREAK_DURATION_MIN] = settings.longBreakDurationMin
            preferences[AppSettingsKeys.LONG_BREAK_INTERVAL] = settings.longBreakInterval
            preferences[AppSettingsKeys.ALERT_SOUND] = settings.alertSound
            preferences[AppSettingsKeys.VIBRATION_ENABLED] = settings.vibrationEnabled
            preferences[AppSettingsKeys.DARK_MODE] = settings.darkMode.name
        }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        return SettingsValidator.sanitize(
            AppSettings(
                focusDurationMin = preferences[AppSettingsKeys.FOCUS_DURATION_MIN]
                    ?: defaults.focusDurationMin,
                shortBreakDurationMin = preferences[AppSettingsKeys.SHORT_BREAK_DURATION_MIN]
                    ?: defaults.shortBreakDurationMin,
                longBreakDurationMin = preferences[AppSettingsKeys.LONG_BREAK_DURATION_MIN]
                    ?: defaults.longBreakDurationMin,
                longBreakInterval = preferences[AppSettingsKeys.LONG_BREAK_INTERVAL]
                    ?: defaults.longBreakInterval,
                alertSound = preferences[AppSettingsKeys.ALERT_SOUND] ?: defaults.alertSound,
                vibrationEnabled = preferences[AppSettingsKeys.VIBRATION_ENABLED]
                    ?: defaults.vibrationEnabled,
                darkMode = preferences[AppSettingsKeys.DARK_MODE]?.let {
                    runCatching { DarkMode.valueOf(it) }.getOrNull()
                } ?: defaults.darkMode,
            ),
        )
    }
}
