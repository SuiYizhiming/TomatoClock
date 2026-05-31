package com.xuweikai.tomatoclock.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xuweikai.tomatoclock.core.model.Task
import com.xuweikai.tomatoclock.domain.task.TaskTitleValidationError
import com.xuweikai.tomatoclock.feature.tasks.TaskUiState
import com.xuweikai.tomatoclock.ui.theme.Danger
import com.xuweikai.tomatoclock.ui.theme.PageBackground
import com.xuweikai.tomatoclock.ui.theme.Success
import com.xuweikai.tomatoclock.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    uiState: TaskUiState,
    onAddTask: (String) -> Unit,
    onUpdateTask: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onFocusTask: (String) -> Unit,
    onCompletedExpandedChange: (Boolean) -> Unit,
    onConfirmFocusCompletedTask: () -> Unit,
    onDismissFocusCompletedTask: () -> Unit,
    onClearTitleError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAdding by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }
    var activeTaskIdsBeforeAdd by remember { mutableStateOf<Set<String>?>(null) }
    var pendingEditTaskId by remember { mutableStateOf<String?>(null) }
    var pendingEditTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.activeTasks, uiState.completedTasks, uiState.titleError) {
        val taskIdsBeforeAdd = activeTaskIdsBeforeAdd
        if (taskIdsBeforeAdd != null) {
            val activeTaskIds = uiState.activeTasks.mapTo(mutableSetOf()) { it.id }
            if (uiState.titleError == null && activeTaskIds.any { it !in taskIdsBeforeAdd }) {
                newTitle = ""
                isAdding = false
                activeTaskIdsBeforeAdd = null
            } else if (uiState.titleError != null) {
                activeTaskIdsBeforeAdd = null
            }
        }

        val editTaskId = pendingEditTaskId
        val editTitle = pendingEditTitle
        if (editTaskId != null && editTitle != null) {
            val updatedTask = (uiState.activeTasks + uiState.completedTasks)
                .firstOrNull { it.id == editTaskId }
            if (uiState.titleError == null && updatedTask?.title == editTitle) {
                editingTask = null
                pendingEditTaskId = null
                pendingEditTitle = null
            } else if (uiState.titleError != null) {
                pendingEditTaskId = null
                pendingEditTitle = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "任务",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            if (isAdding) {
                item {
                    InlineTaskInput(
                        value = newTitle,
                        error = uiState.titleError,
                        onValueChange = {
                            newTitle = it
                            onClearTitleError()
                        },
                        onSave = {
                            activeTaskIdsBeforeAdd = uiState.activeTasks.mapTo(mutableSetOf()) { it.id }
                            onAddTask(newTitle)
                        },
                        onCancel = {
                            newTitle = ""
                            isAdding = false
                            onClearTitleError()
                        },
                    )
                }
            }
            item {
                SectionHeader(
                    title = "未完成",
                    count = uiState.activeTasks.size,
                    expanded = true,
                    onClick = {},
                )
            }
            if (uiState.activeTasks.isEmpty()) {
                item { EmptyTaskState(text = "还没有任务，添加一个开始专注。") }
            } else {
                items(uiState.activeTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onComplete = { onCompleteTask(task.id) },
                        onFocus = { onFocusTask(task.id) },
                        onEdit = { editingTask = task },
                        onDelete = { deletingTask = task },
                    )
                }
            }
            item {
                SectionHeader(
                    title = "已完成",
                    count = uiState.completedCount,
                    expanded = uiState.isCompletedExpanded,
                    onClick = { onCompletedExpandedChange(!uiState.isCompletedExpanded) },
                )
            }
            if (uiState.isCompletedExpanded) {
                if (uiState.completedTasks.isEmpty()) {
                    item { EmptyTaskState(text = "完成的任务会显示在这里。") }
                } else {
                    items(uiState.completedTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onComplete = {},
                            onFocus = { onFocusTask(task.id) },
                            onEdit = { editingTask = task },
                            onDelete = { deletingTask = task },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isAdding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineLarge)
        }
    }

    editingTask?.let { task ->
        var editTitle by remember(task.id) { mutableStateOf(task.title) }
        ModalBottomSheet(onDismissRequest = { editingTask = null }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("编辑任务", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = {
                        editTitle = it
                        onClearTitleError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.titleError != null,
                    supportingText = { TaskErrorText(uiState.titleError) },
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { editingTask = null },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            pendingEditTaskId = task.id
                            pendingEditTitle = editTitle.trim()
                            onUpdateTask(task.id, editTitle)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    deletingTask?.let { task ->
        ModalBottomSheet(onDismissRequest = { deletingTask = null }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("删除任务？", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { deletingTask = null },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onDeleteTask(task.id)
                            deletingTask = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("删除")
                    }
                }
            }
        }
    }

    uiState.pendingCompletionConfirmation?.let {
        ModalBottomSheet(onDismissRequest = onDismissFocusCompletedTask) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("恭喜完成任务？", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(it.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismissFocusCompletedTask,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("稍后")
                    }
                    Button(
                        onClick = onConfirmFocusCompletedTask,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("标记完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineTaskInput(
    value: String,
    error: TaskTitleValidationError?,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("任务标题") },
                isError = error != null,
                supportingText = { TaskErrorText(error) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                Button(onClick = onSave) { Text("保存") }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onComplete: () -> Unit,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { if (!task.isCompleted) onComplete() },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.title,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.isCompleted) TextSecondary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            )
            TextButton(onClick = onFocus) {
                Text("专注", color = Success)
            }
            TextButton(onClick = onDelete) {
                Text("删除", color = Danger)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "$count",
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (expanded) "收起" else "展开", color = TextSecondary)
    }
}

@Composable
private fun EmptyTaskState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = TextSecondary)
    }
}

@Composable
private fun TaskErrorText(error: TaskTitleValidationError?) {
    if (error != null) {
        Text(
            text = when (error) {
                TaskTitleValidationError.EMPTY -> "标题不能为空"
                TaskTitleValidationError.TOO_LONG -> "标题不能超过 100 个字符"
                TaskTitleValidationError.UNSUPPORTED_CHARACTER -> "标题包含不支持的字符"
            },
        )
    }
}
