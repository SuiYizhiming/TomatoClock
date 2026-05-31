package com.xuweikai.tomatoclock.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_records",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["linkedTaskId"]),
        Index(value = ["dateKey"]),
    ],
)
data class FocusRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val linkedTaskId: String?,
    val startTimestamp: Long,
    val durationSeconds: Int,
    val completedAt: Long,
    val dateKey: String,
)
