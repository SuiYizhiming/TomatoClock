package com.xuweikai.tomatoclock.domain.task

class ValidateTaskUseCase {
    fun validateTitle(title: String): TaskTitleValidation {
        val normalizedTitle = title.trim()
        val error = when {
            normalizedTitle.isEmpty() -> TaskTitleValidationError.EMPTY
            normalizedTitle.length > MAX_TITLE_LENGTH -> TaskTitleValidationError.TOO_LONG
            normalizedTitle.any { it.isUnsupportedTitleCharacter() } -> {
                TaskTitleValidationError.UNSUPPORTED_CHARACTER
            }
            else -> null
        }

        return TaskTitleValidation(
            normalizedTitle = normalizedTitle,
            error = error,
        )
    }

    private fun Char.isUnsupportedTitleCharacter(): Boolean {
        return isISOControl()
    }

    companion object {
        const val MAX_TITLE_LENGTH = 100
    }
}
