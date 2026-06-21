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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    init {
        // Observe settings changes and update idle timer display
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.focusDurationMin }
                .distinctUntilChanged()
                .collect { focusDurationMin ->
                    val current = _uiState.value
                    android.util.Log.d("TimerViewModel", "settings collect: focusDurationMin=$focusDurationMin, status=${current.status}, session=${current.session?.sessionId}")
                    // Only update if idle (no active session)
                    if (current.status == TimerStatus.IDLE && current.session == null) {
                        val durationSec = focusDurationMin * 60
                        android.util.Log.d("TimerViewModel", "settings collect: updating idle state, durationSec=$durationSec")
                        _uiState.value = current.copy(
                            plannedDurationSec = durationSec,
                            remainingSec = durationSec,
                        )
                    }
                }
        }
    }

    fun startFocus(taskId: String? = null) {
        viewModelScope.launch {
            stateMutex.withLock {
                val activeSession = timerRepository.getActiveSession()
                android.util.Log.d("TimerViewModel", "startFocus: activeSession=${activeSession?.sessionId}, status=${activeSession?.status}")
                if (activeSession != null) {
                    restoreActiveSessionInternal(activeSession)
                    return@withLock
                }

                // Check for existing idle session (could be focus or break)
                val readySession = _uiState.value.session?.takeIf {
                    it.status == TimerStatus.IDLE
                }
                android.util.Log.d("TimerViewModel", "startFocus: readySession=${readySession?.sessionId}, mode=${readySession?.mode}, plannedDuration=${readySession?.plannedDurationSec}")

                if (readySession != null) {
                    // Start the existing idle session (e.g., break session after focus completed)
                    val session = stateMachine.transition(
                        current = readySession,
                        event = TimerEvent.Start(
                            mode = readySession.mode,
                            plannedDurationSec = readySession.plannedDurationSec,
                            taskId = if (readySession.mode == TimerMode.FOCUS) taskId else null,
                        ),
                    ) ?: return@withLock
                    timerRepository.updateSession(session)
                    appendLog(session, OperationType.START)
                    publishState(session)
                    startEngine(session)
                } else {
                    // Create new focus session
                    val durationSec = settingsRepository.getSettings().focusDurationMin.toSeconds()
                    android.util.Log.d("TimerViewModel", "startFocus: creating new session, durationSec=$durationSec")
                    val session = stateMachine.transition(
                        current = null,
                        event = TimerEvent.Start(
                            mode = TimerMode.FOCUS,
                            plannedDurationSec = durationSec,
                            taskId = taskId,
                        ),
                    ) ?: return@withLock
                    timerRepository.createSession(session)
                    appendLog(session, OperationType.START)
                    publishState(session)
                    startEngine(session)
                }
            }
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
        android.util.Log.d("TimerViewModel", "confirmReset called")
        viewModelScope.launch {
            stateMutex.withLock {
                val session = currentOrActiveSession()
                android.util.Log.d("TimerViewModel", "confirmReset: session=${session?.sessionId}, status=${session?.status}")
                if (session == null) return@withLock

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
                android.util.Log.d("TimerViewModel", "confirmReset: current.status=${current.status}, remainingSec=${current.remainingSec}")

                val invalid = stateMachine.transition(current, TimerEvent.Reset())
                android.util.Log.d("TimerViewModel", "confirmReset: transition result=${invalid?.status}")
                if (invalid == null) return@withLock

                timerEngine.stop()
                timerRepository.updateSession(invalid)
                appendLog(invalid, OperationType.RESET)
                // Reset to idle state with latest settings duration
                val settings = settingsRepository.getSettings()
                val durationSec = settings.focusDurationMin * 60
                android.util.Log.d("TimerViewModel", "confirmReset: focusDurationMin=${settings.focusDurationMin}, durationSec=$durationSec")
                _uiState.value = TimerUiState(
                    mode = TimerMode.FOCUS,
                    plannedDurationSec = durationSec,
                    remainingSec = durationSec,
                )
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
            stateMutex.withLock {
                val activeSession = timerRepository.getActiveSession()
                android.util.Log.d("TimerViewModel", "restoreLatestSession: activeSession=${activeSession?.sessionId}, status=${activeSession?.status}")
                if (activeSession == null) {
                    val settings = settingsRepository.getSettings()
                    val durationSec = settings.focusDurationMin * 60
                    android.util.Log.d("TimerViewModel", "restoreLatestSession: setting idle state, focusDurationMin=${settings.focusDurationMin}, durationSec=$durationSec")
                    _uiState.value = TimerUiState(
                        mode = TimerMode.FOCUS,
                        plannedDurationSec = durationSec,
                        remainingSec = durationSec,
                    )
                    return@withLock
                }
                restoreActiveSessionInternal(activeSession)
            }
        }
    }

    override fun onCleared() {
        timerEngine.stop()
        super.onCleared()
    }

    /**
     * Restore active session - must be called while holding stateMutex.
     */
    private suspend fun restoreActiveSessionInternal(session: TimerSession) {
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
            finishSessionInternal(restored)
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
                finishSessionInternal(ticked)
            }
        }
    }

    private suspend fun finishCurrentSession() {
        stateMutex.withLock {
            val session = _uiState.value.session ?: timerRepository.getActiveSession() ?: return
            finishSessionInternal(session)
        }
    }

    private suspend fun finishSession(session: TimerSession) {
        stateMutex.withLock {
            finishSessionInternal(session)
        }
    }

    /**
     * Internal finish logic - must be called while holding stateMutex.
     *
     * NOTE: This may be invoked from inside the timer engine's own coroutine (e.g. via
     * handleTick -> finishSessionInternal). Calling timerEngine.stop() cancels that very
     * coroutine, which would make every subsequent suspend call throw CancellationException.
     * To keep the finish flow (DB update, alert, event emit, next-cycle setup) atomic, all
     * work after timerEngine.stop() runs under NonCancellable.
     */
    private suspend fun finishSessionInternal(session: TimerSession) {
        val freshSession = timerRepository.findSession(session.sessionId) ?: session
        if (freshSession.status == TimerStatus.COMPLETED || freshSession.status == TimerStatus.INVALID) {
            return
        }

        timerEngine.stop()

        withContext(NonCancellable) {
            val completed = stateMachine.transition(
                freshSession.copy(remainingSec = 0),
                TimerEvent.Complete,
            ) ?: return@withContext

            timerRepository.updateSession(completed)
            appendLog(completed, OperationType.COMPLETE)
            publishState(completed)

            // Trigger alert (non-critical, catch exceptions)
            runCatching { alertManager.notifyFinish(completed.mode) }
            runCatching { _effects.emit(TimerEffect.TimerFinished(completed.mode)) }

            // Transition to next state (critical - must not fail)
            try {
                when (completed.mode) {
                    TimerMode.FOCUS -> completeFocus(completed)
                    TimerMode.SHORT_BREAK,
                    TimerMode.LONG_BREAK,
                    -> completeBreak(completed)
                }
            } catch (e: Exception) {
                // If transition fails, create a basic idle session so user can continue
                val fallbackSession = TimerSession(
                    sessionId = idGenerator.nextId(),
                    taskId = null,
                    mode = TimerMode.FOCUS,
                    plannedDurationSec = completed.plannedDurationSec,
                    remainingSec = completed.plannedDurationSec,
                    status = TimerStatus.IDLE,
                    startAt = clock.elapsedRealtimeMillis(),
                    pauseAt = null,
                    completedAt = null,
                    resetAt = null,
                    invalidReason = null,
                )
                timerRepository.createSession(fallbackSession)
                publishState(fallbackSession)
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
        // Non-critical: archive focus record
        runCatching { focusCompletedEventSink.emit(event) }
        runCatching { _effects.emit(TimerEffect.FocusCompleted(event)) }

        // Determine break type
        val settings = settingsRepository.getSettings()
        val validFocusCount = timerRepository.countValidFocusSinceLastLongBreak()
        val nextStep = cycleManager.nextAfter(
            session = completed,
            validFocusCount = validFocusCount,
            settings = settings,
        )
        val breakDurationSec = when (nextStep) {
            is TimerCycleStep.StartBreak -> nextStep.plannedDurationSec
            else -> settings.shortBreakDurationMin * 60
        }
        val breakMode = when (nextStep) {
            is TimerCycleStep.StartBreak -> nextStep.mode
            else -> TimerMode.SHORT_BREAK
        }
        // Create idle break session - user will manually start
        val readySession = TimerSession(
            sessionId = idGenerator.nextId(),
            taskId = null,
            mode = breakMode,
            plannedDurationSec = breakDurationSec,
            remainingSec = breakDurationSec,
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
