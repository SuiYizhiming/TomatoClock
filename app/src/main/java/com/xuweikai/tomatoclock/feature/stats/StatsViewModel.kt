package com.xuweikai.tomatoclock.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TodaySummary
import com.xuweikai.tomatoclock.domain.stats.DateKeys
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatsViewModel(
    private val statisticsRepository: StatisticsRepository,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var todaySummaryJob: Job? = null

    fun loadTodaySummary() {
        todaySummaryJob?.cancel()
        todaySummaryJob = viewModelScope.launch {
            statisticsRepository.observeTodaySummary().collect { summary ->
                _uiState.update {
                    it.copy(
                        todaySummary = summary,
                        todayComparisonLabel = summary.toComparisonLabel(),
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun loadWeeklyTrend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val endDateKey = DateKeys.today(nowProvider(), timeZone)
                statisticsRepository.getWeeklyTrend(endDateKey)
            }.onSuccess { trend ->
                _uiState.update {
                    it.copy(
                        weeklyTrend = trend,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "统计数据加载失败",
                    )
                }
            }
        }
    }

    fun loadMonthStats(year: Int, month: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                statisticsRepository.getMonthStats(year, month)
            }.onSuccess { monthStats ->
                _uiState.update {
                    it.copy(
                        monthDays = monthStats.map { stats -> stats.toMonthDayUiState() },
                        selectedDate = monthStats.firstOrNull { stats ->
                            stats.dateKey == DateKeys.today(nowProvider(), timeZone)
                        },
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "月度统计加载失败",
                    )
                }
            }
        }
    }

    fun selectDate(dateKey: String) {
        val selected = _uiState.value.monthDays.firstOrNull { it.dateKey == dateKey }
        _uiState.update {
            it.copy(
                selectedDate = selected?.let { day ->
                    DayFocusStats(
                        dateKey = day.dateKey,
                        tomatoCount = day.tomatoCount,
                        totalFocusSeconds = day.totalFocusSeconds,
                    )
                },
            )
        }
    }

    override fun onCleared() {
        todaySummaryJob?.cancel()
        super.onCleared()
    }

    private fun TodaySummary.toComparisonLabel(): String {
        val delta = yesterdayTomatoDelta ?: 0
        return when {
            delta > 0 -> "比昨日多 $delta 个番茄"
            delta < 0 -> "比昨日少 ${-delta} 个番茄"
            else -> "与昨日持平"
        }
    }

    private fun DayFocusStats.toMonthDayUiState(): MonthDayUiState =
        MonthDayUiState(
            dateKey = dateKey,
            dayOfMonth = DateKeys.dayOfMonth(dateKey),
            tomatoCount = tomatoCount,
            totalFocusSeconds = totalFocusSeconds,
            checkInLevel = tomatoCount.toCheckInLevel(),
        )

    private fun Int.toCheckInLevel(): CheckInLevel = when {
        this <= 0 -> CheckInLevel.NONE
        this == 1 -> CheckInLevel.LOW
        this in 2..3 -> CheckInLevel.MEDIUM
        else -> CheckInLevel.HIGH
    }
}
