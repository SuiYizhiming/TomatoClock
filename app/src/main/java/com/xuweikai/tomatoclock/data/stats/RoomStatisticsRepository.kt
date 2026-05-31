package com.xuweikai.tomatoclock.data.stats

import androidx.room.withTransaction
import com.xuweikai.tomatoclock.core.database.AppDatabase
import com.xuweikai.tomatoclock.core.database.entity.DailyFocusStatsEntity
import com.xuweikai.tomatoclock.core.database.entity.FocusRecordEntity
import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import com.xuweikai.tomatoclock.core.model.TodaySummary
import com.xuweikai.tomatoclock.domain.stats.DateKeys
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomStatisticsRepository(
    private val database: AppDatabase,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : StatisticsRepository {
    private val statisticsDao = database.statisticsDao()

    override suspend fun archiveFocus(session: TimerSession) {
        val completedAt = session.completedAt ?: return
        if (session.mode != TimerMode.FOCUS || session.status != TimerStatus.COMPLETED) return

        val durationSeconds = session.plannedDurationSec
        if (durationSeconds <= 0) return

        val dateKey = DateKeys.fromMillis(completedAt, timeZone)
        database.withTransaction {
            val insertedRowId = statisticsDao.insertFocusRecord(
                FocusRecordEntity(
                    id = idProvider(),
                    sessionId = session.sessionId,
                    linkedTaskId = session.taskId,
                    startTimestamp = session.startAt,
                    durationSeconds = durationSeconds,
                    completedAt = completedAt,
                    dateKey = dateKey,
                ),
            )

            if (insertedRowId == -1L) return@withTransaction

            val current = statisticsDao.getDailyStatsInRange(dateKey, dateKey).firstOrNull()
            statisticsDao.upsertDailyStats(
                DailyFocusStatsEntity(
                    dateKey = dateKey,
                    tomatoCount = (current?.tomatoCount ?: 0) + 1,
                    totalFocusSeconds = (current?.totalFocusSeconds ?: 0) + durationSeconds,
                    updatedAt = nowProvider(),
                ),
            )
        }
    }

    override suspend fun clearLinkedTask(taskId: String) {
        statisticsDao.clearLinkedTask(taskId)
    }

    override fun observeTodaySummary(): Flow<TodaySummary> {
        val todayKey = DateKeys.today(nowProvider(), timeZone)
        val yesterdayKey = DateKeys.plusDays(todayKey, -1, timeZone)

        return combine(
            statisticsDao.observeDailyStats(todayKey),
            statisticsDao.observeDailyStats(yesterdayKey),
            statisticsDao.observeAllTimeFocusSeconds(),
        ) { today, yesterday, allTimeFocusSeconds ->
            val todayTomatoCount = today?.tomatoCount ?: 0
            TodaySummary(
                dateKey = todayKey,
                tomatoCount = todayTomatoCount,
                totalFocusSeconds = today?.totalFocusSeconds ?: 0,
                allTimeFocusSeconds = allTimeFocusSeconds,
                yesterdayTomatoDelta = todayTomatoCount - (yesterday?.tomatoCount ?: 0),
            )
        }
    }

    override suspend fun getWeeklyTrend(endDateKey: String): List<DailyTrend> {
        val startDateKey = DateKeys.plusDays(endDateKey, -6, timeZone)
        val storedByDate = statisticsDao.getDailyStatsInRange(startDateKey, endDateKey)
            .associateBy { it.dateKey }

        return DateKeys.daysBetweenInclusive(startDateKey, endDateKey, timeZone).map { dateKey ->
            val stats = storedByDate[dateKey]
            DailyTrend(
                dateKey = dateKey,
                tomatoCount = stats?.tomatoCount ?: 0,
                totalFocusSeconds = stats?.totalFocusSeconds ?: 0,
            )
        }
    }

    override suspend fun getMonthStats(year: Int, month: Int): List<DayFocusStats> {
        val dateKeys = DateKeys.monthDateKeys(year, month, timeZone)
        val storedByDate = statisticsDao.getDailyStatsInRange(
            startDateKey = dateKeys.first(),
            endDateKey = dateKeys.last(),
        ).associateBy { it.dateKey }

        return dateKeys.map { dateKey ->
            val stats = storedByDate[dateKey]
            DayFocusStats(
                dateKey = dateKey,
                tomatoCount = stats?.tomatoCount ?: 0,
                totalFocusSeconds = stats?.totalFocusSeconds ?: 0,
            )
        }
    }
}
