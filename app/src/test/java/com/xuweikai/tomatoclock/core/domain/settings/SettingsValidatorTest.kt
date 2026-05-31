package com.xuweikai.tomatoclock.core.domain.settings

import com.xuweikai.tomatoclock.core.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {

    @Test
    fun defaultSettingsAreValid() {
        assertTrue(SettingsValidator.validate(AppSettings()).isEmpty())
    }

    @Test
    fun rejectsInvalidDurationsAndInterval() {
        val errors = SettingsValidator.validate(
            AppSettings(
                focusDurationMin = 0,
                shortBreakDurationMin = 61,
                longBreakDurationMin = -1,
                longBreakInterval = 9,
            ),
        )

        assertEquals(
            setOf(
                SettingsField.FOCUS_DURATION,
                SettingsField.SHORT_BREAK_DURATION,
                SettingsField.LONG_BREAK_DURATION,
                SettingsField.LONG_BREAK_INTERVAL,
            ),
            errors.map { it.field }.toSet(),
        )
    }

    @Test
    fun rejectsUnsupportedAlertSound() {
        val errors = SettingsValidator.validate(AppSettings(alertSound = "unknown"))

        assertEquals(listOf(SettingsField.ALERT_SOUND), errors.map { it.field })
    }
}
