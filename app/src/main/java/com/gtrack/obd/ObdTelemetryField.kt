package com.gtrack.obd

/** Champ télémétrique alimenté par un PID OBD mode 01. */
enum class ObdTelemetryField {
    SPEED,
    RPM,
    COOLANT,
    OIL_TEMP,
    INTAKE_MAP,
    BAROMETRIC,
    TIMING_ADVANCE,
    ENGINE_LOAD,
    THROTTLE,
    O2_VOLTAGE,
    AFR_LAMBDA,
    AFR_EQUIV,

    /** PID constructeur écrivant directement dans [ObdSnapshot] via [ObdPidQuery.writer]. */
    EXTENDED
}

typealias ObdPidDecoder = (IntArray) -> Float?

/** Écrit une valeur décodée dans le snapshot (pour les PID étendus génériques). */
typealias ObdSnapshotWriter = (ObdSnapshot, Float) -> ObdSnapshot

data class ObdPidQuery(
    val pidHex: String,
    val field: ObdTelemetryField,
    /** Commande complète type Torque (ex. «221102»). Si null → mode 01 + [pidHex]. */
    val modeAndPid: String? = null,
    val decoder: ObdPidDecoder? = null,
    /** Header CAN ELM327 (ATSH) à poser avant la requête, ex. «745». Null = header par défaut. */
    val header: String? = null,
    /** Si défini avec [decoder], écrit la valeur dans le snapshot (champ [ObdTelemetryField.EXTENDED]). */
    val writer: ObdSnapshotWriter? = null
) {
    constructor(pid: String, field: ObdTelemetryField) : this(
        pidHex = pid,
        field = field,
        modeAndPid = null,
        decoder = null
    )
}
