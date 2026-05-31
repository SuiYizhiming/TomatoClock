package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus

class CycleManager {
    fun nextAfter(
        session: TimerSession,
        validFocusCount: Int,
        settings: AppSettings,
    ): TimerCycleStep {
        if (session.status != TimerStatus.COMPLETED) return TimerCycleStep.Idle

        return when (session.mode) {
            TimerMode.FOCUS -> nextBreak(validFocusCount, settings)
            TimerMode.SHORT_BREAK,
            TimerMode.LONG_BREAK,
            -> TimerCycleStep.ReadyNextFocus(
                plannedDurationSec = settings.focusDurationMin.toSeconds(),
            )
        }
    }

    private fun nextBreak(
        validFocusCount: Int,
        settings: AppSettings,
    ): TimerCycleStep {
        val interval = settings.longBreakInterval.coerceAtLeast(1)
        val shouldLongBreak = validFocusCount > 0 && validFocusCount % interval == 0
        return if (shouldLongBreak) {
            TimerCycleStep.StartBreak(
                mode = TimerMode.LONG_BREAK,
                plannedDurationSec = settings.longBreakDurationMin.toSeconds(),
            )
        } else {
            TimerCycleStep.StartBreak(
                mode = TimerMode.SHORT_BREAK,
                plannedDurationSec = settings.shortBreakDurationMin.toSeconds(),
            )
        }
    }

    private fun Int.toSeconds(): Int = this * SECONDS_PER_MINUTE

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}

sealed interface TimerCycleStep {
    data class StartBreak(
        val mode: TimerMode,
        val plannedDurationSec: Int,
    ) : TimerCycleStep

    data class ReadyNextFocus(
        val plannedDurationSec: Int,
    ) : TimerCycleStep

    data object Idle : TimerCycleStep
}

