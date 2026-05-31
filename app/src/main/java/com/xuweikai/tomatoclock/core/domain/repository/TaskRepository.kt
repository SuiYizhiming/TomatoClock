package com.xuweikai.tomatoclock.core.domain.repository

import com.xuweikai.tomatoclock.core.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeActiveTasks(): Flow<List<Task>>
    fun observeCompletedTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun findTask(id: String): Task?
}
