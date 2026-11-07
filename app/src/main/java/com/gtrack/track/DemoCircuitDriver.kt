package com.gtrack.track

/**
 * Parcourt le circuit de démonstration à vitesse constante.
 *
 * Le mode démo ne se contente plus de faire clignoter un point GPS autour d'une
 * position fixe : il roule réellement le tour, ce qui permet de valider de bout
 * en bout le chrono, les secteurs, le delta et le ghost sur le téléphone.
 */
class DemoCircuitDriver(
    private val trace: List<GeoPoint> = TrackGeometry.demoCircuit().circuitOutline,
    private val speedMps: Double = 22.0
) {
    private val cumulative = TrackMath.cumulativeDistances(trace)
    private val totalMeters = TrackMath.totalLength(cumulative)
    private var travelledMeters = 0.0

    /** Distance parcourue depuis le départ de la démo, en mètres. */
    val distanceMeters: Double get() = travelledMeters

    fun step(deltaMs: Long): GeoPoint {
        if (totalMeters <= 0.0 || trace.size < 2) return GeoPoint(0.0, 0.0)
        val advanced = speedMps * (deltaMs.coerceAtLeast(0L) / 1000.0)
        travelledMeters = (travelledMeters + advanced) % totalMeters
        return TrackMath.pointAtDistance(trace, cumulative, travelledMeters).first
    }
}
