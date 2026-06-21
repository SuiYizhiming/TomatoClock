package com.xuweikai.tomatoclock.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.settings.SettingsField
import com.xuweikai.tomatoclock.core.domain.settings.SettingsValidationError
import com.xuweikai.tomatoclock.core.domain.settings.SettingsValidator
import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.core.model.DarkMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadSettings()
    }

    fun loadSettings() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveError = null) }
            runCatching {
                settingsRepository.observeSettings().collect { settings ->
                    _uiState.update {
                        it.copy(
                            settings = settings,
                            draftSettings = settings,
                            isLoading = false,
                            validationErrors = emptyMap(),
                            saveError = null,
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        saveError = throwable.message ?: "Failed to load settings.",
                    )
                }
            }
        }
    }

    fun updateDuration(type: SettingsField, value: Int) {
        val current = _uiState.value.draftSettings
        val next = when (type) {
            SettingsField.FOCUS_DURATION -> current.copy(focusDurationMin = value)
            SettingsField.SHORT_BREAK_DURATION -> current.copy(shortBreakDurationMin = value)
            SettingsField.LONG_BREAK_DURATION -> current.copy(longBreakDurationMin = value)
            SettingsField.LONG_BREAK_INTERVAL -> current.copy(longBreakInterval = value)
            SettingsField.ALERT_SOUND -> current
        }
        updateDraft(next)
    }

    fun saveDurations() {
        val next = _uiState.value.draftSettings
        val errors = SettingsValidator.validate(next).associateByField()
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors, saveError = null) }
            return
        }
        saveSettings(next, showSavingState = true)
    }

    fun selectAlertSound(sound: String) {
        val next = _uiState.value.draftSettings.copy(alertSound = sound)
        if (SettingsValidator.isSupportedAlertSound(sound)) {
            saveSettings(next, showSavingState = false)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        val next = _uiState.value.draftSettings.copy(vibrationEnabled = enabled)
        saveSettings(next, showSavingState = false)
    }

    fun setDarkMode(mode: DarkMode) {
        val next = _uiState.value.draftSettings.copy(darkMode = mode)
        saveSettings(next, showSavingState = false)
    }

    private fun updateDraft(settings: AppSettings) {
        val errors = SettingsValidator.validate(settings).associateByField()
        _uiState.update {
            it.copy(
                draftSettings = settings,
                validationErrors = errors,
                saveError = null,
            )
        }
    }

    private fun saveSettings(settings: AppSettings, showSavingState: Boolean) {
        val errors = SettingsValidator.validate(settings).associateByField()
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(draftSettings = settings, validationErrors = errors, saveError = null) }
            return
        }

        // Only show saving state for duration saves to avoid button flicker.
        if (showSavingState) {
            _uiState.update { it.copy(draftSettings = settings, isSavingDurations = true, saveError = null) }
        } else {
            _uiState.update { it.copy(draftSettings = settings, saveError = null) }
        }
        viewModelScope.launch {
            runCatching {
                settingsRepository.saveSettings(settings)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isSavingDurations = false,
                        validationErrors = emptyMap(),
                        saveError = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSavingDurations = false,
                        saveError = throwable.message ?: "Failed to save settings.",
                    )
                }
            }
        }
    }

    private fun List<SettingsValidationError>.associateByField(): Map<SettingsField, String> {
        return associate { it.field to it.message }
    }
}
