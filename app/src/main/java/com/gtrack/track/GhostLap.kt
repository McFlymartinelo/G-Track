package com.gtrack.track

/**
 * Meilleur tour de référence (Ghost) : temps cumulés par secteur et distance parcourue approximative.
 */
data class GhostLap(
    val lapTimeMs: Long,
    val sectorTimesMs: LongArray = longArrayOf(0L, 0L, 0L),
    val sectorCumulativeMs: LongArray = longArrayOf(0L, 0L, 0L, 0L)
) {
    fun sectorTime(sectorIndex: Int): Long =
        sectorTimesMs.getOrElse(sectorIndex) { 0L }

    fun cumulativeAtSectorEnd(sectorIndex: Int): Long =
        sectorCumulativeMs.getOrElse(sectorIndex + 1) { lapTimeMs }

    companion object {
        fun fromSectorTimes(sectorTimes: LongArray): GhostLap {
            val cumulative = LongArray(sectorTimes.size + 1)
            var acc = 0L
            sectorTimes.forEachIndexed { i, t ->
                acc += t
                cumulative[i + 1] = acc
            }
            return GhostLap(
                lapTimeMs = acc,
                sectorTimesMs = sectorTimes.copyOf(),
                sectorCumulativeMs = cumulative
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GhostLap
        return lapTimeMs == other.lapTimeMs &&
            sectorTimesMs.contentEquals(other.sectorTimesMs)
    }

    override fun hashCode(): Int = 31 * lapTimeMs.hashCode() + sectorTimesMs.contentHashCode()
}
