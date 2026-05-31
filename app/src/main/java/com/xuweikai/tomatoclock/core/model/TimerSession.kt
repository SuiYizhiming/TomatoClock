package com.xuweikai.tomatoclock.core.model

data class TimerSession(
    val sessionId: String,
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
