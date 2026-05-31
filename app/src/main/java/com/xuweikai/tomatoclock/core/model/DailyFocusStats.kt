package com.xuweikai.tomatoclock.core.model

data class DailyFocusStats(
    val dateKey: String,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
    val updatedAt: Long,
)
