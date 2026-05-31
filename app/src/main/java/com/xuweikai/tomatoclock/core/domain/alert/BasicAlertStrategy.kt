package com.xuweikai.tomatoclock.core.domain.alert

object BasicAlertStrategy {
    fun chooseDelivery(environment: AlertEnvironment): AlertDelivery {
        return when {
            !environment.isSilent && environment.vibrationEnabled -> AlertDelivery.SOUND_AND_VIBRATION
            !environment.isSilent -> AlertDelivery.SOUND_ONLY
            environment.vibrationEnabled -> AlertDelivery.VIBRATION_ONLY
            else -> AlertDelivery.POPUP_ONLY
        }
    }
}
