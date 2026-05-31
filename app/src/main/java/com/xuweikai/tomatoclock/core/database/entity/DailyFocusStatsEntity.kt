package com.xuweikai.tomatoclock.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_focus_stats")
data class DailyFocusStatsEntity(
    @PrimaryKey val dateKey: String,
    val tomatoCount: Int,
    val totalFocusSeconds: Int,
    val updatedAt: Long,
)
