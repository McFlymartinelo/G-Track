package com.gtrack.track

/**
 * Géométrie du circuit : ligne SF/A, 3 secteurs et tracé GPS (schéma carte).
 */
data class GeoPoint(val latitude: Double, val longitude: Double)

data class VirtualLine(
    val pointA: GeoPoint,
    val pointB: GeoPoint,
    val detectionRadiusMeters: Double = 12.0
)

data class TrackGeometry(
    val name: String,
    val startFinishLine: VirtualLine,
    val sector1Line: VirtualLine,
    val sector2Line: VirtualLine,
    val sector3Line: VirtualLine,
    val circuitOutline: List<GeoPoint>
) {
    companion object {
        /** Red Bull Ring (Spielberg) — tracé simplifié + lignes SF/secteurs. */
        fun redBullRing(): TrackGeometry {
            val outline = listOf(
                GeoPoint(47.2188, 14.7640),
                GeoPoint(47.2205, 14.7705),
                GeoPoint(47.2238, 14.7755),
                GeoPoint(47.2275, 14.7768),
                GeoPoint(47.2310, 14.7735),
                GeoPoint(47.2335, 14.7680),
                GeoPoint(47.2348, 14.7610),
                GeoPoint(47.2340, 14.7540),
                GeoPoint(47.2315, 14.7485),
                GeoPoint(47.2278, 14.7455),
                GeoPoint(47.2235, 14.7465),
                GeoPoint(47.2198, 14.7510),
                GeoPoint(47.2175, 14.7575),
                GeoPoint(47.2188, 14.7640)
            )
            return TrackGeometry(
                name = "Red Bull Ring",
                startFinishLine = VirtualLine(
                    GeoPoint(47.2188, 14.7640),
                    GeoPoint(47.2198, 14.7510)
                ),
                sector1Line = VirtualLine(
                    GeoPoint(47.2275, 14.7768),
                    GeoPoint(47.2310, 14.7735)
                ),
                sector2Line = VirtualLine(
                    GeoPoint(47.2348, 14.7610),
                    GeoPoint(47.2315, 14.7485)
                ),
                sector3Line = VirtualLine(
                    GeoPoint(47.2235, 14.7465),
                    GeoPoint(47.2175, 14.7575)
                ),
                circuitOutline = outline
            )
        }

        fun defaultSample(): TrackGeometry = redBullRing()
    }
}
