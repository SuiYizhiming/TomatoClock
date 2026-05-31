package com.xuweikai.tomatoclock.di

import android.content.Context
import androidx.room.Room
import com.xuweikai.tomatoclock.core.database.AppDatabase
import com.xuweikai.tomatoclock.core.domain.repository.AlertManager
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.repository.StatisticsRepository
import com.xuweikai.tomatoclock.core.domain.repository.TaskRepository
import com.xuweikai.tomatoclock.core.domain.repository.TimerRepository
import com.xuweikai.tomatoclock.data.alert.AndroidAlertManager
import com.xuweikai.tomatoclock.data.settings.DataStoreSettingsRepository
import com.xuweikai.tomatoclock.data.settings.appSettingsDataStore
import com.xuweikai.tomatoclock.data.stats.RoomStatisticsRepository
import com.xuweikai.tomatoclock.data.task.RoomTaskRepository
import com.xuweikai.tomatoclock.data.timer.RoomTimerRepository
import com.xuweikai.tomatoclock.feature.timer.FocusCompletedEventSink

interface AppContainer {
    val database: AppDatabase
    val timerRepository: TimerRepository
    val taskRepository: TaskRepository
    val statisticsRepository: StatisticsRepository
    val settingsRepository: SettingsRepository
    val alertManager: AlertManager
    val focusCompletedEventSink: FocusCompletedEventSink
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).build()
    }

    override val timerRepository: TimerRepository by lazy {
        RoomTimerRepository(database.timerDao())
    }

    override val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.taskDao())
    }

    override val statisticsRepository: StatisticsRepository by lazy {
        RoomStatisticsRepository(database)
    }

    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.appSettingsDataStore)
    }

    override val alertManager: AlertManager by lazy {
        AndroidAlertManager(appContext, settingsRepository)
    }

    override val focusCompletedEventSink: FocusCompletedEventSink by lazy {
        FocusCompletedEventSink { event ->
            val completedSession = timerRepository.findSession(event.sessionId)
            if (completedSession != null) {
                statisticsRepository.archiveFocus(completedSession)
            }
        }
    }
}
