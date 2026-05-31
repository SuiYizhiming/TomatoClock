package com.xuweikai.tomatoclock.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuweikai.tomatoclock.core.database.entity.DailyFocusStatsEntity
import com.xuweikai.tomatoclock.core.database.entity.FocusRecordEntity
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseSchemaTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun databaseCreatesTablesAndSupportsStatsUpsert() = runBlocking {
        database.statisticsDao().upsertDailyStats(
            DailyFocusStatsEntity(
                dateKey = "2026-05-30",
                tomatoCount = 1,
                totalFocusSeconds = 1500,
                updatedAt = 1_800_000_000_000,
            ),
        )

        val stats = database.statisticsDao().getDailyStatsInRange(
            startDateKey = "2026-05-30",
            endDateKey = "2026-05-30",
        )

        assertEquals(1, stats.size)
        assertEquals("2026-05-30", stats.single().dateKey)
    }

    @Test
    fun focusRecordSessionIdIsUnique() = runBlocking {
        val firstInsert = database.statisticsDao().insertFocusRecord(
            focusRecord(id = "record-1", sessionId = "session-1"),
        )
        val duplicateInsert = database.statisticsDao().insertFocusRecord(
            focusRecord(id = "record-2", sessionId = "session-1"),
        )

        assertTrue(firstInsert > 0)
        assertEquals(-1L, duplicateInsert)
    }

    private fun focusRecord(
        id: String,
        sessionId: String,
    ): FocusRecordEntity = FocusRecordEntity(
        id = id,
        sessionId = sessionId,
        linkedTaskId = null,
        startTimestamp = 1_800_000_000_000,
        durationSeconds = 1500,
        completedAt = 1_800_000_001_500,
        dateKey = "2026-05-30",
    )
}
