package com.xuweikai.tomatoclock.core.domain.repository

import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TodaySummary
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    suspend fun archiveFocus(session: TimerSession)
    suspend fun clearLinkedTask(taskId: String)
    fun observeTodaySummary(): Flow<TodaySummary>
    suspend fun getWeeklyTrend(endDateKey: String): List<DailyTrend>
    suspend fun getMonthStats(year: Int, month: Int): List<DayFocusStats>
}
