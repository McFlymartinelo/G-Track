package com.gtrack.track

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackGeometryTest {

    private val origin = GeoPoint(47.0, 6.0)

    /** Carré de 200 m de côté, fermé, dans le sens est puis nord. */
    private fun squareTrace(): List<GeoPoint> = listOf(
        TrackMath.toGeo(0.0, 0.0, origin),
        TrackMath.toGeo(200.0, 0.0, origin),
        TrackMath.toGeo(200.0, 200.0, origin),
        TrackMath.toGeo(0.0, 200.0, origin),
        TrackMath.toGeo(0.0, 0.0, origin)
    )

    private fun midpointMeters(line: VirtualLine): Pair<Double, Double> {
        val a = TrackMath.toMeters(line.pointA, origin)
        val b = TrackMath.toMeters(line.pointB, origin)
        return Pair((a.first + b.first) / 2.0, (a.second + b.second) / 2.0)
    }

    @Test
    fun `la ligne SF est placee au depart et perpendiculaire au sens de marche`() {
        val geometry = TrackGeometry.fromTrace("Test", squareTrace(), halfWidthMeters = 9.0)
        val (x, y) = midpointMeters(geometry.startFinishLine)
        assertEquals(0.0, x, 1.0)
        assertEquals(0.0, y, 1.0)

        // Départ vers l'est : la ligne doit être orientée nord-sud.
        val a = TrackMath.toMeters(geometry.startFinishLine.pointA, origin)
        val b = TrackMath.toMeters(geometry.startFinishLine.pointB, origin)
        assertEquals(a.first, b.first, 1.0)
        assertEquals(18.0, kotlin.math.abs(b.second - a.second), 1.0)
    }

    @Test
    fun `les secteurs tombent au tiers et aux deux tiers du tour`() {
        val geometry = TrackGeometry.fromTrace("Test", squareTrace())
        val (x1, y1) = midpointMeters(geometry.sector1Line)
        val (x2, y2) = midpointMeters(geometry.sector2Line)

        // 800 m de tour : 266,7 m puis 533,3 m depuis le départ.
        assertEquals(200.0, x1, 3.0)
        assertEquals(66.7, y1, 3.0)
        assertEquals(66.7, x2, 3.0)
        assertEquals(200.0, y2, 3.0)
    }

    @Test
    fun `le troisieme secteur se ferme sur la ligne SF`() {
        val geometry = TrackGeometry.fromTrace("Test", squareTrace())
        assertEquals(geometry.startFinishLine, geometry.sector3Line)
    }

    @Test
    fun `un trace non ferme reste utilisable`() {
        val geometry = TrackGeometry.fromTrace("Test", squareTrace().dropLast(1))
        assertTrue(geometry.circuitOutline.size >= 4)
        assertTrue(TrackMath.totalLength(TrackMath.cumulativeDistances(geometry.circuitOutline)) > 700.0)
    }
}
