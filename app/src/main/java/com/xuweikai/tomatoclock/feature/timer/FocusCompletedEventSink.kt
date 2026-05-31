package com.xuweikai.tomatoclock.feature.timer

import com.xuweikai.tomatoclock.core.domain.event.FocusCompletedEvent

fun interface FocusCompletedEventSink {
    suspend fun emit(event: FocusCompletedEvent)
}

object NoopFocusCompletedEventSink : FocusCompletedEventSink {
    override suspend fun emit(event: FocusCompletedEvent) = Unit
}

