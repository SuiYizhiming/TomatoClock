package com.xuweikai.tomatoclock.core.domain.settings

data class SettingsValidationError(
    val field: SettingsField,
    val message: String,
)
