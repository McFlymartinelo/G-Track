package com.gtrack.track

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Petites conversions géométriques partagées par la création de circuit et la démo.
 *
 * On travaille dans un repère plan local (mètres) centré sur le premier point du
 * tracé : à l'échelle d'un circuit (< 10 km), l'erreur reste négligeable.
 */
object TrackMath {

    const val METERS_PER_DEG_LAT = 111_320.0

    fun metersPerDegLon(latitudeDeg: Double): Double =
        METERS_PER_DEG_LAT * cos(Math.toRadians(latitudeDeg))

    fun toMeters(point: GeoPoint, origin: GeoPoint): Pair<Double, Double> {
        val lonScale = metersPerDegLon(origin.latitude)
        return Pair(
            (point.longitude - origin.longitude) * lonScale,
            (point.latitude - origin.latitude) * METERS_PER_DEG_LAT
        )
    }

    fun toGeo(x: Double, y: Double, origin: GeoPoint): GeoPoint {
        val lonScale = metersPerDegLon(origin.latitude)
        return GeoPoint(
            latitude = origin.latitude + y / METERS_PER_DEG_LAT,
            longitude = origin.longitude + x / lonScale
        )
    }

    /**
     * Distances cumulées le long du tracé, en mètres.
     * Le tracé est considéré fermé : le dernier point rejoint le premier.
     */
    fun cumulativeDistances(trace: List<GeoPoint>): DoubleArray {
        if (trace.isEmpty()) return DoubleArray(0)
        val origin = trace.first()
        val out = DoubleArray(trace.size + 1)
        var acc = 0.0
        out[0] = 0.0
        for (i in 1..trace.size) {
            val previous = toMeters(trace[i - 1], origin)
            val current = toMeters(trace[i % trace.size], origin)
            acc += hypot(current.first - previous.first, current.second - previous.second)
            out[i] = acc
        }
        return out
    }

    /** Longueur totale du tour fermé. */
    fun totalLength(cumulative: DoubleArray): Double =
        if (cumulative.isEmpty()) 0.0 else cumulative.last()

    /**
     * Position et direction de marche à une distance donnée depuis le départ.
     * La direction retournée est un vecteur unitaire dans le repère local (est, nord).
     */
    fun pointAtDistance(
        trace: List<GeoPoint>,
        cumulative: DoubleArray,
        distance: Double
    ): Pair<GeoPoint, Pair<Double, Double>> {
        require(trace.size >= 2) { "Un tracé doit contenir au moins deux points" }
        val total = totalLength(cumulative)
        if (total <= 0.0) {
            return trace.first() to Pair(1.0, 0.0)
        }
        var target = distance % total
        if (target < 0) target += total

        // `cumulative` contient un point de plus que `trace` (fermeture du tour).
        val segment = (0 until trace.size).firstOrNull { i ->
            target >= cumulative[i] && target <= cumulative[i + 1]
        } ?: (trace.size - 1)

        val origin = trace.first()
        val start = toMeters(trace[segment], origin)
        val end = toMeters(trace[(segment + 1) % trace.size], origin)
        val segmentLength = cumulative[segment + 1] - cumulative[segment]
        if (segmentLength <= 0.0) {
            return trace[segment] to Pair(1.0, 0.0)
        }
        val ratio = ((target - cumulative[segment]) / segmentLength).coerceIn(0.0, 1.0)
        val x = start.first + (end.first - start.first) * ratio
        val y = start.second + (end.second - start.second) * ratio
        val dx = (end.first - start.first) / segmentLength
        val dy = (end.second - start.second) / segmentLength
        return toGeo(x, y, origin) to Pair(dx, dy)
    }

    /**
     * Ligne perpendiculaire au sens de marche, centrée sur `point`.
     * C'est la ligne que le pilote franchit lorsqu'il passe à cet endroit.
     */
    fun perpendicularLine(
        point: GeoPoint,
        direction: Pair<Double, Double>,
        halfWidthMeters: Double
    ): VirtualLine {
        val length = hypot(direction.first, direction.second)
        val dx = if (length == 0.0) 1.0 else direction.first / length
        val dy = if (length == 0.0) 0.0 else direction.second / length
        // Perpendiculaire = rotation de 90°.
        val px = -dy * halfWidthMeters
        val py = dx * halfWidthMeters
        val origin = point
        return VirtualLine(
            pointA = toGeo(-px, -py, origin),
            pointB = toGeo(px, py, origin),
            detectionRadiusMeters = halfWidthMeters
        )
    }

    /** true si les deux points sont à moins de `meters` l'un de l'autre. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val origin = a
        val p = toMeters(b, origin)
        return hypot(p.first, p.second)
    }

    /** true si le tracé se referme à moins de `toleranceMeters` de son départ. */
    fun isClosed(trace: List<GeoPoint>, toleranceMeters: Double = 25.0): Boolean {
        if (trace.size < 4) return false
        return distanceMeters(trace.first(), trace.last()) <= toleranceMeters &&
            abs(cumulativeDistances(trace).last()) > 100.0
    }
}
