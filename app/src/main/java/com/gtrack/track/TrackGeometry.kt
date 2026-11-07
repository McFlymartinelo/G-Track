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

        /**
         * Construit la géométrie d'un circuit à partir d'un tracé GPS fermé.
         *
         * C'est la brique utilisée par le futur mode « tour d'apprentissage » :
         * on roule un tour, on relève le tracé, et on en déduit la ligne de
         * départ/arrivée ainsi que les lignes de secteur.
         *
         * @param sectorFractions positions des secteurs, en fraction du tour
         *        (0.333 et 0.666 par défaut).
         */
        fun fromTrace(
            name: String,
            trace: List<GeoPoint>,
            sectorFractions: List<Double> = listOf(1.0 / 3.0, 2.0 / 3.0),
            halfWidthMeters: Double = 10.0
        ): TrackGeometry {
            require(trace.size >= 4) { "Un tracé de circuit doit contenir au moins 4 points" }

            // Un tracé enregistré se termine souvent sur son point de départ :
            // on retire le doublon pour ne pas créer de segment de longueur nulle.
            val points = if (TrackMath.distanceMeters(trace.first(), trace.last()) < 2.0) {
                trace.dropLast(1)
            } else {
                trace
            }
            val cumulative = TrackMath.cumulativeDistances(points)
            val total = TrackMath.totalLength(cumulative)
            require(total > 50.0) { "Tracé trop court pour être un circuit" }

            fun lineAt(fraction: Double): VirtualLine {
                val (point, direction) = TrackMath.pointAtDistance(
                    points, cumulative, total * fraction
                )
                return TrackMath.perpendicularLine(point, direction, halfWidthMeters)
            }

            val startFinish = lineAt(0.0)
            return TrackGeometry(
                name = name,
                startFinishLine = startFinish,
                sector1Line = lineAt(sectorFractions.getOrElse(0) { 1.0 / 3.0 }),
                sector2Line = lineAt(sectorFractions.getOrElse(1) { 2.0 / 3.0 }),
                // Le 3e secteur se termine sur la ligne SF : c'est elle qui le clôt.
                sector3Line = startFinish,
                circuitOutline = points
            )
        }

        /**
         * Circuit virtuel utilisé par le mode démo.
         *
         * Un ovale d'environ 1 km : à 22 m/s un tour dure ~45 s, ce qui permet
         * de voir le chrono, les secteurs, le delta et le ghost fonctionner
         * sans voiture ni GPS réel.
         */
        fun demoCircuit(): TrackGeometry {
            val center = GeoPoint(47.2235, 14.7600)
            val halfWidthEastWestM = 200.0
            val halfHeightNorthSouthM = 110.0
            val steps = 400
            val latScale = TrackMath.METERS_PER_DEG_LAT
            val lonScale = TrackMath.metersPerDegLon(center.latitude)
            val trace = (0..steps).map { i ->
                val angle = 2.0 * Math.PI * i / steps
                GeoPoint(
                    latitude = center.latitude +
                        halfHeightNorthSouthM * kotlin.math.sin(angle) / latScale,
                    longitude = center.longitude +
                        halfWidthEastWestM * kotlin.math.cos(angle) / lonScale
                )
            }
            return fromTrace("Circuit démo", trace, halfWidthMeters = 9.0)
        }
    }
}
