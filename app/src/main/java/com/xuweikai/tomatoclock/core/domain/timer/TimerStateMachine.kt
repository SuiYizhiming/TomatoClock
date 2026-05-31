package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus

class TimerStateMachine(
    private val clock: TimerClock,
    private val idGenerator: TimerIdGenerator = UuidTimerIdGenerator,
) {
    fun transition(
        current: TimerSession?,
        event: TimerEvent,
    ): TimerSession? {
        return when (event) {
            is TimerEvent.Start -> start(current, event)
            TimerEvent.Pause -> pause(current)
            TimerEvent.Resume -> resume(current)
            TimerEvent.Complete -> complete(current)
            is TimerEvent.Reset -> reset(current, event.reason)
            is TimerEvent.Tick -> tick(current, event.remainingSec)
            is TimerEvent.Restore -> restore(current, event.remainingSec)
        }
    }

    private fun start(
        current: TimerSession?,
        event: TimerEvent.Start,
    ): TimerSession? {
        if (current?.status == TimerStatus.RUNNING || current?.status == TimerStatus.PAUSED) {
            return current
        }

        if (current?.status == TimerStatus.IDLE) {
            return current.copy(
                taskId = event.taskId,
                mode = event.mode,
                plannedDurationSec = event.plannedDurationSec,
                remainingSec = event.plannedDurationSec,
                status = TimerStatus.RUNNING,
                startAt = clock.elapsedRealtimeMillis(),
                pauseAt = null,
                completedAt = null,
                resetAt = null,
                invalidReason = null,
            )
        }

        return TimerSession(
            sessionId = idGenerator.nextId(),
            taskId = event.taskId,
            mode = event.mode,
            plannedDurationSec = event.plannedDurationSec,
            remainingSec = event.plannedDurationSec,
            status = TimerStatus.RUNNING,
            startAt = clock.elapsedRealtimeMillis(),
            pauseAt = null,
            completedAt = null,
            resetAt = null,
            invalidReason = null,
        )
    }

    private fun pause(current: TimerSession?): TimerSession? {
        if (current?.status != TimerStatus.RUNNING) return current

        return current.copy(
            status = TimerStatus.PAUSED,
            pauseAt = clock.wallTimeMillis(),
        )
    }

    private fun resume(current: TimerSession?): TimerSession? {
        if (current?.status != TimerStatus.PAUSED) return current

        val elapsedBeforePauseSec = current.plannedDurationSec - current.remainingSec
        val adjustedStartAt = clock.elapsedRealtimeMillis() - elapsedBeforePauseSec * MILLIS_PER_SECOND
        return current.copy(
            status = TimerStatus.RUNNING,
            startAt = adjustedStartAt,
            pauseAt = null,
        )
    }

    private fun complete(current: TimerSession?): TimerSession? {
        if (current?.status != TimerStatus.RUNNING && current?.status != TimerStatus.PAUSED) return current

        return current.copy(
            status = TimerStatus.COMPLETED,
            remainingSec = 0,
            completedAt = clock.wallTimeMillis(),
        )
    }

    private fun reset(
        current: TimerSession?,
        reason: String,
    ): TimerSession? {
        if (current?.status != TimerStatus.RUNNING && current?.status != TimerStatus.PAUSED) return current

        return current.copy(
            status = TimerStatus.INVALID,
            resetAt = clock.wallTimeMillis(),
            invalidReason = reason,
        )
    }

    private fun tick(
        current: TimerSession?,
        remainingSec: Int,
    ): TimerSession? {
        if (current?.status != TimerStatus.RUNNING) return current

        return current.copy(remainingSec = remainingSec.coerceAtLeast(0))
    }

    private fun restore(
        current: TimerSession?,
        remainingSec: Int,
    ): TimerSession? {
        if (current?.status != TimerStatus.RUNNING) return current

        return current.copy(remainingSec = remainingSec.coerceAtLeast(0))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
