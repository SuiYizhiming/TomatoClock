package com.xuweikai.tomatoclock.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.TodaySummary
import com.xuweikai.tomatoclock.feature.stats.CheckInLevel
import com.xuweikai.tomatoclock.feature.stats.MonthDayUiState
import com.xuweikai.tomatoclock.feature.stats.StatsUiState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onLoadTodaySummary: () -> Unit,
    onLoadWeeklyTrend: () -> Unit,
    onLoadMonthStats: (Int, Int) -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDateDetailVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        onLoadTodaySummary()
        onLoadWeeklyTrend()
        onLoadMonthStats(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("统计", style = MaterialTheme.typography.headlineLarge)
        }
        item {
            TodaySummaryCard(
                summary = uiState.todaySummary,
                comparisonLabel = uiState.todayComparisonLabel,
            )
        }
        item {
            WeeklyTrendCard(trend = uiState.weeklyTrend)
        }
        item {
            MonthCalendarCard(
                monthDays = uiState.monthDays,
                onSelectDate = { dateKey ->
                    isDateDetailVisible = true
                    onSelectDate(dateKey)
                },
            )
        }
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (isDateDetailVisible && uiState.selectedDate != null) {
        ModalBottomSheet(onDismissRequest = { isDateDetailVisible = false }) {
            val selectedDate = uiState.selectedDate
            DateDetailSheet(selectedDate = selectedDate)
        }
    }
}

@Composable
private fun TodaySummaryCard(
    summary: TodaySummary?,
    comparisonLabel: String,
) {
    StatsCard(title = "今日概览") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryMetric(
                label = "番茄",
                value = "${summary?.tomatoCount ?: 0}",
            )
            SummaryMetric(
                label = "今日专注",
                value = (summary?.totalFocusSeconds ?: 0).formatDuration(),
            )
            SummaryMetric(
                label = "累计",
                value = (summary?.allTimeFocusSeconds ?: 0).formatDuration(),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = comparisonLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WeeklyTrendCard(trend: List<DailyTrend>) {
    StatsCard(title = "最近 7 天") {
        val normalizedTrend = trend.ifEmpty {
            List(7) { DailyTrend(dateKey = "", tomatoCount = 0, totalFocusSeconds = 0) }
        }
        WeeklyBarChart(trend = normalizedTrend)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            normalizedTrend.forEach { day ->
                Text(
                    text = day.dateKey.shortDateLabel(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(trend: List<DailyTrend>) {
    val maxTomatoes = trend.maxOfOrNull { it.tomatoCount }?.coerceAtLeast(1) ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val barGap = 10.dp.toPx()
        val barWidth = (size.width - barGap * (trend.size - 1)) / trend.size
        trend.forEachIndexed { index, day ->
            val ratio = day.tomatoCount.toFloat() / maxTomatoes.toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(if (day.tomatoCount > 0) 8.dp.toPx() else 2.dp.toPx())
            val left = index * (barWidth + barGap)
            val color = if (index == trend.lastIndex) primaryColor else surfaceVariantColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MonthCalendarCard(
    monthDays: List<MonthDayUiState>,
    onSelectDate: (String) -> Unit,
) {
    StatsCard(title = "月度打卡") {
        if (monthDays.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无打卡记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(260.dp),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(monthDays, key = { it.dateKey }) { day ->
                    DayCell(day = day, onClick = { onSelectDate(day.dateKey) })
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: MonthDayUiState,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(day.checkInLevel.color(), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = if (day.checkInLevel == CheckInLevel.NONE) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DateDetailSheet(selectedDate: DayFocusStats) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(selectedDate.dateKey, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text("番茄数：${selectedDate.tomatoCount}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("专注时长：${selectedDate.totalFocusSeconds.formatDuration()}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

private fun Int.formatDuration(): String {
    val minutes = this.coerceAtLeast(0) / 60
    return when {
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun String.shortDateLabel(): String {
    if (length < 10) return "--"
    return substring(5).replace("-", "/")
}

@Composable
private fun CheckInLevel.color(): Color = when (this) {
    CheckInLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
    CheckInLevel.LOW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.46f)
    CheckInLevel.MEDIUM -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
    CheckInLevel.HIGH -> MaterialTheme.colorScheme.secondary
}
