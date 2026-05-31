package com.xuweikai.tomatoclock.core.domain.alert

import org.junit.Assert.assertEquals
import org.junit.Test

class BasicAlertStrategyTest {

    @Test
    fun normalVolumeUsesSoundAndOptionalVibration() {
        assertEquals(
            AlertDelivery.SOUND_AND_VIBRATION,
            BasicAlertStrategy.chooseDelivery(
                AlertEnvironment(isSilent = false, vibrationEnabled = true),
            ),
        )
        assertEquals(
            AlertDelivery.SOUND_ONLY,
            BasicAlertStrategy.chooseDelivery(
                AlertEnvironment(isSilent = false, vibrationEnabled = false),
            ),
        )
    }

    @Test
    fun silentModeFallsBackToVibrationOrPopup() {
        assertEquals(
            AlertDelivery.VIBRATION_ONLY,
            BasicAlertStrategy.chooseDelivery(
                AlertEnvironment(isSilent = true, vibrationEnabled = true),
            ),
        )
        assertEquals(
            AlertDelivery.POPUP_ONLY,
            BasicAlertStrategy.chooseDelivery(
                AlertEnvironment(isSilent = true, vibrationEnabled = false),
            ),
        )
    }
}
