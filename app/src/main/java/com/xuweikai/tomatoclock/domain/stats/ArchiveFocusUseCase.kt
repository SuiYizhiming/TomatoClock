package com.xuweikai.tomatoclock.domain.stats

import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerSession
import com.xuweikai.tomatoclock.core.model.TimerStatus

class ArchiveFocusUseCase(
    private val statisticsRepository: StatisticsRepository,
) {
    suspend fun execute(session: TimerSession) {
        if (session.status != TimerStatus.COMPLETED) return
        if (session.mode != TimerMode.FOCUS) return
        if (session.completedAt == null) return

        statisticsRepository.archiveFocus(session)
    }
}
