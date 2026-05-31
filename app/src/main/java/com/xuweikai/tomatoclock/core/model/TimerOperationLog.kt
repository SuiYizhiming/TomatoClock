package com.xuweikai.tomatoclock.core.model

data class TimerOperationLog(
    val logId: String,
    val sessionId: String,
    val operation: OperationType,
    val operatedAt: Long,
    val remainingSec: Int,
)
