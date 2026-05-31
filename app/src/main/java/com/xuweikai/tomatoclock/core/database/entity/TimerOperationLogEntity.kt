package com.xuweikai.tomatoclock.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xuweikai.tomatoclock.core.model.OperationType

@Entity(
    tableName = "timer_operation_logs",
    foreignKeys = [
        ForeignKey(
            entity = TimerSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class TimerOperationLogEntity(
    @PrimaryKey val logId: String,
    val sessionId: String,
    val operation: OperationType,
    val operatedAt: Long,
    val remainingSec: Int,
)
