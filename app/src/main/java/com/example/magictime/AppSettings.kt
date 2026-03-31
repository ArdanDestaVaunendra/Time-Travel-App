package com.example.magictime

data class AppSettings(
    var globalDelay: Long = 5000L,
    var timeJumpOffset: Int = 8,
    var timeFlowSpeed: Float = 0.85f,
    var is24HourFormat: Boolean = true,
    var dateLanguage: String = "en",

    var floatDisplayDuration: Long = 10000L,
    var isShakeTriggerEnabled: Boolean = false,
    var useRedCardBack: Boolean = false,
    var objectScale: Float = 0.91f,
    var floatTargetCardPath: String? = null,

    var stackSystem: String = "Bart Harding",
    var predictionTarget: String = "BOTH",
    var predictionDuration: Long = 10000L,
    var predictionLanguage: String = "en",

    var showOperator: Boolean = true,
    var showRunningText: Boolean = true,
    var operatorText: String = "TELKOMSEL•Emergency calls only",
    var marqueeText: String = "Hello, Welcome to my device",
    var networkMode: String = "SIM1_5G_ON",
    var wallpaperPath: String? = null,
    var isPinEnabled: Boolean = true,
    var isVolumeTriggerForTime: Boolean = false,

    var activeRoutines: Set<String> = emptySet(),
    var currentStatusMode: String = "PRESET"
)