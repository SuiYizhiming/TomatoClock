package com.xuweikai.tomatoclock.data.timer

import com.xuweikai.tomatoclock.core.database.dao.TimerDao
import com.xuweikai.tomatoclock.core.domain.repository.TimerRepository
import com.xuweikai.tomatoclock.core.model.TimerOperationLog
import com.xuweikai.tomatoclock.core.model.TimerSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTimerRepository(
    private val timerDao: TimerDao,
) : TimerRepository {
    override fun observeActiveSession(): Flow<TimerSession?> {
        return timerDao.observeActiveSession().map { it?.asModel() }
    }

    override suspend fun getActiveSession(): TimerSession? {
        return timerDao.getActiveSession()?.asModel()
    }

    override suspend fun createSession(session: TimerSession) {
        timerDao.insertSession(session.asEntity())
    }

    override suspend fun updateSession(session: TimerSession) {
        timerDao.updateSession(session.asEntity())
    }

    override suspend fun findSession(sessionId: String): TimerSession? {
        return timerDao.getSessionById(sessionId)?.asModel()
    }

    override suspend fun appendLog(log: TimerOperationLog) {
        timerDao.insertLog(log.asEntity())
    }
}

