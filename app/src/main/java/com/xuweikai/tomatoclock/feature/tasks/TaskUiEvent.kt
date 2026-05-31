package com.xuweikai.tomatoclock.feature.tasks

enum class TaskOperationFailure {
    TASK_NOT_FOUND,
    PERSISTENCE_ERROR,
}

sealed interface TaskUiEvent {
    data class StartFocus(val taskId: String) : TaskUiEvent
    data class OperationFailed(val reason: TaskOperationFailure) : TaskUiEvent
}
