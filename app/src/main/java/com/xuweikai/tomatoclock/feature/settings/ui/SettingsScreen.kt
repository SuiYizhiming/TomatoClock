package com.xuweikai.tomatoclock.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xuweikai.tomatoclock.core.domain.settings.SettingsField
import com.xuweikai.tomatoclock.core.domain.settings.SettingsValidator
import com.xuweikai.tomatoclock.core.model.DarkMode
import com.xuweikai.tomatoclock.feature.settings.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateDuration: (SettingsField, Int) -> Unit,
    onSaveDurations: () -> Unit,
    onSelectAlertSound: (String) -> Unit,
    onPreviewAlertSound: (String) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.headlineLarge)
        }
        item {
            SettingsGroup(title = "外观") {
                DarkModeSelector(
                    selected = uiState.draftSettings.darkMode,
                    onSelect = onDarkModeChange,
                )
            }
        }
        item {
            SettingsGroup(title = "计时") {
                DurationStepper(
                    title = "专注时长",
                    value = uiState.draftSettings.focusDurationMin,
                    suffix = "分钟",
                    min = SettingsValidator.MIN_DURATION_MIN,
                    max = SettingsValidator.MAX_DURATION_MIN,
                    error = uiState.validationErrors[SettingsField.FOCUS_DURATION],
                    onChange = { onUpdateDuration(SettingsField.FOCUS_DURATION, it) },
                )
                HorizontalDivider()
                DurationStepper(
                    title = "短休息",
                    value = uiState.draftSettings.shortBreakDurationMin,
                    suffix = "分钟",
                    min = SettingsValidator.MIN_DURATION_MIN,
                    max = SettingsValidator.MAX_DURATION_MIN,
                    error = uiState.validationErrors[SettingsField.SHORT_BREAK_DURATION],
                    onChange = { onUpdateDuration(SettingsField.SHORT_BREAK_DURATION, it) },
                )
                HorizontalDivider()
                DurationStepper(
                    title = "长休息",
                    value = uiState.draftSettings.longBreakDurationMin,
                    suffix = "分钟",
                    min = SettingsValidator.MIN_DURATION_MIN,
                    max = SettingsValidator.MAX_DURATION_MIN,
                    error = uiState.validationErrors[SettingsField.LONG_BREAK_DURATION],
                    onChange = { onUpdateDuration(SettingsField.LONG_BREAK_DURATION, it) },
                )
                HorizontalDivider()
                DurationStepper(
                    title = "长休息间隔",
                    value = uiState.draftSettings.longBreakInterval,
                    suffix = "轮",
                    min = SettingsValidator.MIN_LONG_BREAK_INTERVAL,
                    max = SettingsValidator.MAX_LONG_BREAK_INTERVAL,
                    error = uiState.validationErrors[SettingsField.LONG_BREAK_INTERVAL],
                    onChange = { onUpdateDuration(SettingsField.LONG_BREAK_INTERVAL, it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSaveDurations,
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSavingDurations) "保存中…" else "保存时长")
                }
                Text(
                    text = "下一次专注周期生效",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        item {
            SettingsGroup(title = "提醒") {
                AlertSoundRow(
                    title = "Classic",
                    sound = "classic",
                    selectedSound = uiState.draftSettings.alertSound,
                    onSelectAlertSound = onSelectAlertSound,
                    onPreviewAlertSound = onPreviewAlertSound,
                )
                HorizontalDivider()
                AlertSoundRow(
                    title = "Soft",
                    sound = "soft",
                    selectedSound = uiState.draftSettings.alertSound,
                    onSelectAlertSound = onSelectAlertSound,
                    onPreviewAlertSound = onPreviewAlertSound,
                )
                HorizontalDivider()
                AlertSoundRow(
                    title = "Pulse",
                    sound = "pulse",
                    selectedSound = uiState.draftSettings.alertSound,
                    onSelectAlertSound = onSelectAlertSound,
                    onPreviewAlertSound = onPreviewAlertSound,
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("震动", style = MaterialTheme.typography.bodyLarge)
                        Text("静音时优先使用震动提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.draftSettings.vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                    )
                }
            }
        }
        if (uiState.saveError != null) {
            item {
                Text(
                    text = uiState.saveError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
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
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DurationStepper(
    title: String,
    value: Int,
    suffix: String,
    min: Int,
    max: Int,
    error: String?,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onChange(value - 1) },
                    enabled = value > min,
                ) {
                    Text("-")
                }
                Text(
                    text = "$value $suffix",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(
                    onClick = { onChange(value + 1) },
                    enabled = value < max,
                ) {
                    Text("+")
                }
            }
        }
    }
}

@Composable
private fun AlertSoundRow(
    title: String,
    sound: String,
    selectedSound: String,
    onSelectAlertSound: (String) -> Unit,
    onPreviewAlertSound: (String) -> Unit,
) {
    val selected = selectedSound == sound
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (selected) "当前音效" else "点击选择",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        IconButton(onClick = { onPreviewAlertSound(sound) }) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "试听",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            modifier = Modifier.width(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                TextButton(
                    onClick = { onSelectAlertSound(sound) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("选择", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DarkModeSelector(
    selected: DarkMode,
    onSelect: (DarkMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DarkModeChip(
            label = "跟随系统",
            selected = selected == DarkMode.SYSTEM,
            onClick = { onSelect(DarkMode.SYSTEM) },
            modifier = Modifier.weight(1f),
        )
        DarkModeChip(
            label = "浅色",
            selected = selected == DarkMode.LIGHT,
            onClick = { onSelect(DarkMode.LIGHT) },
            modifier = Modifier.weight(1f),
        )
        DarkModeChip(
            label = "深色",
            selected = selected == DarkMode.DARK,
            onClick = { onSelect(DarkMode.DARK) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DarkModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
