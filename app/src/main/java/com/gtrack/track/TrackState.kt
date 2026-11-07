package com.gtrack.track

data class TrackState(
    val currentLapMs: Long = 0L,
    val currentSectorMs: Long = 0L,
    val sectorIndex: Int = 0,
    val lapCount: Int = 0,
    val deltaChronoMs: Long = 0L,
    val sectorFasterThanBest: Boolean? = null,
    val ghost: GhostLap? = null,
    val bestLapMs: Long? = null,
    val lapHistory: List<LapRecord> = emptyList(),
    val targetLapCount: Int = 12,
    /** true = secteur plus rapide que le ghost, false = plus lent, null = inconnu */
    val sectorFaster: List<Boolean?> = listOf(null, null, null)
)
