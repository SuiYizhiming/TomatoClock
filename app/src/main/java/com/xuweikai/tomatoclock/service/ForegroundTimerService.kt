package com.xuweikai.tomatoclock.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xuweikai.tomatoclock.core.domain.timer.MonotonicTimerEngine
import com.xuweikai.tomatoclock.core.domain.timer.TimerEngine
import com.xuweikai.tomatoclock.core.model.TimerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class ForegroundTimerService : Service() {
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timerEngine: TimerEngine = MonotonicTimerEngine()
    private val _events = MutableSharedFlow<ForegroundTimerEvent>(extraBufferCapacity = 1)

    val events: SharedFlow<ForegroundTimerEvent> = _events

    override fun onBind(intent: Intent?): IBinder = binder

    fun startTimer(session: TimerSession) {
        timerEngine.start(
            scope = serviceScope,
            session = session,
            onTick = { remainingSec ->
                _events.emit(ForegroundTimerEvent.Tick(session.sessionId, remainingSec))
            },
            onFinish = {
                _events.emit(ForegroundTimerEvent.Finished(session.sessionId))
            },
        )
    }

    fun stopTimer() {
        timerEngine.stop()
    }

    override fun onDestroy() {
        timerEngine.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun service(): ForegroundTimerService = this@ForegroundTimerService
    }
}

sealed interface ForegroundTimerEvent {
    data class Tick(
        val sessionId: String,
        val remainingSec: Int,
    ) : ForegroundTimerEvent

    data class Finished(
        val sessionId: String,
    ) : ForegroundTimerEvent
}

