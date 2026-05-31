package com.xuweikai.tomatoclock.core.domain.settings

import com.xuweikai.tomatoclock.core.model.AppSettings

object SettingsValidator {
    const val MIN_DURATION_MIN = 1
    const val MAX_DURATION_MIN = 60
    const val MIN_LONG_BREAK_INTERVAL = 2
    const val MAX_LONG_BREAK_INTERVAL = 8

    val supportedAlertSounds: Set<String> = setOf("classic", "soft", "pulse")

    fun validate(settings: AppSettings): List<SettingsValidationError> {
        return buildList {
            validateDuration(SettingsField.FOCUS_DURATION, settings.focusDurationMin)?.let(::add)
            validateDuration(SettingsField.SHORT_BREAK_DURATION, settings.shortBreakDurationMin)?.let(::add)
            validateDuration(SettingsField.LONG_BREAK_DURATION, settings.longBreakDurationMin)?.let(::add)
            validateLongBreakInterval(settings.longBreakInterval)?.let(::add)
            validateAlertSound(settings.alertSound)?.let(::add)
        }
    }

    fun validateField(field: SettingsField, value: Int): SettingsValidationError? {
        return when (field) {
            SettingsField.FOCUS_DURATION,
            SettingsField.SHORT_BREAK_DURATION,
            SettingsField.LONG_BREAK_DURATION -> validateDuration(field, value)
            SettingsField.LONG_BREAK_INTERVAL -> validateLongBreakInterval(value)
            SettingsField.ALERT_SOUND -> null
        }
    }

    fun requireValid(settings: AppSettings) {
        val errors = validate(settings)
        require(errors.isEmpty()) {
            errors.joinToString(separator = "; ") { "${it.field}: ${it.message}" }
        }
    }

    fun sanitize(settings: AppSettings): AppSettings {
        val defaults = AppSettings()
        return settings.copy(
            focusDurationMin = settings.focusDurationMin.takeIf(::isValidDuration)
                ?: defaults.focusDurationMin,
            shortBreakDurationMin = settings.shortBreakDurationMin.takeIf(::isValidDuration)
                ?: defaults.shortBreakDurationMin,
            longBreakDurationMin = settings.longBreakDurationMin.takeIf(::isValidDuration)
                ?: defaults.longBreakDurationMin,
            longBreakInterval = settings.longBreakInterval
                .takeIf { it in MIN_LONG_BREAK_INTERVAL..MAX_LONG_BREAK_INTERVAL }
                ?: defaults.longBreakInterval,
            alertSound = settings.alertSound.takeIf(supportedAlertSounds::contains)
                ?: defaults.alertSound,
        )
    }

    fun isSupportedAlertSound(sound: String): Boolean = sound in supportedAlertSounds

    private fun validateDuration(
        field: SettingsField,
        value: Int,
    ): SettingsValidationError? {
        return if (isValidDuration(value)) {
            null
        } else {
            SettingsValidationError(field, "Duration must be between 1 and 60 minutes.")
        }
    }

    private fun validateLongBreakInterval(value: Int): SettingsValidationError? {
        return if (value in MIN_LONG_BREAK_INTERVAL..MAX_LONG_BREAK_INTERVAL) {
            null
        } else {
            SettingsValidationError(
                SettingsField.LONG_BREAK_INTERVAL,
                "Long break interval must be between 2 and 8 focus sessions.",
            )
        }
    }

    private fun validateAlertSound(sound: String): SettingsValidationError? {
        return if (sound in supportedAlertSounds) {
            null
        } else {
            SettingsValidationError(SettingsField.ALERT_SOUND, "Unsupported alert sound.")
        }
    }

    private fun isValidDuration(value: Int): Boolean = value in MIN_DURATION_MIN..MAX_DURATION_MIN
}
