package com.gtrack.track

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackManagerTest {

    private fun testGeometry() = TrackGeometry(
        name = "Circuit de test",
        startFinishLine = VirtualLine(GeoPoint(0.0, 0.0), GeoPoint(0.001, 0.0)),
        sector1Line = VirtualLine(GeoPoint(0.0, 0.001), GeoPoint(0.001, 0.001)),
        sector2Line = VirtualLine(GeoPoint(0.0, 0.002), GeoPoint(0.001, 0.002)),
        sector3Line = VirtualLine(GeoPoint(0.0, 0.003), GeoPoint(0.001, 0.003)),
        circuitOutline = emptyList()
    )

    /**
     * Trajectoire synthétique : le pilote part à l'ouest de la ligne SF, la
     * franchit vers l'est, enchaîne S1 et S2, puis revient par le nord pour
     * repasser SF. Les lignes S1/S2 sont franchies une seule fois par tour.
     */
    private fun driveLap(
        manager: TrackManager,
        lapStartMs: Long,
        sector1Ms: Long,
        sector2Ms: Long,
        lapEndMs: Long
    ) {
        val t1 = lapStartMs + sector1Ms
        val t2 = t1 + sector2Ms
        manager.onGpsUpdate(0.0005, 0.0015, t1)          // S1
        manager.onGpsUpdate(0.0005, 0.0025, t2)          // S2
        manager.onGpsUpdate(0.0005, 0.0035, t2 + 1000)   // S3 (affichage uniquement)
        manager.onGpsUpdate(0.0100, 0.0035, t2 + 2000)   // sortie nord
        manager.onGpsUpdate(0.0100, -0.0005, t2 + 3000)  // retour ouest
        manager.onGpsUpdate(0.0005, -0.0005, t2 + 4000)  // retour sud
        manager.onGpsUpdate(0.0005, 0.0005, lapEndMs)    // SF
    }

    @Test
    fun `le premier point gps ne demarre pas un tour`() {
        val manager = TrackManager(testGeometry())
        manager.onGpsUpdate(0.0005, 0.0005, 1000)
        assertEquals(0, manager.state.value.lapCount)
    }

    @Test
    fun `le ghost correspond au tour reellement realise`() {
        val manager = TrackManager(testGeometry())
        manager.onGpsUpdate(0.0005, -0.0005, 0)
        manager.onGpsUpdate(0.0005, 0.0005, 1000) // franchissement SF -> départ

        driveLap(manager, lapStartMs = 1000, sector1Ms = 10_000, sector2Ms = 10_000, lapEndMs = 41_000)

        val state = manager.state.value
        assertEquals(40_000L, state.bestLapMs)
        val ghost = state.ghost!!
        assertEquals(40_000L, ghost.lapTimeMs)
        assertArrayEquals(longArrayOf(10_000, 10_000, 20_000), ghost.sectorTimesMs)
        assertEquals(listOf(10_000L, 20_000L, 40_000L), ghost.sectorCumulativeMs.drop(1))
    }

    @Test
    fun `un meilleur tour remplace le ghost et corrige les flags`() {
        val manager = TrackManager(testGeometry())
        manager.onGpsUpdate(0.0005, -0.0005, 0)
        manager.onGpsUpdate(0.0005, 0.0005, 1000)

        driveLap(manager, lapStartMs = 1000, sector1Ms = 10_000, sector2Ms = 10_000, lapEndMs = 41_000)
        driveLap(manager, lapStartMs = 41_000, sector1Ms = 9_000, sector2Ms = 9_000, lapEndMs = 70_000)

        val state = manager.state.value
        assertEquals(29_000L, state.bestLapMs)
        assertArrayEquals(longArrayOf(9_000, 9_000, 11_000), state.ghost!!.sectorTimesMs)
        assertEquals(2, state.lapHistory.size)
        assertFalse(state.lapHistory.first { it.lapNumber == 1 }.isSessionBest)
        assertTrue(state.lapHistory.first { it.lapNumber == 2 }.isSessionBest)
    }
}
