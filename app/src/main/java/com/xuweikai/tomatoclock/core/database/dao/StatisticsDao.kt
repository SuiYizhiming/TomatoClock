package com.xuweikai.tomatoclock.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.xuweikai.tomatoclock.core.database.entity.DailyFocusStatsEntity
import com.xuweikai.tomatoclock.core.database.entity.FocusRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFocusRecord(record: FocusRecordEntity): Long

    @Upsert
    suspend fun upsertDailyStats(stats: DailyFocusStatsEntity)

    @Query("SELECT * FROM daily_focus_stats WHERE dateKey = :dateKey LIMIT 1")
    fun observeDailyStats(dateKey: String): Flow<DailyFocusStatsEntity?>

    @Query("SELECT * FROM daily_focus_stats WHERE dateKey BETWEEN :startDateKey AND :endDateKey ORDER BY dateKey ASC")
    suspend fun getDailyStatsInRange(
        startDateKey: String,
        endDateKey: String,
    ): List<DailyFocusStatsEntity>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM focus_records")
    fun observeAllTimeFocusSeconds(): Flow<Int>

    @Query("UPDATE focus_records SET linkedTaskId = NULL WHERE linkedTaskId = :taskId")
    suspend fun clearLinkedTask(taskId: String)
}
