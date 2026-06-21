package com.xuweikai.tomatoclock.feature.timer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerStatus
import com.xuweikai.tomatoclock.feature.timer.TimerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onPrimaryAction: () -> Unit,
    onConfirmReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = uiState.mode.label(),
                color = contentColor.copy(alpha = 0.74f),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(28.dp))
            TimerProgressRing(
                remainingSec = uiState.remainingSec,
                plannedDurationSec = uiState.plannedDurationSec,
                foregroundColor = MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.14f),
            ) {
                Text(
                    text = uiState.remainingSec.formatTime(),
                    color = contentColor,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(34.dp))
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
            ) {
                Text(text = uiState.primaryActionLabel(), fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.status == TimerStatus.RUNNING || uiState.status == TimerStatus.PAUSED) {
                TextButton(onClick = { showResetConfirmation = true }) {
                    Text(text = "重置", color = contentColor.copy(alpha = 0.82f))
                }
            }
        }
    }

    if (showResetConfirmation) {
        ModalBottomSheet(onDismissRequest = { showResetConfirmation = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = "确认重置计时？",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前专注不会计入统计，计时记录将标记为无效。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showResetConfirmation = false },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            showResetConfirmation = false
                            onConfirmReset()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("确认重置")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerProgressRing(
    remainingSec: Int,
    plannedDurationSec: Int,
    foregroundColor: Color,
    trackColor: Color,
    content: @Composable () -> Unit,
) {
    val progress = when {
        plannedDurationSec <= 0 -> 1f
        else -> (remainingSec.toFloat() / plannedDurationSec.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier.size(268.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = foregroundColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

private fun TimerMode.label(): String = when (this) {
    TimerMode.FOCUS -> "专注中"
    TimerMode.SHORT_BREAK -> "短休息"
    TimerMode.LONG_BREAK -> "长休息"
}

private fun TimerUiState.primaryActionLabel(): String = when (status) {
    TimerStatus.RUNNING -> "暂停"
    TimerStatus.PAUSED -> "继续"
    else -> "开始"
}

private fun Int.formatTime(): String {
    val normalized = coerceAtLeast(0)
    val minutes = normalized / 60
    val seconds = normalized % 60
    return "%02d:%02d".format(minutes, seconds)
}
