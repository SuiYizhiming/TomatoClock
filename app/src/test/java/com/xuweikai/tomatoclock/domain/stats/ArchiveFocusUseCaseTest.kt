package com.xuweikai.tomatoclock.domain.stats

import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import com.xuweikai.tomatoclock.core.model.TodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveFocusUseCaseTest {
    private val repository = FakeStatisticsRepository()
    private val useCase = ArchiveFocusUseCase(repository)

    @Test
    fun executeArchivesCompletedFocusSession() = runBlocking {
        val session = timerSession(
            mode = TimerMode.FOCUS,
            status = TimerStatus.COMPLETED,
            completedAt = 1_800_000_000_000,
        )

        useCase.execute(session)

        assertEquals(listOf(session), repository.archivedSessions)
    }

    @Test
    fun executeIgnoresNonCompletedFocusSessions() = runBlocking {
        useCase.execute(timerSession(status = TimerStatus.RUNNING))
        useCase.execute(timerSession(mode = TimerMode.SHORT_BREAK))
        useCase.execute(timerSession(completedAt = null))

        assertEquals(emptyList<TimerSession>(), repository.archivedSessions)
    }

    private fun timerSession(
        mode: TimerMode = TimerMode.FOCUS,
        status: TimerStatus = TimerStatus.COMPLETED,
        completedAt: Long? = 1_800_000_000_000,
    ): TimerSession = TimerSession(
        sessionId = "session-1",
        taskId = "task-1",
        mode = mode,
        plannedDurationSec = 1500,
        remainingSec = 0,
        status = status,
        startAt = 1_799_999_998_500,
        pauseAt = null,
        completedAt = completedAt,
        resetAt = null,
        invalidReason = null,
    )

    private class FakeStatisticsRepository : StatisticsRepository {
        val archivedSessions = mutableListOf<TimerSession>()

        override suspend fun archiveFocus(session: TimerSession) {
            archivedSessions += session
        }

        override suspend fun clearLinkedTask(taskId: String) = Unit

        override fun observeTodaySummary(): Flow<TodaySummary> = emptyFlow()

        override suspend fun getWeeklyTrend(endDateKey: String): List<DailyTrend> = emptyList()

        override suspend fun getMonthStats(year: Int, month: Int): List<DayFocusStats> = emptyList()
    }
}
