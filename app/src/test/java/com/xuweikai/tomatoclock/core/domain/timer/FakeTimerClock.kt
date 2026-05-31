package com.xuweikai.tomatoclock.core.domain.timer

internal class FakeTimerClock(
    private var elapsedMillis: Long = 0L,
    private var wallMillis: Long = 1_800_000_000_000L,
) : TimerClock {
    override fun elapsedRealtimeMillis(): Long = elapsedMillis

    override fun wallTimeMillis(): Long = wallMillis

    fun advance(millis: Long) {
        elapsedMillis += millis
        wallMillis += millis
    }
}

internal class SequenceTimerIdGenerator : TimerIdGenerator {
    private var index = 0

    override fun nextId(): String {
        index += 1
        return "id-$index"
    }
}

