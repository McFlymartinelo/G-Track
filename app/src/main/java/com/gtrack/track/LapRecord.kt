package com.gtrack.track

data class LapRecord(
    val lapNumber: Int,
    val lapTimeMs: Long,
    val isSessionBest: Boolean = false
)
