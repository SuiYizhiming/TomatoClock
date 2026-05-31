package com.xuweikai.tomatoclock.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsDefaultsTest {
    @Test
    fun defaultSettingsMatchProductContract() {
        val settings = AppSettings()

        assertEquals(25, settings.focusDurationMin)
        assertEquals(5, settings.shortBreakDurationMin)
        assertEquals(15, settings.longBreakDurationMin)
        assertEquals(4, settings.longBreakInterval)
        assertEquals("classic", settings.alertSound)
        assertTrue(settings.vibrationEnabled)
    }
}
