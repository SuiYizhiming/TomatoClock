package com.xuweikai.tomatoclock.core.domain.timer

import com.xuweikai.tomatoclock.core.model.TimerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface TimerEngine {
    fun start(
        scope: CoroutineScope,
        session: TimerSession,
        onTick: suspend (remainingSec: Int) -> Unit,
        onFinish: suspend () -> Unit,
    )

    fun stop()
}

class MonotonicTimerEngine(
    private val clock: TimerClock = SystemTimerClock,
) : TimerEngine {
    private var job: Job? = null

    override fun start(
        scope: CoroutineScope,
        session: TimerSession,
        onTick: suspend (remainingSec: Int) -> Unit,
        onFinish: suspend () -> Unit,
    ) {
        stop()
        job = scope.launch {
            while (true) {
                val remainingSec = TimerSessionRestorer.remainingSeconds(
                    session = session,
                    nowElapsedMillis = clock.elapsedRealtimeMillis(),
                )
                onTick(remainingSec)
                if (remainingSec <= 0) {
                    onFinish()
                    break
                }
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}

