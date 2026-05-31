package com.xuweikai.tomatoclock.core.domain.timer

import android.os.SystemClock

interface TimerClock {
    fun elapsedRealtimeMillis(): Long
    fun wallTimeMillis(): Long
}

object SystemTimerClock : TimerClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun wallTimeMillis(): Long = System.currentTimeMillis()
}

