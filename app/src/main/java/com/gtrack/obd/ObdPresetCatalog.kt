package com.gtrack.obd

/**
 * Catalogue embarqué de profils PID (extensible via JSON / serveur plus tard).
 */
object ObdPresetCatalog {

    const val DEFAULT_PRESET_ID = "generic_obd2"

    private val standardCore = listOf(
        ObdPidQuery(ObdPid.SPEED, ObdTelemetryField.SPEED),
        ObdPidQuery(ObdPid.RPM, ObdTelemetryField.RPM),
        ObdPidQuery(ObdPid.THROTTLE, ObdTelemetryField.THROTTLE),
        ObdPidQuery(ObdPid.COOLANT_TEMP, ObdTelemetryField.COOLANT),
        ObdPidQuery(ObdPid.ENGINE_LOAD, ObdTelemetryField.ENGINE_LOAD),
        ObdPidQuery(ObdPid.TIMING_ADVANCE, ObdTelemetryField.TIMING_ADVANCE),
        ObdPidQuery(ObdPid.INTAKE_MAP, ObdTelemetryField.INTAKE_MAP),
        ObdPidQuery(ObdPid.BAROMETRIC, ObdTelemetryField.BAROMETRIC),
        ObdPidQuery(ObdPid.O2_VOLTAGE_B1S1, ObdTelemetryField.O2_VOLTAGE),
        ObdPidQuery(ObdPid.O2_LAMBDA_B1S1, ObdTelemetryField.AFR_LAMBDA),
        ObdPidQuery(ObdPid.COMMANDED_EQUIV, ObdTelemetryField.AFR_EQUIV)
    )

    // Décodeurs SAE J1979 génériques (mode 01) pour PID étendus écrits dans le snapshot.
    private val decAMinus40: ObdPidDecoder = { ObdExtendedPid.byteA(it)?.let { a -> a - 40f } }
    private val decPercent255: ObdPidDecoder = { ObdExtendedPid.byteA(it)?.let { a -> a * 100f / 255f } }
    private val decVoltAb: ObdPidDecoder = { ObdExtendedPid.combineAb(it)?.let { ab -> ab / 1000f } }
    private val decTempAb10: ObdPidDecoder = { ObdExtendedPid.combineAb(it)?.let { ab -> ab / 10f - 40f } }

    private fun stdExt(
        pid: String,
        decoder: ObdPidDecoder,
        writer: ObdSnapshotWriter
    ) = ObdPidQuery(
        pidHex = pid,
        field = ObdTelemetryField.EXTENDED,
        decoder = decoder,
        writer = writer
    )

    /**
     * Citroën C4 B7 (CAN ISO 15765-4, EOBD) : PIDs standard + capteurs supplémentaires
     * exposés par l'ECU (temp. huile/admission/extérieure/catalyseur/échappement, tension ECU,
     * pédale). Les PIDs non supportés par le moteur sont auto-désactivés au bout de 4 échecs.
     */
    private val citroenC4Queries: List<ObdPidQuery> = standardCore + listOf(
        ObdPidQuery(ObdPid.OIL_TEMP, ObdTelemetryField.OIL_TEMP),                 // 0x5C
        stdExt("0F", decAMinus40) { s, v -> s.copy(intakeTempC = v) },            // temp. air admission
        stdExt("46", decAMinus40) { s, v -> s.copy(extTempC = v) },               // temp. air ambiant
        stdExt("42", decVoltAb) { s, v -> s.copy(battVoltV = v) },                // tension batterie ECU
        stdExt("49", decPercent255) { s, v -> s.copy(pedalPercent = v) },         // pédale accélérateur
        stdExt("3C", decTempAb10) { s, v -> s.copy(catTempC = v) },               // temp. catalyseur B1S1
        stdExt("78", decTempAb10) { s, v -> s.copy(egtC = v) }                    // temp. gaz échappement B1
    )

    val all: List<ObdVehiclePreset> = listOf(
        ObdVehiclePreset(
            id = DEFAULT_PRESET_ID,
            brand = "Générique",
            model = "OBD-II (ISO)",
            summary = "PIDs standard SAE — compatible avec la plupart des véhicules après 2001.",
            queries = standardCore + ObdPidQuery(ObdPid.OIL_TEMP, ObdTelemetryField.OIL_TEMP)
        ),
        ObdVehiclePreset(
            id = "vag_vw_audi",
            brand = "VAG",
            model = "VW / Audi / Skoda / Seat",
            summary = "PIDs standard + temp. huile (0x5C) si l’ECU la expose.",
            queries = standardCore + ObdPidQuery(ObdPid.OIL_TEMP, ObdTelemetryField.OIL_TEMP)
        ),
        ObdVehiclePreset(
            id = "bmw",
            brand = "BMW",
            model = "Série E/F (OBD-II)",
            summary = "PIDs standard ; certains modèles nécessitent un adaptateur BMW pour données étendues.",
            queries = standardCore + ObdPidQuery(ObdPid.OIL_TEMP, ObdTelemetryField.OIL_TEMP)
        ),
        ObdVehiclePreset(
            id = "citroen_c4_b7",
            brand = "Citroën",
            model = "C4 B7 (2010–2016)",
            summary = "PIDs SAE C4 B7 : + temp. huile (5C)/admission (0F)/ext. (46)/cata (3C)/échap. (78), tension ECU (42), pédale (49).",
            queries = citroenC4Queries
        ),
        ObdVehiclePreset(
            id = "renault_psa",
            brand = "Renault / PSA",
            model = "Peugeot 208, Clio… (générique)",
            summary = "PIDs standard OBD-II — Citroën / Peugeot / Renault (MAP pour turbo).",
            queries = standardCore
        ),
        ObdVehiclePreset(
            id = "alpine",
            brand = "Alpine",
            model = "A110 / A290 (mode 22)",
            summary = "PIDs étendus Alpine : vitesse 221102, régime 221201, temp. 221101/22111F, papillon 221117…",
            queries = ObdAlpinePidCatalog.telemetryQueries()
        ),
        ObdVehiclePreset(
            id = "ford_gm",
            brand = "Ford / GM",
            model = "OBD-II US/EU",
            summary = "PIDs standard SAE.",
            queries = standardCore
        ),
        ObdVehiclePreset(
            id = "toyota_honda",
            brand = "Toyota / Honda",
            model = "OBD-II",
            summary = "PIDs standard ; AFR souvent via lambda (0x24).",
            queries = standardCore
        ),
        ObdVehiclePreset(
            id = "performance_track",
            brand = "Circuit",
            model = "Priorité chrono / G",
            summary = "Vitesse, RPM, papillon, charge — rafraîchissement plus rapide des données utiles piste.",
            queries = listOf(
                ObdPidQuery(ObdPid.SPEED, ObdTelemetryField.SPEED),
                ObdPidQuery(ObdPid.RPM, ObdTelemetryField.RPM),
                ObdPidQuery(ObdPid.THROTTLE, ObdTelemetryField.THROTTLE),
                ObdPidQuery(ObdPid.ENGINE_LOAD, ObdTelemetryField.ENGINE_LOAD),
                ObdPidQuery(ObdPid.COOLANT_TEMP, ObdTelemetryField.COOLANT),
                ObdPidQuery(ObdPid.INTAKE_MAP, ObdTelemetryField.INTAKE_MAP)
            )
        )
    )

    fun byId(id: String?): ObdVehiclePreset =
        all.find { it.id == id } ?: all.first { it.id == DEFAULT_PRESET_ID }
}
