package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleManagerTest {
    private val cycleManager = CycleManager()
    private val settings = AppSettings(
        focusDurationMin = 25,
        shortBreakDurationMin = 5,
        longBreakDurationMin = 15,
        longBreakInterval = 4,
    )

    @Test
    fun focusCompletionBeforeIntervalStartsShortBreak() {
        val next = cycleManager.nextAfter(
            session = completedSession(TimerMode.FOCUS),
            validFocusCount = 3,
            settings = settings,
        )

        assertTrue(next is TimerCycleStep.StartBreak)
        next as TimerCycleStep.StartBreak
        assertEquals(TimerMode.SHORT_BREAK, next.mode)
        assertEquals(300, next.plannedDurationSec)
    }

    @Test
    fun fourthValidFocusStartsLongBreak() {
        val next = cycleManager.nextAfter(
            session = completedSession(TimerMode.FOCUS),
            validFocusCount = 4,
            settings = settings,
        )

        assertTrue(next is TimerCycleStep.StartBreak)
        next as TimerCycleStep.StartBreak
        assertEquals(TimerMode.LONG_BREAK, next.mode)
        assertEquals(900, next.plannedDurationSec)
    }

    @Test
    fun breakCompletionPreparesNextFocusWithoutAutoStart() {
        val next = cycleManager.nextAfter(
            session = completedSession(TimerMode.SHORT_BREAK),
            validFocusCount = 1,
            settings = settings,
        )

        assertTrue(next is TimerCycleStep.ReadyNextFocus)
        next as TimerCycleStep.ReadyNextFocus
        assertEquals(1_500, next.plannedDurationSec)
    }

    private fun completedSession(mode: TimerMode): TimerSession = TimerSession(
        sessionId = "session",
        taskId = null,
        mode = mode,
        plannedDurationSec = 1,
        remainingSec = 0,
        status = TimerStatus.COMPLETED,
        startAt = 0L,
        pauseAt = null,
        completedAt = 1L,
        resetAt = null,
        invalidReason = null,
    )
}

