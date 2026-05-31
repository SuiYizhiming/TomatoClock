package com.xuweikai.tomatoclock.data.task

import com.xuweikai.tomatoclock.core.database.dao.TaskDao
import com.xuweikai.tomatoclock.core.domain.repository.TaskRepository
import com.xuweikai.tomatoclock.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val taskDao: TaskDao,
) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks().map { tasks -> tasks.map { it.toDomain() } }
    }

    override fun observeActiveTasks(): Flow<List<Task>> {
        return taskDao.observeActiveTasks().map { tasks -> tasks.map { it.toDomain() } }
    }

    override fun observeCompletedTasks(): Flow<List<Task>> {
        return taskDao.observeCompletedTasks().map { tasks -> tasks.map { it.toDomain() } }
    }

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun findTask(id: String): Task? {
        return taskDao.findTask(id)?.toDomain()
    }
}
