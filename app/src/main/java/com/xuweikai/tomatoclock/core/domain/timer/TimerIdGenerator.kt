package com.xuweikai.tomatoclock.core.domain.timer

import java.util.UUID

interface TimerIdGenerator {
    fun nextId(): String
}

object UuidTimerIdGenerator : TimerIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}

