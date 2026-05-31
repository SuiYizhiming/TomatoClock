package com.xuweikai.tomatoclock.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppSettingsKeys {
    val FOCUS_DURATION_MIN = intPreferencesKey("focusDurationMin")
    val SHORT_BREAK_DURATION_MIN = intPreferencesKey("shortBreakDurationMin")
    val LONG_BREAK_DURATION_MIN = intPreferencesKey("longBreakDurationMin")
    val LONG_BREAK_INTERVAL = intPreferencesKey("longBreakInterval")
    val ALERT_SOUND = stringPreferencesKey("alertSound")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
}
