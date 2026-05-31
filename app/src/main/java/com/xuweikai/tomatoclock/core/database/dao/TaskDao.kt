package com.xuweikai.tomatoclock.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xuweikai.tomatoclock.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0
        ORDER BY sortOrder ASC, createdAt DESC
        """,
    )
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 1
        ORDER BY completedAt DESC, createdAt DESC
        """,
    )
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, sortOrder ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findTask(id: String): TaskEntity?
}
