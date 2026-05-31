package com.xuweikai.tomatoclock.data.timer

import com.xuweikai.tomatoclock.core.database.entity.TimerOperationLogEntity
import com.xuweikai.tomatoclock.core.database.entity.TimerSessionEntity
import com.xuweikai.tomatoclock.core.model.TimerOperationLog
import com.xuweikai.tomatoclock.core.model.TimerSession

internal fun TimerSessionEntity.asModel(): TimerSession = TimerSession(
    sessionId = sessionId,
    taskId = taskId,
    mode = mode,
    plannedDurationSec = plannedDurationSec,
    remainingSec = remainingSec,
    status = status,
    startAt = startAt,
    pauseAt = pauseAt,
    completedAt = completedAt,
    resetAt = resetAt,
    invalidReason = invalidReason,
)

internal fun TimerSession.asEntity(): TimerSessionEntity = TimerSessionEntity(
    sessionId = sessionId,
    taskId = taskId,
    mode = mode,
    plannedDurationSec = plannedDurationSec,
    remainingSec = remainingSec,
    status = status,
    startAt = startAt,
    pauseAt = pauseAt,
    completedAt = completedAt,
    resetAt = resetAt,
    invalidReason = invalidReason,
)

internal fun TimerOperationLog.asEntity(): TimerOperationLogEntity = TimerOperationLogEntity(
    logId = logId,
    sessionId = sessionId,
    operation = operation,
    operatedAt = operatedAt,
    remainingSec = remainingSec,
)

