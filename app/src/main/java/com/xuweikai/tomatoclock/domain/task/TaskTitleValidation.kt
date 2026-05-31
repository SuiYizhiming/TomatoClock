package com.xuweikai.tomatoclock.domain.task

enum class TaskTitleValidationError {
    EMPTY,
    TOO_LONG,
    UNSUPPORTED_CHARACTER,
}

data class TaskTitleValidation(
    val normalizedTitle: String,
    val error: TaskTitleValidationError?,
) {
    val isValid: Boolean = error == null
}
