package com.xuweikai.tomatoclock.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xuweikai.tomatoclock.core.database.entity.TimerOperationLogEntity
import com.xuweikai.tomatoclock.core.database.entity.TimerSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: TimerSessionEntity)

    @Update
    suspend fun updateSession(session: TimerSessionEntity)

    @Query(
        """
        SELECT * FROM timer_sessions
        WHERE status IN ('RUNNING', 'PAUSED')
        ORDER BY startAt DESC
        LIMIT 1
        """,
    )
    fun observeActiveSession(): Flow<TimerSessionEntity?>

    @Query(
        """
        SELECT * FROM timer_sessions
        WHERE status IN ('RUNNING', 'PAUSED')
        ORDER BY startAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveSession(): TimerSessionEntity?

    @Query("SELECT * FROM timer_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): TimerSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: TimerOperationLogEntity)

    @Query(
        """
        SELECT COUNT(*) FROM timer_sessions
        WHERE mode = 'FOCUS' AND status = 'COMPLETED'
        AND completedAt > (
            SELECT COALESCE(MAX(completedAt), 0) FROM timer_sessions
            WHERE mode = 'LONG_BREAK' AND status = 'COMPLETED'
        )
        """,
    )
    suspend fun countValidFocusSinceLastLongBreak(): Int
}
