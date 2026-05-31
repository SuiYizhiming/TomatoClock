package com.xuweikai.tomatoclock.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xuweikai.tomatoclock.core.database.converter.TimerConverters
import com.xuweikai.tomatoclock.core.database.dao.StatisticsDao
import com.xuweikai.tomatoclock.core.database.dao.TaskDao
import com.xuweikai.tomatoclock.core.database.dao.TimerDao
import com.xuweikai.tomatoclock.core.database.entity.DailyFocusStatsEntity
import com.xuweikai.tomatoclock.core.database.entity.FocusRecordEntity
import com.xuweikai.tomatoclock.core.database.entity.TaskEntity
import com.xuweikai.tomatoclock.core.database.entity.TimerOperationLogEntity
import com.xuweikai.tomatoclock.core.database.entity.TimerSessionEntity

@Database(
    entities = [
        TimerSessionEntity::class,
        TimerOperationLogEntity::class,
        TaskEntity::class,
        FocusRecordEntity::class,
        DailyFocusStatsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(TimerConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun taskDao(): TaskDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        const val DATABASE_NAME = "tomato_clock.db"
    }
}
