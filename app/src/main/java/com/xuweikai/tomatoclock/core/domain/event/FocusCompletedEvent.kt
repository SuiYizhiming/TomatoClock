package com.xuweikai.tomatoclock.core.domain.event

data class FocusCompletedEvent(
    val sessionId: String,
    val taskId: String?,
    val durationSeconds: Int,
    val completedAt: Long,
)
