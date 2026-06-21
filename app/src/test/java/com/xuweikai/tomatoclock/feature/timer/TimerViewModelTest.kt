package com.xuweikai.tomatoclock.feature.timer

import com.xuweikai.tomatoclock.core.domain.event.FocusCompletedEvent
import com.xuweikai.tomatoclock.core.domain.repository.AlertManager
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.repository.TimerRepository
import com.xuweikai.tomatoclock.core.domain.timer.FakeTimerClock
import com.xuweikai.tomatoclock.core.domain.timer.SequenceTimerIdGenerator
import com.xuweikai.tomatoclock.core.domain.timer.TimerEngine
import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.core.model.OperationType
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerOperationLog
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun naturalFocusFinishCompletesSessionAndStartsBreak() = runTest {
        val fixture = timerFixture()

        fixture.viewModel.startFocus(taskId = "task-1")
        advanceUntilIdle()

        fixture.clock.advance(60_000L)
        fixture.timerEngine.finishByTick(remainingSec = 0)
        advanceUntilIdle()

        val focusSession = fixture.timerRepository.sessions.first { it.mode == TimerMode.FOCUS }
        val breakSession = fixture.viewModel.uiState.value.session
        val completeLogs = fixture.timerRepository.logs.filter { it.operation == OperationType.COMPLETE }

        assertEquals(TimerStatus.COMPLETED, focusSession.status)
        assertEquals(0, focusSession.remainingSec)
        assertEquals(TimerMode.SHORT_BREAK, breakSession?.mode)
        assertEquals(TimerStatus.RUNNING, breakSession?.status)
        assertEquals(60, breakSession?.remainingSec)
        assertEquals(listOf(focusSession.sessionId), completeLogs.map { it.sessionId })
        assertEquals(listOf(TimerMode.FOCUS), fixture.alertManager.finishedModes)
        assertEquals(
            listOf(
                FocusCompletedEvent(
                    sessionId = focusSession.sessionId,
                    taskId = "task-1",
                    durationSeconds = 60,
                    completedAt = focusSession.completedAt ?: error("focus completedAt missing"),
                ),
            ),
            fixture.focusCompletedSink.events,
        )

        fixture.close()
    }

    @Test
    fun naturalBreakFinishPreparesNextFocusWithoutAutoStarting() = runTest {
        val fixture = timerFixture()

        fixture.viewModel.startFocus()
        advanceUntilIdle()
        fixture.clock.advance(60_000L)
        fixture.timerEngine.finishByTick(remainingSec = 0)
        advanceUntilIdle()

        fixture.clock.advance(60_000L)
        fixture.timerEngine.finishByTick(remainingSec = 0)
        advanceUntilIdle()

        val readyFocus = fixture.viewModel.uiState.value.session
        val completedBreak = fixture.timerRepository.sessions.first { it.mode == TimerMode.SHORT_BREAK }

        assertEquals(TimerStatus.COMPLETED, completedBreak.status)
        assertEquals(0, completedBreak.remainingSec)
        assertEquals(TimerMode.FOCUS, readyFocus?.mode)
        assertEquals(TimerStatus.IDLE, readyFocus?.status)
        assertEquals(60, readyFocus?.remainingSec)

        fixture.close()
    }

    private fun timerFixture(
        settings: AppSettings = AppSettings(
            focusDurationMin = 1,
            shortBreakDurationMin = 1,
            longBreakDurationMin = 2,
            longBreakInterval = 4,
        ),
    ): TimerViewModelFixture {
        val clock = FakeTimerClock()
        val timerRepository = FakeTimerRepository()
        val settingsRepository = FakeSettingsRepository(settings)
        val alertManager = FakeAlertManager()
        val focusCompletedSink = RecordingFocusCompletedEventSink()
        val timerEngine = FinishableTimerEngine()
        val viewModel = TimerViewModel(
            timerRepository = timerRepository,
            settingsRepository = settingsRepository,
            alertManager = alertManager,
            focusCompletedEventSink = focusCompletedSink,
            clock = clock,
            idGenerator = SequenceTimerIdGenerator(),
            timerEngine = timerEngine,
        )

        return TimerViewModelFixture(
            viewModel = viewModel,
            timerRepository = timerRepository,
            alertManager = alertManager,
            focusCompletedSink = focusCompletedSink,
            timerEngine = timerEngine,
            clock = clock,
        )
    }

    private data class TimerViewModelFixture(
        val viewModel: TimerViewModel,
        val timerRepository: FakeTimerRepository,
        val alertManager: FakeAlertManager,
        val focusCompletedSink: RecordingFocusCompletedEventSink,
        val timerEngine: FinishableTimerEngine,
        val clock: FakeTimerClock,
    ) {
        fun close() {
            timerEngine.stop()
        }
    }

    private class FakeTimerRepository : TimerRepository {
        private val storedSessions = MutableStateFlow<List<TimerSession>>(emptyList())
        val sessions: List<TimerSession>
            get() = storedSessions.value
        val logs = mutableListOf<TimerOperationLog>()

        override fun observeActiveSession(): Flow<TimerSession?> = flowOf(activeSession())

        override suspend fun getActiveSession(): TimerSession? {
            yield()
            return activeSession()
        }

        override suspend fun createSession(session: TimerSession) {
            yield()
            storedSessions.value = storedSessions.value + session
        }

        override suspend fun updateSession(session: TimerSession) {
            yield()
            storedSessions.value = storedSessions.value.map {
                if (it.sessionId == session.sessionId) session else it
            }
        }

        override suspend fun findSession(sessionId: String): TimerSession? {
            yield()
            return storedSessions.value.firstOrNull { it.sessionId == sessionId }
        }

        override suspend fun appendLog(log: TimerOperationLog) {
            yield()
            logs += log
        }

        override suspend fun countValidFocusSinceLastLongBreak(): Int {
            yield()
            return storedSessions.value.count {
                it.mode == TimerMode.FOCUS && it.status == TimerStatus.COMPLETED
            }
        }

        private fun activeSession(): TimerSession? {
            return storedSessions.value.firstOrNull {
                it.status == TimerStatus.RUNNING || it.status == TimerStatus.PAUSED
            }
        }
    }

    private class FakeSettingsRepository(
        private var settings: AppSettings,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = flowOf(settings)

        override suspend fun getSettings(): AppSettings {
            yield()
            return settings
        }

        override suspend fun saveSettings(settings: AppSettings) {
            yield()
            this.settings = settings
        }
    }

    private class FakeAlertManager : AlertManager {
        val finishedModes = mutableListOf<TimerMode>()

        override suspend fun notifyFinish(mode: TimerMode) {
            yield()
            finishedModes += mode
        }

        override suspend fun playPreview(sound: String) = Unit
    }

    private class RecordingFocusCompletedEventSink : FocusCompletedEventSink {
        val events = mutableListOf<FocusCompletedEvent>()

        override suspend fun emit(event: FocusCompletedEvent) {
            yield()
            events += event
        }
    }

    private class FinishableTimerEngine : TimerEngine {
        private var scope: CoroutineScope? = null
        private var job: Job? = null
        private var onTick: (suspend (Int) -> Unit)? = null
        private var onFinish: (suspend () -> Unit)? = null

        override fun start(
            scope: CoroutineScope,
            session: TimerSession,
            onTick: suspend (remainingSec: Int) -> Unit,
            onFinish: suspend () -> Unit,
        ) {
            stop()
            this.scope = scope
            this.onTick = onTick
            this.onFinish = onFinish
        }

        override fun stop() {
            job?.cancel()
            job = null
        }

        fun finishByTick(remainingSec: Int) {
            val engineScope = scope ?: error("Timer engine has not started")
            val tickCallback = onTick ?: error("Timer tick callback has not been set")
            val finishCallback = onFinish ?: error("Timer finish callback has not been set")
            job = engineScope.launch {
                tickCallback(remainingSec)
                finishCallback()
            }
        }
    }

    class MainDispatcherRule(
        private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
