package com.xuweikai.tomatoclock.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val throwable: Throwable) : AppResult<Nothing>
}
