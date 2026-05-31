package com.xuweikai.tomatoclock.core.domain.repository

import com.xuweikai.tomatoclock.core.model.TimerMode

interface AlertManager {
    suspend fun notifyFinish(mode: TimerMode)
    suspend fun playPreview(sound: String)
}
