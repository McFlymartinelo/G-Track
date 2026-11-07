package com.gtrack.track

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestionnaire de circuit : détection SF/secteurs, comparaison Ghost et delta chrono.
 *
 * Convention de secteurs (3 secteurs qui couvrent le tour) :
 *  - secteur 1 : SF -> S1
 *  - secteur 2 : S1 -> S2
 *  - secteur 3 : S2 -> SF
 *
 * La ligne `sector3Line` reste disponible pour l'affichage cartographique,
 * mais elle ne découpe pas le chronométrage : sinon le dernier tronçon
 * (S3 -> SF) n'est jamais compté et le ghost ne correspond pas au tour réel.
 */
class TrackManager(
    private val geometry: TrackGeometry = TrackGeometry.defaultSample()
) {
    companion object {
        /** Garde-fou anti-rebond GPS : un tour ne peut pas durer moins de 15 s. */
        private const val MIN_LAP_MS = 15_000L
    }

    private val _state = MutableStateFlow(TrackState())
    val state: StateFlow<TrackState> = _state.asStateFlow()

    private var previousPosition: GeoPoint? = null
    private var lapStartMs = 0L
    private var sectorStartMs = 0L
    private val sessionBestSectorMs = longArrayOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
    private val currentLapSectorMs = LongArray(3)
    private var sessionBestLapMs: Long? = null
    private var sessionBestLapNumber: Int? = null
    private var ghost: GhostLap? = null
    private var inLap = false

    fun onGpsUpdate(latitude: Double, longitude: Double, nowMs: Long = System.currentTimeMillis()) {
        val current = GeoPoint(latitude, longitude)
        val prev = previousPosition
        previousPosition = current

        if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.startFinishLine)) {
            onStartFinishCross(nowMs)
        } else if (inLap) {
            when (_state.value.sectorIndex) {
                0 -> if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.sector1Line)) {
                    onSectorCross(1, nowMs)
                }
                1 -> if (GeoFenceDetector.hasCrossedLine(prev, current, geometry.sector2Line)) {
                    onSectorCross(2, nowMs)
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
            if (lapTime < MIN_LAP_MS) return

            val finalSector = nowMs - sectorStartMs
            if (finalSector > 0) {
                currentLapSectorMs[2] = finalSector
                recordSectorFlag(2, finalSector)
            }
            finalizeLap(lapTime, _state.value.lapCount.coerceAtLeast(1))
        }

        inLap = true
        lapStartMs = nowMs
        sectorStartMs = nowMs
        currentLapSectorMs.fill(0L)
        _state.value = _state.value.copy(
            currentLapMs = 0L,
            currentSectorMs = 0L,
            sectorIndex = 0,
            lapCount = _state.value.lapCount + 1,
            sectorFaster = listOf(null, null, null),
            sectorFasterThanBest = null
        )
    }

    private fun onSectorCross(nextSector: Int, nowMs: Long) {
        val sectorIndex = _state.value.sectorIndex.coerceIn(0, 2)
        val sectorTime = nowMs - sectorStartMs
        if (sectorTime <= 0) return

        currentLapSectorMs[sectorIndex] = sectorTime
        if (sectorTime < sessionBestSectorMs[sectorIndex]) {
            sessionBestSectorMs[sectorIndex] = sectorTime
        }
        recordSectorFlag(sectorIndex, sectorTime)

        sectorStartMs = nowMs
        _state.value = _state.value.copy(
            sectorIndex = nextSector.coerceIn(0, 2),
            currentSectorMs = 0L
        )
        updateDelta(nowMs)
    }

    private fun recordSectorFlag(sectorIndex: Int, sectorTimeMs: Long) {
        val ghostSector = ghost?.sectorTime(sectorIndex) ?: 0L
        val faster = if (ghostSector > 0) sectorTimeMs < ghostSector else null
        val flags = _state.value.sectorFaster.toMutableList()
        while (flags.size < 3) flags.add(null)
        flags[sectorIndex.coerceIn(0, 2)] = faster
        _state.value = _state.value.copy(
            sectorFaster = flags,
            sectorFasterThanBest = faster
        )
    }

    private fun finalizeLap(lapTimeMs: Long, lapNumber: Int) {
        val isNewBest = sessionBestLapMs == null || lapTimeMs < sessionBestLapMs!!
        if (isNewBest) {
            sessionBestLapMs = lapTimeMs
            sessionBestLapNumber = lapNumber

            // Le ghost doit représenter le tour réellement réalisé.
            if (currentLapSectorMs.all { it > 0 }) {
                ghost = GhostLap(
                    lapTimeMs = currentLapSectorMs.sum(),
                    sectorTimesMs = currentLapSectorMs.copyOf(),
                    sectorCumulativeMs = buildCumulative(currentLapSectorMs)
                )
            }
        }

        val newRecord = LapRecord(
            lapNumber = lapNumber,
            lapTimeMs = lapTimeMs,
            isSessionBest = isNewBest
        )
        val history = (_state.value.lapHistory + newRecord)
            .takeLast(3)
            .map { it.copy(isSessionBest = it.lapNumber == sessionBestLapNumber) }
            .sortedByDescending { it.lapNumber }

        _state.value = _state.value.copy(
            bestLapMs = sessionBestLapMs,
            ghost = ghost,
            lapHistory = history
        )
    }

    private fun buildCumulative(sectors: LongArray): LongArray {
        val out = LongArray(sectors.size + 1)
        var acc = 0L
        sectors.forEachIndexed { i, t ->
            if (t != Long.MAX_VALUE) acc += t
            out[i + 1] = acc
        }
        return out
    }

    private fun updateDelta(nowMs: Long) {
        val ghostRef = ghost
        val sectorIdx = _state.value.sectorIndex.coerceIn(0, 2)
        val lapElapsed = nowMs - lapStartMs
        val sectorElapsed = nowMs - sectorStartMs

        // Nombre de secteurs déjà bouclés dans le tour courant.
        val completed = sectorIdx
        val currentCompleted = (0 until completed).sumOf { currentLapSectorMs[it] }
        val delta = if (ghostRef == null || completed == 0) {
            0L
        } else {
            currentCompleted - ghostRef.cumulativeAtSectorEnd(completed - 1)
        }

        _state.value = _state.value.copy(
            currentLapMs = lapElapsed,
            currentSectorMs = sectorElapsed,
            deltaChronoMs = delta,
            ghost = ghostRef
        )
    }

    fun resetSession() {
        previousPosition = null
        inLap = false
        sessionBestSectorMs.fill(Long.MAX_VALUE)
        currentLapSectorMs.fill(0L)
        sessionBestLapMs = null
        sessionBestLapNumber = null
        ghost = null
        _state.value = TrackState()
    }
}
