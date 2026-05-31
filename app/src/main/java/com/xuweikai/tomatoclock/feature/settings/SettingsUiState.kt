package com.xuweikai.tomatoclock.feature.settings

import com.xuweikai.tomatoclock.core.domain.settings.SettingsField
import com.xuweikai.tomatoclock.core.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val draftSettings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val validationErrors: Map<SettingsField, String> = emptyMap(),
    val saveError: String? = null,
) {
    val canSave: Boolean
        get() = validationErrors.isEmpty() && !isSaving
}
