package com.xuweikai.tomatoclock.core.domain.repository

import com.xuweikai.tomatoclock.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}
