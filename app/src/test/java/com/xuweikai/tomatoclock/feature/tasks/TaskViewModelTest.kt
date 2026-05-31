package com.xuweikai.tomatoclock.feature.tasks

import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.domain.repository.TaskRepository
import com.xuweikai.tomatoclock.core.model.DailyTrend
import com.xuweikai.tomatoclock.core.model.DayFocusStats
import com.xuweikai.tomatoclock.core.model.Task
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TodaySummary
import com.xuweikai.tomatoclock.domain.task.TaskTitleValidationError
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskViewModelTest {
    @Test
    fun addTaskRejectsInvalidTitle() = runBlocking {
        val fixture = taskFixture()
        fixture.viewModel.addTask(" ")
        yield()

        assertEquals(TaskTitleValidationError.EMPTY, fixture.viewModel.uiState.value.titleError)
        assertTrue(fixture.taskRepository.tasks.isEmpty())
        fixture.close()
    }

    @Test
    fun addTaskPlacesNewActiveTaskAtTop() = runBlocking {
        val existing = task(id = "old", title = "old", sortOrder = 4, createdAt = 1L)
        val fixture = taskFixture(initialTasks = listOf(existing))

        fixture.viewModel.addTask("  新任务!  ")
        yield()

        val activeTasks = fixture.viewModel.uiState.value.activeTasks
        assertEquals("新任务!", activeTasks.first().title)
        assertEquals(3, activeTasks.first().sortOrder)
        assertEquals("old", activeTasks.last().id)
        fixture.close()
    }

    @Test
    fun updateTitleOnlyChangesTitle() = runBlocking {
        val original = task(
            id = "task-1",
            title = "before",
            isCompleted = true,
            createdAt = 10L,
            completedAt = 20L,
            sortOrder = 7,
        )
        val fixture = taskFixture(initialTasks = listOf(original))

        fixture.viewModel.updateTitle("task-1", " after ")
        yield()

        val updated = fixture.taskRepository.findTask("task-1")!!
        assertEquals("after", updated.title)
        assertEquals(original.copy(title = "after"), updated)
        fixture.close()
    }

    @Test
    fun deleteTaskClearsLinkedFocusRecordsBeforeDeletingTask() = runBlocking {
        val operationLog = mutableListOf<String>()
        val fixture = taskFixture(
            initialTasks = listOf(task(id = "task-1")),
            operationLog = operationLog,
        )

        fixture.viewModel.deleteTask("task-1")
        yield()

        assertEquals(listOf("clear:task-1", "delete:task-1"), operationLog)
        assertNull(fixture.taskRepository.findTask("task-1"))
        fixture.close()
    }

    @Test
    fun completeTaskMarksTaskCompletedAndPreservesSortOrder() = runBlocking {
        val fixture = taskFixture(
            initialTasks = listOf(task(id = "task-1", sortOrder = 11)),
            clock = { 99L },
        )

        fixture.viewModel.completeTask("task-1")
        yield()

        val completed = fixture.taskRepository.findTask("task-1")!!
        assertTrue(completed.isCompleted)
        assertEquals(99L, completed.completedAt)
        assertEquals(11, completed.sortOrder)
        fixture.close()
    }

    @Test
    fun focusCompletionOnlyCompletesTaskAfterConfirmation() = runBlocking {
        val fixture = taskFixture(
            initialTasks = listOf(task(id = "task-1")),
            clock = { 500L },
        )

        fixture.viewModel.onFocusCompleted("task-1")
        yield()

        assertEquals("task-1", fixture.viewModel.uiState.value.pendingCompletionConfirmation?.id)
        assertFalse(fixture.taskRepository.findTask("task-1")!!.isCompleted)

        fixture.viewModel.confirmFocusCompletedTask()
        yield()

        val completed = fixture.taskRepository.findTask("task-1")!!
        assertTrue(completed.isCompleted)
        assertEquals(500L, completed.completedAt)
        assertNull(fixture.viewModel.uiState.value.pendingCompletionConfirmation)
        fixture.close()
    }

    @Test
    fun focusCompletionDoesNotPromptForDeletedOrAlreadyCompletedTask() = runBlocking {
        val fixture = taskFixture(
            initialTasks = listOf(task(id = "done", isCompleted = true, completedAt = 1L)),
        )

        fixture.viewModel.onFocusCompleted("missing")
        fixture.viewModel.onFocusCompleted("done")
        yield()

        assertNull(fixture.viewModel.uiState.value.pendingCompletionConfirmation)
        fixture.close()
    }

    private fun taskFixture(
        initialTasks: List<Task> = emptyList(),
        operationLog: MutableList<String> = mutableListOf(),
        clock: () -> Long = { 100L },
    ): TaskViewModelFixture {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        val taskRepository = FakeTaskRepository(initialTasks, operationLog)
        val statisticsRepository = FakeStatisticsRepository(operationLog)
        val nextId = AtomicInteger(1)
        val viewModel = TaskViewModel(
            taskRepository = taskRepository,
            statisticsRepository = statisticsRepository,
            clock = clock,
            idGenerator = { "generated-${nextId.getAndIncrement()}" },
            coroutineScope = scope,
        )

        return TaskViewModelFixture(
            viewModel = viewModel,
            taskRepository = taskRepository,
            scope = scope,
        )
    }

    private data class TaskViewModelFixture(
        val viewModel: TaskViewModel,
        val taskRepository: FakeTaskRepository,
        val scope: CoroutineScope,
    ) {
        fun close() {
            scope.cancel()
        }
    }

    private class FakeTaskRepository(
        initialTasks: List<Task>,
        private val operationLog: MutableList<String>,
    ) : TaskRepository {
        private val storedTasks = MutableStateFlow(sortTasks(initialTasks))
        val tasks: List<Task>
            get() = storedTasks.value

        override fun observeTasks(): Flow<List<Task>> {
            return storedTasks
        }

        override fun observeActiveTasks(): Flow<List<Task>> {
            return storedTasks.map { tasks ->
                tasks
                    .filter { !it.isCompleted }
                    .sortedWith(compareBy<Task> { it.sortOrder }.thenByDescending { it.createdAt })
            }
        }

        override fun observeCompletedTasks(): Flow<List<Task>> {
            return storedTasks.map { tasks ->
                tasks
                    .filter { it.isCompleted }
                    .sortedWith(compareByDescending<Task> { it.completedAt ?: Long.MIN_VALUE }
                        .thenByDescending { it.createdAt })
            }
        }

        override suspend fun addTask(task: Task) {
            storedTasks.update { sortTasks(it + task) }
        }

        override suspend fun updateTask(task: Task) {
            storedTasks.update { tasks ->
                sortTasks(tasks.map { if (it.id == task.id) task else it })
            }
        }

        override suspend fun deleteTask(id: String) {
            operationLog += "delete:$id"
            storedTasks.update { tasks -> sortTasks(tasks.filterNot { it.id == id }) }
        }

        override suspend fun findTask(id: String): Task? {
            return storedTasks.value.firstOrNull { it.id == id }
        }

        private fun sortTasks(tasks: List<Task>): List<Task> {
            return tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.sortOrder }
                    .thenByDescending { it.createdAt },
            )
        }
    }

    private class FakeStatisticsRepository(
        private val operationLog: MutableList<String>,
    ) : StatisticsRepository {
        override suspend fun archiveFocus(session: TimerSession) = Unit

        override suspend fun clearLinkedTask(taskId: String) {
            operationLog += "clear:$taskId"
        }

        override fun observeTodaySummary(): Flow<TodaySummary> {
            return flowOf(
                TodaySummary(
                    dateKey = "2026-05-30",
                    tomatoCount = 0,
                    totalFocusSeconds = 0,
                    allTimeFocusSeconds = 0,
                    yesterdayTomatoDelta = null,
                ),
            )
        }

        override suspend fun getWeeklyTrend(endDateKey: String): List<DailyTrend> = emptyList()

        override suspend fun getMonthStats(year: Int, month: Int): List<DayFocusStats> = emptyList()
    }

    private fun task(
        id: String = "task",
        title: String = "task",
        isCompleted: Boolean = false,
        createdAt: Long = 1L,
        completedAt: Long? = null,
        sortOrder: Int = 0,
    ): Task {
        return Task(
            id = id,
            title = title,
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt,
            sortOrder = sortOrder,
            priority = null,
            tag = null,
            dueDate = null,
        )
    }
}
