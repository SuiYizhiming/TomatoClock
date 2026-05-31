package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TimerStateMachineTest {
    private val clock = FakeTimerClock()
    private val stateMachine = TimerStateMachine(clock, SequenceTimerIdGenerator())

    @Test
    fun duplicateStartKeepsCurrentActiveSession() {
        val session = stateMachine.transition(
            current = null,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 1_500,
                taskId = "task-1",
            ),
        )

        val duplicate = stateMachine.transition(
            current = session,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 1_500,
                taskId = "task-1",
            ),
        )

        assertSame(session, duplicate)
    }

    @Test
    fun startReusesIdlePreparedSession() {
        val idle = com.xuweikai.tomatoclock.core.model.TimerSession(
            sessionId = "ready-focus",
            taskId = null,
            mode = TimerMode.FOCUS,
            plannedDurationSec = 1_500,
            remainingSec = 1_500,
            status = TimerStatus.IDLE,
            startAt = 0L,
            pauseAt = null,
            completedAt = null,
            resetAt = null,
            invalidReason = null,
        )

        val running = stateMachine.transition(
            current = idle,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 1_500,
                taskId = "task-1",
            ),
        )!!

        assertEquals("ready-focus", running.sessionId)
        assertEquals("task-1", running.taskId)
        assertEquals(TimerStatus.RUNNING, running.status)
    }

    @Test
    fun pauseAndResumePreserveElapsedTimeWithMonotonicClock() {
        val running = stateMachine.transition(
            current = null,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 1_500,
                taskId = null,
            ),
        )!!

        clock.advance(12_000L)
        val remainingAtPause = TimerSessionRestorer.remainingSeconds(
            session = running,
            nowElapsedMillis = clock.elapsedRealtimeMillis(),
        )
        val paused = stateMachine.transition(
            current = running.copy(remainingSec = remainingAtPause),
            event = TimerEvent.Pause,
        )!!

        clock.advance(8_000L)
        val resumed = stateMachine.transition(paused, TimerEvent.Resume)!!

        clock.advance(10_000L)
        val restoredRemaining = TimerSessionRestorer.remainingSeconds(
            session = resumed,
            nowElapsedMillis = clock.elapsedRealtimeMillis(),
        )

        assertEquals(TimerStatus.PAUSED, paused.status)
        assertEquals(1_488, paused.remainingSec)
        assertEquals(TimerStatus.RUNNING, resumed.status)
        assertEquals(1_478, restoredRemaining)
    }

    @Test
    fun resetMarksSessionInvalidWithoutCompletingIt() {
        val running = stateMachine.transition(
            current = null,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 1_500,
                taskId = null,
            ),
        )!!

        val invalid = stateMachine.transition(running, TimerEvent.Reset())!!

        assertEquals(TimerStatus.INVALID, invalid.status)
        assertEquals(null, invalid.completedAt)
        assertEquals("User reset timer", invalid.invalidReason)
    }

    @Test
    fun restoreRecomputesRunningRemainingFromElapsedRealtime() {
        val running = stateMachine.transition(
            current = null,
            event = TimerEvent.Start(
                mode = TimerMode.FOCUS,
                plannedDurationSec = 60,
                taskId = null,
            ),
        )!!

        clock.advance(27_000L)
        val restored = TimerSessionRestorer.restore(
            session = running.copy(remainingSec = 60),
            nowElapsedMillis = clock.elapsedRealtimeMillis(),
        )

        assertEquals(33, restored.remainingSec)
    }
}
