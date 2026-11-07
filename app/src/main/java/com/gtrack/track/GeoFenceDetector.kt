package com.gtrack.track

import kotlin.math.cos
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
     * Retourne true si la position a franchi la ligne entre la position précédente et actuelle.
     */
    fun hasCrossedLine(previous: GeoPoint?, current: GeoPoint, line: VirtualLine): Boolean {
        if (previous == null) {
            return distanceToLineMeters(current, line) <= line.detectionRadiusMeters
        }
        val prevSide = sideOfLine(previous, line)
        val currSide = sideOfLine(current, line)
        if (prevSide == 0 || currSide == 0) {
            return distanceToLineMeters(current, line) <= line.detectionRadiusMeters
        }
        return prevSide != currSide
    }

    private fun sideOfLine(point: GeoPoint, line: VirtualLine): Int {
        val p = toMeters(point)
        val a = toMeters(line.pointA)
        val b = toMeters(line.pointB)
        val cross = (b.first - a.first) * (p.second - a.second) - (b.second - a.second) * (p.first - a.first)
        return when {
            cross > 1.0 -> 1
            cross < -1.0 -> -1
            else -> 0
        }
    }

    private fun toMeters(point: GeoPoint): Pair<Double, Double> {
        val latRad = Math.toRadians(point.latitude)
        val lonRad = Math.toRadians(point.longitude)
        val x = EARTH_RADIUS_M * lonRad * cos(latRad)
        val y = EARTH_RADIUS_M * latRad
        return x to y
    }

    private fun hypot(dx: Double, dy: Double): Double = sqrt(dx * dx + dy * dy)
}
