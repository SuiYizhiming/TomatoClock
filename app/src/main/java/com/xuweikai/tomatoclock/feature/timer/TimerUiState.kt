package com.xuweikai.tomatoclock.feature.timer

import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus

data class TimerUiState(
    val session: TimerSession? = null,
    val mode: TimerMode = TimerMode.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val remainingSec: Int = 0,
    val plannedDurationSec: Int = 0,
    val resetConfirmationVisible: Boolean = false,
) {
    val isRunning: Boolean = status == TimerStatus.RUNNING
    val isPaused: Boolean = status == TimerStatus.PAUSED
}

sealed interface TimerEffect {
    data class FocusCompleted(
        val event: com.xuweikai.tomatoclock.core.domain.event.FocusCompletedEvent,
    ) : TimerEffect

    data class TimerFinished(
        val mode: TimerMode,
    ) : TimerEffect
}

