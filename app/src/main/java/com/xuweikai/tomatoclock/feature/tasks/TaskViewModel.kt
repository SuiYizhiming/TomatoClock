package com.xuweikai.tomatoclock.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.domain.repository.TaskRepository
import com.xuweikai.tomatoclock.core.model.Task
import com.xuweikai.tomatoclock.domain.task.TaskTitleValidation
import com.xuweikai.tomatoclock.domain.task.ValidateTaskUseCase
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val statisticsRepository: StatisticsRepository,
    private val validateTaskUseCase: ValidateTaskUseCase = ValidateTaskUseCase(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState

    private val _events = MutableSharedFlow<TaskUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TaskUiEvent> = _events

    init {
        scope.launch {
            combine(
                taskRepository.observeActiveTasks(),
                taskRepository.observeCompletedTasks(),
            ) { activeTasks, completedTasks ->
                activeTasks to completedTasks
            }.collect { (activeTasks, completedTasks) ->
                _uiState.update { current ->
                    current.copy(
                        activeTasks = activeTasks,
                        completedTasks = completedTasks,
                        completedCount = completedTasks.size,
                        pendingCompletionConfirmation = current
                            .pendingCompletionConfirmation
                            ?.takeIf { pending ->
                                activeTasks.any { it.id == pending.id && !it.isCompleted }
                            },
                    )
                }
            }
        }
    }

    fun addTask(title: String) {
        val validation = validateTaskUseCase.validateTitle(title)
        if (!validation.isValid) {
            updateValidationError(validation)
            return
        }

        launchOperation {
            val now = clock()
            taskRepository.addTask(
                Task(
                    id = idGenerator(),
                    title = validation.normalizedTitle,
                    isCompleted = false,
                    createdAt = now,
                    completedAt = null,
                    sortOrder = nextTopSortOrder(),
                    priority = null,
                    tag = null,
                    dueDate = null,
                ),
            )
            clearValidationError()
        }
    }

    fun updateTitle(id: String, title: String) {
        val validation = validateTaskUseCase.validateTitle(title)
        if (!validation.isValid) {
            updateValidationError(validation)
            return
        }

        launchOperation {
            val task = taskRepository.findTask(id)
            if (task == null) {
                emitFailure(TaskOperationFailure.TASK_NOT_FOUND)
                return@launchOperation
            }

            taskRepository.updateTask(task.copy(title = validation.normalizedTitle))
            clearValidationError()
        }
    }

    fun deleteTask(id: String) {
        launchOperation {
            statisticsRepository.clearLinkedTask(id)
            taskRepository.deleteTask(id)
            clearPendingCompletion(id)
        }
    }

    fun deleteTasks(ids: Set<String>) {
        val distinctIds = ids.filter { it.isNotBlank() }.distinct()
        if (distinctIds.isEmpty()) return

        launchOperation {
            distinctIds.forEach { id ->
                statisticsRepository.clearLinkedTask(id)
                taskRepository.deleteTask(id)
                clearPendingCompletion(id)
            }
        }
    }

    fun completeTask(id: String) {
        launchOperation {
            completeTaskIfNeeded(id)
        }
    }

    fun focusTask(id: String) {
        launchOperation(markBusy = false) {
            val task = taskRepository.findTask(id)
            if (task == null) {
                emitFailure(TaskOperationFailure.TASK_NOT_FOUND)
                return@launchOperation
            }

            _events.emit(TaskUiEvent.StartFocus(task.id))
        }
    }

    fun onFocusCompleted(taskId: String?) {
        if (taskId == null) return

        launchOperation(markBusy = false) {
            val task = taskRepository.findTask(taskId)
            if (task == null || task.isCompleted) return@launchOperation

            _uiState.update {
                it.copy(pendingCompletionConfirmation = task)
            }
        }
    }

    fun confirmFocusCompletedTask() {
        val taskId = uiState.value.pendingCompletionConfirmation?.id ?: return

        launchOperation {
            completeTaskIfNeeded(taskId)
            clearPendingCompletion(taskId)
        }
    }

    fun dismissFocusCompletedTask() {
        _uiState.update { it.copy(pendingCompletionConfirmation = null) }
    }

    fun setCompletedExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isCompletedExpanded = expanded) }
    }

    fun clearTitleError() {
        clearValidationError()
    }

    private suspend fun completeTaskIfNeeded(id: String) {
        val task = taskRepository.findTask(id)
        if (task == null) {
            emitFailure(TaskOperationFailure.TASK_NOT_FOUND)
            return
        }
        if (task.isCompleted) return

        taskRepository.updateTask(
            task.copy(
                isCompleted = true,
                completedAt = clock(),
            ),
        )
    }

    private suspend fun nextTopSortOrder(): Int {
        val currentTop = taskRepository
            .observeActiveTasks()
            .first()
            .minOfOrNull { it.sortOrder }

        return if (currentTop == null) 0 else currentTop - 1
    }

    private fun updateValidationError(validation: TaskTitleValidation) {
        _uiState.update { it.copy(titleError = validation.error) }
    }

    private fun clearValidationError() {
        _uiState.update { it.copy(titleError = null) }
    }

    private fun clearPendingCompletion(taskId: String) {
        _uiState.update { current ->
            if (current.pendingCompletionConfirmation?.id == taskId) {
                current.copy(pendingCompletionConfirmation = null)
            } else {
                current
            }
        }
    }

    private fun launchOperation(
        markBusy: Boolean = true,
        block: suspend () -> Unit,
    ) {
        scope.launch {
            if (markBusy) _uiState.update { it.copy(isBusy = true) }
            try {
                block()
            } catch (_: Exception) {
                emitFailure(TaskOperationFailure.PERSISTENCE_ERROR)
            } finally {
                if (markBusy) _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun emitFailure(reason: TaskOperationFailure) {
        _events.emit(TaskUiEvent.OperationFailed(reason))
    }
}
