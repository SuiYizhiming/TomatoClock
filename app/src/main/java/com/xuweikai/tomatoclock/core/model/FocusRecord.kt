package com.xuweikai.tomatoclock.core.model

data class FocusRecord(
    val id: String,
    val sessionId: String,
    val linkedTaskId: String?,
    val startTimestamp: Long,
    val durationSeconds: Int,
    val completedAt: Long,
    val dateKey: String,
)
