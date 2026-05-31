package com.xuweikai.tomatoclock.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTaskUseCaseTest {
    private val useCase = ValidateTaskUseCase()

    @Test
    fun blankTitleIsRejected() {
        val result = useCase.validateTitle("   ")

        assertEquals(TaskTitleValidationError.EMPTY, result.error)
    }

    @Test
    fun titleLongerThanOneHundredCharactersIsRejected() {
        val result = useCase.validateTitle("a".repeat(101))

        assertEquals(TaskTitleValidationError.TOO_LONG, result.error)
    }

    @Test
    fun commonChineseEnglishNumberAndSymbolTitleIsAcceptedAndTrimmed() {
        val result = useCase.validateTitle("  高数 Chapter 1 - 复习 20题!  ")

        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals("高数 Chapter 1 - 复习 20题!", result.normalizedTitle)
    }

    @Test
    fun controlCharactersAreRejected() {
        val result = useCase.validateTitle("read\nbook")

        assertEquals(TaskTitleValidationError.UNSUPPORTED_CHARACTER, result.error)
    }
}
