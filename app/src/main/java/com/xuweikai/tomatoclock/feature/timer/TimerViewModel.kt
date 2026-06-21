package com.xuweikai.tomatoclock.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuweikai.tomatoclock.core.domain.event.FocusCompletedEvent
import com.xuweikai.tomatoclock.core.domain.repository.AlertManager
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.repository.TimerRepository
import com.xuweikai.tomatoclock.core.domain.timer.CycleManager
import com.xuweikai.tomatoclock.core.domain.timer.MonotonicTimerEngine
import com.xuweikai.tomatoclock.core.domain.timer.SystemTimerClock
import com.xuweikai.tomatoclock.core.domain.timer.TimerClock
import com.xuweikai.tomatoclock.core.domain.timer.TimerCycleStep
import com.xuweikai.tomatoclock.core.domain.timer.TimerEngine
import com.xuweikai.tomatoclock.core.domain.timer.TimerEvent
import com.xuweikai.tomatoclock.core.domain.timer.TimerIdGenerator
import com.xuweikai.tomatoclock.core.domain.timer.TimerSessionRestorer
import com.xuweikai.tomatoclock.core.domain.timer.TimerStateMachine
import com.xuweikai.tomatoclock.core.domain.timer.UuidTimerIdGenerator
import com.xuweikai.tomatoclock.core.model.OperationType
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerOperationLog
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TimerViewModel(
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository,
    private val alertManager: AlertManager,
    private val focusCompletedEventSink: FocusCompletedEventSink = NoopFocusCompletedEventSink,
    private val clock: TimerClock = SystemTimerClock,
    private val idGenerator: TimerIdGenerator = UuidTimerIdGenerator,
    private val stateMachine: TimerStateMachine = TimerStateMachine(clock, idGenerator),
    private val cycleManager: CycleManager = CycleManager(),
    private val timerEngine: TimerEngine = MonotonicTimerEngine(clock),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TimerEffect>()
    val effects: SharedFlow<TimerEffect> = _effects.asSharedFlow()

    private val stateMutex = Mutex()

    fun startFocus(taskId: String? = null) {
        viewModelScope.launch {
            val activeSession = timerRepository.getActiveSession()
            if (activeSession != null) {
                restoreActiveSession(activeSession)
                return@launch
            }

            val durationSec = settingsRepository.getSettings().focusDurationMin.toSeconds()
            val readySession = _uiState.value.session?.takeIf {
                it.status == TimerStatus.IDLE && it.mode == TimerMode.FOCUS
            }
            val session = stateMachine.transition(
                current = readySession,
                event = TimerEvent.Start(
                    mode = TimerMode.FOCUS,
                    plannedDurationSec = durationSec,
                    taskId = taskId,
                ),
            ) ?: return@launch

            if (readySession == null) {
                timerRepository.createSession(session)
            } else {
                timerRepository.updateSession(session)
            }
            appendLog(session, OperationType.START)
            publishState(session)
            startEngine(session)
        }
    }

    fun pauseOrResume() {
        viewModelScope.launch {
            val session = currentOrActiveSession() ?: return@launch
            when (session.status) {
                TimerStatus.RUNNING -> pause(session)
                TimerStatus.PAUSED -> resume(session)
                else -> Unit
            }
        }
    }

    fun reset() {
        _uiState.update { it.copy(resetConfirmationVisible = true) }
    }

    fun confirmReset() {
        viewModelScope.launch {
            stateMutex.withLock {
                val session = currentOrActiveSession() ?: return@withLock
                val current = if (session.status == TimerStatus.RUNNING) {
                    session.copy(
                        remainingSec = TimerSessionRestorer.remainingSeconds(
                            session = session,
                            nowElapsedMillis = clock.elapsedRealtimeMillis(),
                        ),
                    )
                } else {
                    session
                }
                val invalid = stateMachine.transition(current, TimerEvent.Reset()) ?: return@withLock

                timerEngine.stop()
                timerRepository.updateSession(invalid)
                appendLog(invalid, OperationType.RESET)
                // Clear UI state to clean idle - don't keep invalid session in UI
                _uiState.value = TimerUiState()
            }
        }
    }

    fun onTick() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: timerRepository.getActiveSession() ?: return@launch
            handleTick(
                session = session,
                remainingSec = TimerSessionRestorer.remainingSeconds(
                    session = session,
                    nowElapsedMillis = clock.elapsedRealtimeMillis(),
                ),
            )
        }
    }

    fun onFinish() {
        viewModelScope.launch {
            finishCurrentSession()
        }
    }

    fun restoreLatestSession() {
        viewModelScope.launch {
            val activeSession = timerRepository.getActiveSession()
            if (activeSession == null) {
                _uiState.value = TimerUiState()
                return@launch
            }
            restoreActiveSession(activeSession)
        }
    }

    override fun onCleared() {
        timerEngine.stop()
        super.onCleared()
    }

    private suspend fun restoreActiveSession(session: TimerSession) {
        if (session.status == TimerStatus.PAUSED) {
            timerRepository.appendLog(operationLog(session, OperationType.RESTORE))
            publishState(session)
            return
        }

        val restored = TimerSessionRestorer.restore(
            session = session,
            nowElapsedMillis = clock.elapsedRealtimeMillis(),
        )
        timerRepository.updateSession(restored)
        timerRepository.appendLog(operationLog(restored, OperationType.RESTORE))
        publishState(restored)
        if (restored.remainingSec <= 0) {
            finishSession(restored)
        } else {
            startEngine(restored)
        }
    }

    private suspend fun pause(session: TimerSession) {
        stateMutex.withLock {
            val remainingSec = TimerSessionRestorer.remainingSeconds(
                session = session,
                nowElapsedMillis = clock.elapsedRealtimeMillis(),
            )
            val sessionWithRemaining = session.copy(remainingSec = remainingSec)
            val paused = stateMachine.transition(sessionWithRemaining, TimerEvent.Pause) ?: return

            timerEngine.stop()
            timerRepository.updateSession(paused)
            appendLog(paused, OperationType.PAUSE)
            publishState(paused)
        }
    }

    private suspend fun resume(session: TimerSession) {
        stateMutex.withLock {
            val resumed = stateMachine.transition(session, TimerEvent.Resume) ?: return

            timerRepository.updateSession(resumed)
            appendLog(resumed, OperationType.RESUME)
            publishState(resumed)
            startEngine(resumed)
        }
    }

    private suspend fun handleTick(
        session: TimerSession,
        remainingSec: Int,
    ) {
        stateMutex.withLock {
            val activeSession = timerRepository.findSession(session.sessionId) ?: return
            if (activeSession.status != TimerStatus.RUNNING) return

            val ticked = stateMachine.transition(activeSession, TimerEvent.Tick(remainingSec)) ?: return
            timerRepository.updateSession(ticked)
            publishState(ticked)
            if (remainingSec <= 0) {
                finishSession(ticked)
            }
        }
    }

    private suspend fun finishCurrentSession() {
        val session = _uiState.value.session ?: timerRepository.getActiveSession() ?: return
        finishSession(session)
    }

    private suspend fun finishSession(session: TimerSession) {
        stateMutex.withLock {
            val freshSession = timerRepository.findSession(session.sessionId) ?: session
            if (freshSession.status == TimerStatus.COMPLETED || freshSession.status == TimerStatus.INVALID) {
                return
            }

            val completed = stateMachine.transition(
                freshSession.copy(remainingSec = 0),
                TimerEvent.Complete,
            ) ?: return

            timerRepository.updateSession(completed)
            appendLog(completed, OperationType.COMPLETE)
            alertManager.notifyFinish(completed.mode)
            _effects.emit(TimerEffect.TimerFinished(completed.mode))
            publishState(completed)

            when (completed.mode) {
                TimerMode.FOCUS -> completeFocus(completed)
                TimerMode.SHORT_BREAK,
                TimerMode.LONG_BREAK,
                -> completeBreak(completed)
            }
        }
    }

    private suspend fun completeFocus(completed: TimerSession) {
        val event = FocusCompletedEvent(
            sessionId = completed.sessionId,
            taskId = completed.taskId,
            durationSeconds = completed.plannedDurationSec,
            completedAt = completed.completedAt ?: clock.wallTimeMillis(),
        )
        focusCompletedEventSink.emit(event)
        _effects.emit(TimerEffect.FocusCompleted(event))

        val validFocusCount = timerRepository.countValidFocusSinceLastLongBreak()
        val nextStep = cycleManager.nextAfter(
            session = completed,
            validFocusCount = validFocusCount,
            settings = settingsRepository.getSettings(),
        )
        if (nextStep is TimerCycleStep.StartBreak) {
            val breakSession = createRunningSession(
                mode = nextStep.mode,
                plannedDurationSec = nextStep.plannedDurationSec,
                taskId = null,
            )
            timerRepository.createSession(breakSession)
            appendLog(breakSession, OperationType.START)
            publishState(breakSession)
            startEngine(breakSession)
        }
    }

    private suspend fun completeBreak(completed: TimerSession) {
        val validFocusCount = timerRepository.countValidFocusSinceLastLongBreak()
        val nextStep = cycleManager.nextAfter(
            session = completed,
            validFocusCount = validFocusCount,
            settings = settingsRepository.getSettings(),
        )
        if (nextStep is TimerCycleStep.ReadyNextFocus) {
            val readySession = TimerSession(
                sessionId = idGenerator.nextId(),
                taskId = null,
                mode = TimerMode.FOCUS,
                plannedDurationSec = nextStep.plannedDurationSec,
                remainingSec = nextStep.plannedDurationSec,
                status = TimerStatus.IDLE,
                startAt = clock.elapsedRealtimeMillis(),
                pauseAt = null,
                completedAt = null,
                resetAt = null,
                invalidReason = null,
            )
            timerRepository.createSession(readySession)
            publishState(readySession)
        }
    }

    private fun createRunningSession(
        mode: TimerMode,
        plannedDurationSec: Int,
        taskId: String?,
    ): TimerSession {
        return stateMachine.transition(
            current = null,
            event = TimerEvent.Start(
                mode = mode,
                plannedDurationSec = plannedDurationSec,
                taskId = taskId,
            ),
        ) ?: error("TimerStateMachine failed to create a running session")
    }

    private fun startEngine(session: TimerSession) {
        timerEngine.start(
            scope = viewModelScope,
            session = session,
            onTick = { remainingSec -> handleTick(session, remainingSec) },
            onFinish = { finishSession(session) },
        )
    }

    private suspend fun currentOrActiveSession(): TimerSession? {
        return _uiState.value.session ?: timerRepository.getActiveSession()
    }

    private fun publishState(session: TimerSession) {
        _uiState.value = TimerUiState(
            session = session,
            mode = session.mode,
            status = session.status,
            remainingSec = session.remainingSec,
            plannedDurationSec = session.plannedDurationSec,
            resetConfirmationVisible = false,
        )
    }

    private suspend fun appendLog(
        session: TimerSession,
        operation: OperationType,
    ) {
        timerRepository.appendLog(operationLog(session, operation))
    }

    private fun operationLog(
        session: TimerSession,
        operation: OperationType,
    ): TimerOperationLog = TimerOperationLog(
        logId = idGenerator.nextId(),
        sessionId = session.sessionId,
        operation = operation,
        operatedAt = clock.wallTimeMillis(),
        remainingSec = session.remainingSec,
    )

    private fun Int.toSeconds(): Int = this * SECONDS_PER_MINUTE

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
