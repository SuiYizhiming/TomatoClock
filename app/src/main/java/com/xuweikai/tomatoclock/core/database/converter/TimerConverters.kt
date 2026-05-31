package com.xuweikai.tomatoclock.core.database.converter

import androidx.room.TypeConverter
import com.xuweikai.tomatoclock.core.model.OperationType
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.TimerStatus

class TimerConverters {
    @TypeConverter
    fun timerModeToString(value: TimerMode): String = value.name

    @TypeConverter
    fun stringToTimerMode(value: String): TimerMode = TimerMode.valueOf(value)

    @TypeConverter
    fun timerStatusToString(value: TimerStatus): String = value.name

    @TypeConverter
    fun stringToTimerStatus(value: String): TimerStatus = TimerStatus.valueOf(value)

    @TypeConverter
    fun operationTypeToString(value: OperationType): String = value.name

    @TypeConverter
    fun stringToOperationType(value: String): OperationType = OperationType.valueOf(value)
}
