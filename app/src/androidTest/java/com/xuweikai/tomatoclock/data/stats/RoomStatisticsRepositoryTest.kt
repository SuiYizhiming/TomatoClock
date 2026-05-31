package com.xuweikai.tomatoclock.data.stats

import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuweikai.tomatoclock.core.database.AppDatabase
import com.xuweikai.tomatoclock.core.database.entity.DailyFocusStatsEntity
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomStatisticsRepositoryTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomStatisticsRepository
    private var nextId = 0

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = RoomStatisticsRepository(
            database = database,
            nowProvider = { millisFor(2026, 5, 30, 12, 0) },
            idProvider = { "record-${++nextId}" },
            timeZone = utc,
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun archiveFocusIsIdempotentBySessionId() = runBlocking {
        val session = completedFocusSession(
            sessionId = "session-1",
            completedAt = millisFor(2026, 5, 30, 23, 0),
        )

        repository.archiveFocus(session)
        repository.archiveFocus(session)

        val stats = database.statisticsDao()
            .getDailyStatsInRange("2026-05-30", "2026-05-30")
            .single()
        assertEquals(1, stats.tomatoCount)
        assertEquals(1500, stats.totalFocusSeconds)
        assertEquals(1500, database.statisticsDao().observeAllTimeFocusSeconds().first())
    }

    @Test
    fun archiveFocusUsesCompletedAtForDateKey() = runBlocking {
        val session = completedFocusSession(
            sessionId = "session-midnight",
            startAt = millisFor(2026, 5, 30, 23, 50),
            completedAt = millisFor(2026, 5, 31, 0, 15),
        )

        repository.archiveFocus(session)

        assertEquals(
            emptyList<DailyFocusStatsEntity>(),
            database.statisticsDao().getDailyStatsInRange("2026-05-30", "2026-05-30"),
        )
        val stats = database.statisticsDao()
            .getDailyStatsInRange("2026-05-31", "2026-05-31")
            .single()
        assertEquals(1, stats.tomatoCount)
    }

    @Test
    fun weeklyTrendFillsMissingDatesWithZero() = runBlocking {
        database.statisticsDao().upsertDailyStats(
            DailyFocusStatsEntity(
                dateKey = "2026-05-28",
                tomatoCount = 2,
                totalFocusSeconds = 3000,
                updatedAt = 1,
            ),
        )
        database.statisticsDao().upsertDailyStats(
            DailyFocusStatsEntity(
                dateKey = "2026-05-30",
                tomatoCount = 1,
                totalFocusSeconds = 1500,
                updatedAt = 1,
            ),
        )

        val trend = repository.getWeeklyTrend("2026-05-30")

        assertEquals(7, trend.size)
        assertEquals("2026-05-24", trend.first().dateKey)
        assertEquals(0, trend.first().tomatoCount)
        assertEquals(2, trend.first { it.dateKey == "2026-05-28" }.tomatoCount)
        assertEquals(1, trend.last().tomatoCount)
    }

    @Test
    fun monthStatsReturnsEveryDateInMonth() = runBlocking {
        database.statisticsDao().upsertDailyStats(
            DailyFocusStatsEntity(
                dateKey = "2026-02-14",
                tomatoCount = 3,
                totalFocusSeconds = 4500,
                updatedAt = 1,
            ),
        )

        val month = repository.getMonthStats(2026, 2)

        assertEquals(28, month.size)
        assertEquals("2026-02-01", month.first().dateKey)
        assertEquals("2026-02-28", month.last().dateKey)
        assertEquals(3, month.first { it.dateKey == "2026-02-14" }.tomatoCount)
        assertEquals(0, month.first { it.dateKey == "2026-02-13" }.tomatoCount)
    }

    @Test
    fun clearLinkedTaskKeepsHistoryAndNullsTaskId() = runBlocking {
        repository.archiveFocus(
            completedFocusSession(
                sessionId = "session-linked",
                taskId = "task-1",
                completedAt = millisFor(2026, 5, 30, 10, 0),
            ),
        )

        repository.clearLinkedTask("task-1")

        val cursor = database.query(
            SimpleSQLiteQuery(
                "SELECT linkedTaskId, COUNT(*) FROM focus_records WHERE sessionId = ?",
                arrayOf("session-linked"),
            ),
        )
        cursor.use {
            it.moveToFirst()
            assertNull(it.getString(0))
            assertEquals(1, it.getInt(1))
        }
    }

    @Test
    fun todaySummaryReturnsZeroValuesWithoutData() = runBlocking {
        val summary = repository.observeTodaySummary().first()

        assertEquals("2026-05-30", summary.dateKey)
        assertEquals(0, summary.tomatoCount)
        assertEquals(0, summary.totalFocusSeconds)
        assertEquals(0, summary.allTimeFocusSeconds)
        assertEquals(0, summary.yesterdayTomatoDelta)
    }

    private fun completedFocusSession(
        sessionId: String,
        taskId: String? = "task-1",
        startAt: Long = millisFor(2026, 5, 30, 22, 35),
        completedAt: Long,
    ): TimerSession = TimerSession(
        sessionId = sessionId,
        taskId = taskId,
        mode = TimerMode.FOCUS,
        plannedDurationSec = 1500,
        remainingSec = 0,
        status = TimerStatus.COMPLETED,
        startAt = startAt,
        pauseAt = null,
        completedAt = completedAt,
        resetAt = null,
        invalidReason = null,
    )

    private fun millisFor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance(utc, Locale.US).apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis
}
