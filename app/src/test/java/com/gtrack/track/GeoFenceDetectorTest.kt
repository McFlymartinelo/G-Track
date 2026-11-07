package com.gtrack.track

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoFenceDetectorTest {

    /** Ligne horizontale à latitude 0, de longitude 0 à 0.001 (~111 m). */
    private val line = VirtualLine(
        pointA = GeoPoint(0.0, 0.0),
        pointB = GeoPoint(0.0, 0.001),
        detectionRadiusMeters = 12.0
    )

    @Test
    fun `franchissement a l interieur du segment detecte`() {
        val previous = GeoPoint(-0.0001, 0.0005)
        val current = GeoPoint(0.0001, 0.0005)
        assertTrue(GeoFenceDetector.hasCrossedLine(previous, current, line))
    }

    @Test
    fun `franchissement sur le prolongement de la ligne ignore`() {
        val previous = GeoPoint(-0.0001, 0.0020)
        val current = GeoPoint(0.0001, 0.0020)
        assertFalse(GeoFenceDetector.hasCrossedLine(previous, current, line))
    }

    @Test
    fun `deux points du meme cote ne declenchent rien`() {
        val previous = GeoPoint(-0.0002, 0.0005)
        val current = GeoPoint(-0.0001, 0.0005)
        assertFalse(GeoFenceDetector.hasCrossedLine(previous, current, line))
    }

    @Test
    fun `premier point gps ne declenche pas de tour`() {
        assertFalse(GeoFenceDetector.hasCrossedLine(null, GeoPoint(0.0, 0.0005), line))
    }

    @Test
    fun `franchissement sur une extremite du segment detecte`() {
        val previous = GeoPoint(-0.0001, 0.0010)
        val current = GeoPoint(0.0001, 0.0010)
        assertTrue(GeoFenceDetector.hasCrossedLine(previous, current, line))
    }
}
