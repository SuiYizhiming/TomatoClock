package com.xuweikai.tomatoclock.data.task

import com.xuweikai.tomatoclock.core.database.entity.TaskEntity
import com.xuweikai.tomatoclock.core.model.Task

internal fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAt = createdAt,
        completedAt = completedAt,
        sortOrder = sortOrder,
        priority = priority,
        tag = tag,
        dueDate = dueDate,
    )
}

internal fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAt = createdAt,
        completedAt = completedAt,
        sortOrder = sortOrder,
        priority = priority,
        tag = tag,
        dueDate = dueDate,
    )
}
