package com.xuweikai.tomatoclock.core.model

data class AppSettings(
    val focusDurationMin: Int = 25,
    val shortBreakDurationMin: Int = 5,
    val longBreakDurationMin: Int = 15,
    val longBreakInterval: Int = 4,
    val alertSound: String = "classic",
    val vibrationEnabled: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
)
