package com.xuweikai.tomatoclock.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
    val sortOrder: Int,
    val priority: Int?,
    val tag: String?,
    val dueDate: Long?,
)
