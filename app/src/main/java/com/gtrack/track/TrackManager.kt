package com.gtrack.track

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestionnaire de circuit : détection SF/A, 3 secteurs, comparaison Ghost et delta chrono.
 */
class TrackManager(
    private val geometry: TrackGeometry = TrackGeometry.defaultSample()
) {
    private val _state = MutableStateFlow(TrackState())
    val state: StateFlow<TrackState> = _state.asStateFlow()

    private var previousPosition: GeoPoint? = null
    private var lapStartMs: Long = 0L
    private var sectorStartMs: Long = 0L
    private var sessionBestSectorMs = longArrayOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
    private var sessionBestLapMs: Long? = null
    private var ghost: GhostLap? = null
    private var inLap = false

    fun onGpsUpdate(latitude: Double, longitude: Double, nowMs: Long = System.currentTimeMillis()) {
        val current = GeoPoint(latitude, longitude)
        val prev = previousPosition
        previousPosition = current

        if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.startFinishLine)) {
            onStartFinishCross(nowMs)
        }
        if (inLap) {
            when (_state.value.sectorIndex) {
                0 -> if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.sector1Line)) {
                    onSectorCross(1, nowMs)
                }
                1 -> if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.sector2Line)) {
                    onSectorCross(2, nowMs)
                }
                2 -> if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.sector3Line)) {
                    onFinalSectorMarker(nowMs)
                }
            }
        }

        if (inLap) {
            updateDelta(nowMs)
        }
    }

    private fun onStartFinishCross(nowMs: Long) {
        if (inLap) {
            val lapTime = nowMs - lapStartMs
            finalizeLap(lapTime, _state.value.lapCount.coerceAtLeast(1), nowMs)
        }
        inLap = true
        lapStartMs = nowMs
        sectorStartMs = nowMs
        _state.value = _state.value.copy(
            currentLapMs = 0L,
            currentSectorMs = 0L,
            sectorIndex = 0,
            lapCount = _state.value.lapCount + 1
        )
    }

    /** Marqueur fin du 3ᵉ secteur (tronçon ligne3 → SF, index inchangé). */
    private fun onFinalSectorMarker(nowMs: Long) {
        val sectorTime = nowMs - sectorStartMs
        if (sectorTime > 0 && sectorTime < sessionBestSectorMs[2]) {
            sessionBestSectorMs[2] = sectorTime
        }
        sectorStartMs = nowMs
        _state.value = _state.value.copy(currentSectorMs = 0L)
        updateDelta(nowMs)
    }

    private fun onSectorCross(nextSector: Int, nowMs: Long) {
        val sectorIndex = _state.value.sectorIndex.coerceIn(0, 2)
        val sectorTime = nowMs - sectorStartMs
        if (sectorTime > 0 && sectorTime < sessionBestSectorMs[sectorIndex]) {
            sessionBestSectorMs[sectorIndex] = sectorTime
        }
        sectorStartMs = nowMs
        _state.value = _state.value.copy(
            sectorIndex = nextSector.coerceIn(0, 2),
            currentSectorMs = 0L
        )
        updateDelta(nowMs)
    }

    private fun finalizeLap(lapTimeMs: Long, lapNumber: Int, nowMs: Long) {
        val isNewBest = sessionBestLapMs == null || lapTimeMs < sessionBestLapMs!!
        if (isNewBest) {
            sessionBestLapMs = lapTimeMs
            ghost = GhostLap(
                lapTimeMs = lapTimeMs,
                sectorTimesMs = sessionBestSectorMs.copyOf(),
                sectorCumulativeMs = buildCumulative(sessionBestSectorMs)
            )
        }
        val newRecord = LapRecord(
            lapNumber = lapNumber,
            lapTimeMs = lapTimeMs,
            isSessionBest = isNewBest
        )
        val history = (_state.value.lapHistory + newRecord)
            .takeLast(10)
            .sortedByDescending { it.lapNumber }
            .take(3)
            .sortedByDescending { it.lapNumber }

        _state.value = _state.value.copy(
            bestLapMs = sessionBestLapMs,
            ghost = ghost,
            lapHistory = history
        )
        updateDelta(nowMs)
    }

    private fun buildCumulative(sectors: LongArray): LongArray {
        val out = LongArray(4)
        var acc = 0L
        sectors.forEachIndexed { i, t ->
            if (t != Long.MAX_VALUE) acc += t
            out[i + 1] = acc
        }
        return out
    }

    private fun updateDelta(nowMs: Long) {
        val ghostRef = ghost
        val lapElapsed = nowMs - lapStartMs
        val sectorElapsed = nowMs - sectorStartMs
        val sectorIdx = _state.value.sectorIndex

        val sectorFaster: Boolean?
        val delta: Long

        if (ghostRef == null) {
            sectorFaster = null
            delta = 0L
        } else {
            val ghostSector = ghostRef.sectorTime(sectorIdx.coerceIn(0, 2))
            val bestSector = sessionBestSectorMs[sectorIdx.coerceIn(0, 2)]
            sectorFaster = when {
                bestSector == Long.MAX_VALUE -> null
                sectorElapsed < bestSector -> true
                sectorElapsed > bestSector -> false
                else -> null
            }
            val ghostLapAtNow = ghostRef.cumulativeAtSectorEnd(
                (sectorIdx - 1).coerceAtLeast(-1)
            ) + if (ghostSector > 0) {
                (sectorElapsed.toDouble() / ghostSector * ghostSector).toLong()
            } else {
                sectorElapsed
            }
            delta = lapElapsed - ghostLapAtNow.coerceAtMost(ghostRef.lapTimeMs)
        }

        val sectorFlags = _state.value.sectorFaster.toMutableList()
        if (sectorIdx in 0..2) {
            sectorFlags[sectorIdx] = sectorFaster
        }

        _state.value = _state.value.copy(
            currentLapMs = lapElapsed,
            currentSectorMs = sectorElapsed,
            deltaChronoMs = delta,
            sectorFasterThanBest = sectorFaster,
            sectorFaster = sectorFlags,
            ghost = ghostRef
        )
    }

    fun resetSession() {
        previousPosition = null
        inLap = false
        sessionBestSectorMs = longArrayOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
        sessionBestLapMs = null
        ghost = null
        _state.value = TrackState()
    }
}
