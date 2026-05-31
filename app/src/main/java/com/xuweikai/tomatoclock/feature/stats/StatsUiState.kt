package com.xuweikai.tomatoclock.feature.stats

import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TodaySummary

data class StatsUiState(
    val todaySummary: TodaySummary? = null,
    val todayComparisonLabel: String = "与昨日持平",
    val weeklyTrend: List<DailyTrend> = emptyList(),
    val monthDays: List<MonthDayUiState> = emptyList(),
    val selectedDate: DayFocusStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class MonthDayUiState(
    val dateKey: String,
    val dayOfMonth: Int,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
    val checkInLevel: CheckInLevel,
)

enum class CheckInLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}
