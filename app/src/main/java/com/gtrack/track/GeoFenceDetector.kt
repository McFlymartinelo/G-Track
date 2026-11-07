package com.gtrack.track

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Détection de franchissement de ligne (géofencing par collision segment / position).
 */
object GeoFenceDetector {

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceToLineMeters(position: GeoPoint, line: VirtualLine): Double {
        val p = toMeters(position)
        val a = toMeters(line.pointA)
        val b = toMeters(line.pointB)
        val abx = b.first - a.first
        val aby = b.second - a.second
        val apx = p.first - a.first
        val apy = p.second - a.second
        val abLenSq = abx * abx + aby * aby
        if (abLenSq == 0.0) return hypot(p.first - a.first, p.second - a.second)
        val t = ((apx * abx + apy * aby) / abLenSq).coerceIn(0.0, 1.0)
        val closestX = a.first + t * abx
        val closestY = a.second + t * aby
        return hypot(p.first - closestX, p.second - closestY)
    }

    /**
     * Retourne true si le segment [position précédente, position actuelle]
     * coupe réellement le segment de la ligne de détection.
     *
     * Contrairement à un simple test de côté, on vérifie que l'intersection
     * tombe bien entre les deux extrémités de la ligne : un passage sur le
     * prolongement de la ligne ne déclenche pas de faux tour.
     */
    fun hasCrossedLine(previous: GeoPoint?, current: GeoPoint, line: VirtualLine): Boolean {
        // Sans point précédent, aucun franchissement n'est prouvable.
        if (previous == null) return false
        return segmentsIntersect(
            toMeters(previous),
            toMeters(current),
            toMeters(line.pointA),
            toMeters(line.pointB),
        )
    }

    private fun segmentsIntersect(
        p1: Pair<Double, Double>,
        p2: Pair<Double, Double>,
        p3: Pair<Double, Double>,
        p4: Pair<Double, Double>,
    ): Boolean {
        val d1 = cross(p3, p4, p1)
        val d2 = cross(p3, p4, p2)
        val d3 = cross(p1, p2, p3)
        val d4 = cross(p1, p2, p4)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
        ) {
            return true
        }

        // Cas particulier : intersection sur une extrémité (tolérance 1 m²).
        val eps = 1.0
        return (abs(d1) <= eps && onSegment(p3, p4, p1)) ||
            (abs(d2) <= eps && onSegment(p3, p4, p2)) ||
            (abs(d3) <= eps && onSegment(p1, p2, p3)) ||
            (abs(d4) <= eps && onSegment(p1, p2, p4))
    }

    private fun onSegment(
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
        p: Pair<Double, Double>,
    ): Boolean {
        val eps = 1e-9
        return p.first >= min(a.first, b.first) - eps &&
            p.first <= max(a.first, b.first) + eps &&
            p.second >= min(a.second, b.second) - eps &&
            p.second <= max(a.second, b.second) + eps
    }

    private fun cross(
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
        p: Pair<Double, Double>,
    ): Double = (b.first - a.first) * (p.second - a.second) -
        (b.second - a.second) * (p.first - a.first)

    private fun toMeters(point: GeoPoint): Pair<Double, Double> {
        val latRad = Math.toRadians(point.latitude)
        val lonRad = Math.toRadians(point.longitude)
        val x = EARTH_RADIUS_M * lonRad * cos(latRad)
        val y = EARTH_RADIUS_M * latRad
        return x to y
    }

    private fun hypot(dx: Double, dy: Double): Double = sqrt(dx * dx + dy * dy)
}
