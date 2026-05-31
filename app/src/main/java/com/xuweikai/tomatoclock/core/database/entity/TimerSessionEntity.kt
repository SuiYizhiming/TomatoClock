package com.xuweikai.tomatoclock.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerStatus

@Entity(tableName = "timer_sessions")
data class TimerSessionEntity(
    @PrimaryKey val sessionId: String,
    val taskId: String?,
    val mode: TimerMode,
    val plannedDurationSec: Int,
    val remainingSec: Int,
    val status: TimerStatus,
    val startAt: Long,
    val pauseAt: Long?,
    val completedAt: Long?,
    val resetAt: Long?,
    val invalidReason: String?,
)
