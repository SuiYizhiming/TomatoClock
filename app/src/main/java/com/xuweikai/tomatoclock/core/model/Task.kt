package com.xuweikai.tomatoclock.core.model

data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
    val sortOrder: Int,
    val priority: Int?,
    val tag: String?,
    val dueDate: Long?,
)
