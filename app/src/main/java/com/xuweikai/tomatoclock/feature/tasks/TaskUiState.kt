package com.xuweikai.tomatoclock.feature.tasks

import com.xuweikai.tomatoclock.core.model.Task
import com.xuweikai.tomatoclock.domain.task.TaskTitleValidationError

data class TaskUiState(
    val activeTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val isCompletedExpanded: Boolean = false,
    val pendingCompletionConfirmation: Task? = null,
    val titleError: TaskTitleValidationError? = null,
    val isBusy: Boolean = false,
)
