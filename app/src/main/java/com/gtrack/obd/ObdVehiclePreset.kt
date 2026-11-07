package com.gtrack.obd

/**
 * Profil véhicule : PIDs à interroger et formules associées ([ObdPid]).
 * Les marques partagent souvent les PIDs standard ; les profils affinent la priorité et les PIDs optionnels.
 */
data class ObdVehiclePreset(
    val id: String,
    val brand: String,
    val model: String?,
    val summary: String,
    val queries: List<ObdPidQuery>
) {
    val displayName: String =
        if (model.isNullOrBlank()) brand else "$brand · $model"
}
