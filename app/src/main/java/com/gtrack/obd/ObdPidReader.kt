package com.gtrack.obd

import com.gtrack.auto.theme.RpmScale
import kotlinx.coroutines.flow.StateFlow

/**
 * Interroge l'ELM327 selon le [ObdVehiclePreset] actif (lots rotatifs).
 */
class ObdPidReader(
    private val connector: ObdConnector
) {
    private companion object {
        /** Capteurs très dynamiques : interrogés à CHAQUE cycle pour un affichage fluide. */
        val PRIORITY_FIELDS = setOf(
            ObdTelemetryField.SPEED,
            ObdTelemetryField.RPM,
            ObdTelemetryField.THROTTLE,
            ObdTelemetryField.INTAKE_MAP,
            ObdTelemetryField.ENGINE_LOAD
        )

        /** Nb d'échecs consécutifs avant de considérer un PID non supporté par le véhicule. */
        const val UNSUPPORTED_THRESHOLD = 4
    }

    private var snapshot = ObdSnapshot()
    private var activePreset: ObdVehiclePreset = ObdPresetCatalog.byId(ObdPresetCatalog.DEFAULT_PRESET_ID)

    private var priorityQueries: List<ObdPidQuery> = emptyList()
    private var rotatingQueries: List<ObdPidQuery> = emptyList()
    private var rotatingIndex = 0
    private var currentHeader: String? = null
    private var fuelTypeDetected = false

    private val failureStreak = HashMap<String, Int>()
    private val disabledPids = HashSet<String>()

    var lastPollMatchedCount: Int = 0
        private set
    var lastPollAtMs: Long = 0L
        private set

    val connectionState: StateFlow<ObdConnectionState> = connector.connectionState

    fun setPreset(preset: ObdVehiclePreset) {
        activePreset = preset
        val queries = preset.queries.ifEmpty {
            ObdPresetCatalog.byId(ObdPresetCatalog.DEFAULT_PRESET_ID).queries
        }
        priorityQueries = queries.filter { it.field in PRIORITY_FIELDS }
        rotatingQueries = queries.filterNot { it.field in PRIORITY_FIELDS }
        reset()
    }

    fun getActivePreset(): ObdVehiclePreset = activePreset

    suspend fun poll(): ObdSnapshot {
        if (connector.connectionState.value != ObdConnectionState.CONNECTED) {
            return snapshot
        }
        detectFuelTypeOnce()
        val cycle = buildCycle()
        var matched = 0
        for (query in cycle) {
            val response = queryModeResponse(query)
            val bytes = response?.let { parseBytes(query, it) }
            if (bytes != null) {
                matched++
                resetFailure(query)
                snapshot = applyParsedQuery(snapshot, query, bytes)
            } else {
                registerFailure(query)
            }
        }
        lastPollMatchedCount = matched
        lastPollAtMs = System.currentTimeMillis()
        return snapshot
    }

    /** Tous les PIDs prioritaires actifs + un PID secondaire (rotation). */
    private fun buildCycle(): List<ObdPidQuery> {
        val cycle = ArrayList<ObdPidQuery>(priorityQueries.size + 1)
        priorityQueries.forEach { if (it.key() !in disabledPids) cycle.add(it) }
        nextRotatingQuery()?.let { cycle.add(it) }
        return cycle
    }

    private fun nextRotatingQuery(): ObdPidQuery? {
        if (rotatingQueries.isEmpty()) return null
        val activeRotating = rotatingQueries.filter { it.key() !in disabledPids }
        if (activeRotating.isEmpty()) return null
        val query = activeRotating[rotatingIndex % activeRotating.size]
        rotatingIndex++
        return query
    }

    private fun ObdPidQuery.key(): String = modeAndPid ?: pidHex

    private fun resetFailure(query: ObdPidQuery) {
        failureStreak.remove(query.key())
    }

    private fun registerFailure(query: ObdPidQuery) {
        val key = query.key()
        val streak = (failureStreak[key] ?: 0) + 1
        failureStreak[key] = streak
        if (streak >= UNSUPPORTED_THRESHOLD && disabledPids.add(key)) {
            ObdDebugLog.logInfo("PID $key non supporté — désactivé (profil ${activePreset.id})")
        }
    }

    fun reset() {
        snapshot = ObdSnapshot()
        rotatingIndex = 0
        currentHeader = null
        fuelTypeDetected = false
        failureStreak.clear()
        disabledPids.clear()
    }

    private suspend fun queryModeResponse(query: ObdPidQuery): String? {
        applyHeaderIfNeeded(query.header)
        val cmd = query.modeAndPid?.let { it.uppercase() }
            ?: return connector.queryPid(query.pidHex)
        return connector.sendCommand(cmd)
    }

    /** Lit une fois le PID 0151 (Fuel Type) et adapte la zone rouge (diesel → 6000). */
    private suspend fun detectFuelTypeOnce() {
        if (fuelTypeDetected) return
        fuelTypeDetected = true
        applyHeaderIfNeeded(null)
        val response = connector.queryPid("51") ?: return
        val code = ObdPid.parseMode01Bytes(response, "51")?.let { ObdExtendedPid.byteA(it) } ?: return
        RpmScale.setFromObdFuelCode(code)
        ObdDebugLog.logInfo("Carburant détecté (PID 0151=$code) → redline ${RpmScale.redline} tr/min")
    }

    /** Pose le header CAN (ATSH) seulement quand il change, pour limiter la latence. */
    private suspend fun applyHeaderIfNeeded(header: String?) {
        val target = header ?: ObdAlpinePidCatalog.DEFAULT_HEADER
        if (target == currentHeader) return
        connector.sendCommand("ATSH$target")
        currentHeader = target
    }

    private fun parseBytes(query: ObdPidQuery, response: String): IntArray? =
        when (val modePid = query.modeAndPid) {
            null -> ObdPid.parseMode01Bytes(response, query.pidHex.uppercase())
            else -> ObdExtendedPid.parseBytes(response, modePid)
        }

    private fun applyParsedQuery(
        current: ObdSnapshot,
        query: ObdPidQuery,
        bytes: IntArray
    ): ObdSnapshot {
        val writer = query.writer
        val decoder = query.decoder
        if (writer != null && decoder != null) {
            val value = decoder(bytes) ?: return current
            return writer(current, value)
        }
        decoder?.invoke(bytes)?.let { decoded ->
            return applyDecoded(current, query.field, decoded)
        }

        return when (query.field) {
            ObdTelemetryField.SPEED -> current.copy(speedKmh = ObdPid.speedKmh(bytes))
            ObdTelemetryField.RPM -> current.copy(rpm = ObdPid.rpm(bytes))
            ObdTelemetryField.COOLANT -> current.copy(waterTempC = ObdPid.coolantC(bytes))
            ObdTelemetryField.OIL_TEMP -> current.copy(oilTempC = ObdPid.oilTempC(bytes))
            ObdTelemetryField.INTAKE_MAP -> {
                val map = ObdPid.mapKpa(bytes)
                current.copy(
                    mapKpa = map,
                    turboPressureBar = ObdPid.turboBar(map, current.baroKpa)
                )
            }
            ObdTelemetryField.BAROMETRIC -> {
                val baro = ObdPid.mapKpa(bytes)
                current.copy(
                    baroKpa = baro,
                    turboPressureBar = ObdPid.turboBar(current.mapKpa, baro)
                )
            }
            ObdTelemetryField.TIMING_ADVANCE -> current.copy(
                ignitionAdvanceBtdc = ObdPid.timingAdvanceBtdc(bytes)
            )
            ObdTelemetryField.ENGINE_LOAD -> current.copy(
                engineLoadPercent = ObdPid.engineLoadPercent(bytes)
            )
            ObdTelemetryField.THROTTLE -> current.copy(
                throttlePercent = ObdPid.throttlePercent(bytes)
            )
            ObdTelemetryField.O2_VOLTAGE -> current.copy(
                o2VoltageV = ObdPid.o2VoltageV(bytes)
            )
            ObdTelemetryField.AFR_LAMBDA -> current.copy(
                afr = ObdPid.afrFromLambda(bytes) ?: current.afr
            )
            ObdTelemetryField.AFR_EQUIV -> current.copy(
                afr = ObdPid.afrFromCommandedEquiv(bytes) ?: current.afr
            )
            ObdTelemetryField.EXTENDED -> current
        }
    }

    private fun applyDecoded(
        current: ObdSnapshot,
        field: ObdTelemetryField,
        value: Float
    ): ObdSnapshot = when (field) {
        ObdTelemetryField.SPEED -> current.copy(speedKmh = value)
        ObdTelemetryField.RPM -> current.copy(rpm = value.toInt())
        ObdTelemetryField.COOLANT -> current.copy(waterTempC = value)
        ObdTelemetryField.OIL_TEMP -> current.copy(oilTempC = value)
        ObdTelemetryField.INTAKE_MAP -> {
            val map = value.toInt()
            current.copy(
                mapKpa = map,
                turboPressureBar = ObdPid.turboBar(map, current.baroKpa)
            )
        }
        ObdTelemetryField.BAROMETRIC -> {
            val baro = value.toInt()
            current.copy(
                baroKpa = baro,
                turboPressureBar = ObdPid.turboBar(current.mapKpa, baro)
            )
        }
        ObdTelemetryField.TIMING_ADVANCE -> current.copy(ignitionAdvanceBtdc = value)
        ObdTelemetryField.ENGINE_LOAD -> current.copy(engineLoadPercent = value)
        ObdTelemetryField.THROTTLE -> current.copy(throttlePercent = value)
        ObdTelemetryField.O2_VOLTAGE -> current.copy(o2VoltageV = value)
        ObdTelemetryField.AFR_LAMBDA, ObdTelemetryField.AFR_EQUIV -> current.copy(afr = value)
        ObdTelemetryField.EXTENDED -> current
    }
}
