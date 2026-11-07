package com.gtrack.track

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCircuitTest {

    @Test
    fun `la demo enchaine des tours complets`() {
        val geometry = TrackGeometry.demoCircuit()
        val manager = TrackManager(geometry)
        val driver = DemoCircuitDriver(geometry.circuitOutline)

        var now = 0L
        // 200 s à 22 m/s ≈ 4,4 tours de ~45 s.
        repeat(2000) {
            now += 100
            val position = driver.step(100)
            manager.onGpsUpdate(position.latitude, position.longitude, now)
        }

        val state = manager.state.value
        assertTrue("au moins deux tours attendus, obtenu ${state.lapCount}", state.lapCount >= 2)
        assertNotNull("le chrono doit avoir un meilleur tour", state.bestLapMs)
        assertNotNull("un ghost doit exister après le premier tour", state.ghost)
        assertTrue("le delta doit être renseigné", state.ghost!!.sectorTimesMs.all { it > 0 })
    }

    @Test
    fun `le tour de demo dure environ 45 secondes`() {
        val geometry = TrackGeometry.demoCircuit()
        val total = TrackMath.totalLength(TrackMath.cumulativeDistances(geometry.circuitOutline))
        val lapMs = total / 22.0 * 1000.0
        assertEquals(45_000.0, lapMs, 3_000.0)
    }
}
