package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import kotlin.math.ceil

object TimerSessionRestorer {
    fun remainingSeconds(
        session: TimerSession,
        nowElapsedMillis: Long,
    ): Int {
        if (session.status != TimerStatus.RUNNING) return session.remainingSec

        val elapsedMillis = (nowElapsedMillis - session.startAt).coerceAtLeast(0L)
        val elapsedSeconds = ceil(elapsedMillis / MILLIS_PER_SECOND.toDouble()).toInt()
        return (session.plannedDurationSec - elapsedSeconds).coerceAtLeast(0)
    }

    fun restore(
        session: TimerSession,
        nowElapsedMillis: Long,
    ): TimerSession {
        if (session.status != TimerStatus.RUNNING) return session

        return session.copy(remainingSec = remainingSeconds(session, nowElapsedMillis))
    }

    private const val MILLIS_PER_SECOND = 1_000L
}

