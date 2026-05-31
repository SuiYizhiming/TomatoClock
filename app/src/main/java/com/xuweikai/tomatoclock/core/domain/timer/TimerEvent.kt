package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.TimerMode

sealed interface TimerEvent {
    data class Start(
        val mode: TimerMode,
        val plannedDurationSec: Int,
        val taskId: String?,
    ) : TimerEvent

    data object Pause : TimerEvent
    data object Resume : TimerEvent
    data object Complete : TimerEvent

    data class Reset(
        val reason: String = "User reset timer",
    ) : TimerEvent

    data class Tick(
        val remainingSec: Int,
    ) : TimerEvent

    data class Restore(
        val remainingSec: Int,
    ) : TimerEvent
}

