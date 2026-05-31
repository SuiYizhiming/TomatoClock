package com.xuweikai.tomatoclock.core.domain.repository

import com.xuweikai.tomatoclock.core.model.TimerOperationLog
import com.xuweikai.tomatoclock.core.model.TimerSession
import kotlinx.coroutines.flow.Flow

interface TimerRepository {
    fun observeActiveSession(): Flow<TimerSession?>
    suspend fun getActiveSession(): TimerSession?
    suspend fun createSession(session: TimerSession)
    suspend fun updateSession(session: TimerSession)
    suspend fun findSession(sessionId: String): TimerSession?
    suspend fun appendLog(log: TimerOperationLog)
}
