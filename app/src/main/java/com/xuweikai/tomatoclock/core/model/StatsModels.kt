package com.xuweikai.tomatoclock.core.model

data class TodaySummary(
    val dateKey: String,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
    val allTimeFocusSeconds: Int,
    val yesterdayTomatoDelta: Int?,
)

data class DailyTrend(
    val dateKey: String,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
)

data class DayFocusStats(
    val dateKey: String,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
)
