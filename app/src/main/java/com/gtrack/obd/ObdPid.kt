package com.gtrack.obd

/**
 * PIDs OBD-II mode 01 (ELM327 : commande « 01XX »).
 */
object ObdPid {
    const val ENGINE_LOAD = "04"
    const val COOLANT_TEMP = "05"
    const val SHORT_FUEL_TRIM_1 = "06"
    const val INTAKE_MAP = "0B"
    const val RPM = "0C"
    const val SPEED = "0D"
    const val TIMING_ADVANCE = "0E"
    const val THROTTLE = "11"
    const val O2_VOLTAGE_B1S1 = "14"
    const val O2_LAMBDA_B1S1 = "24"
    const val BAROMETRIC = "33"
    const val COMMANDED_EQUIV = "34"
    const val OIL_TEMP = "5C"

    fun parseMode01Bytes(response: String?, pidHex: String): IntArray? {
        if (response.isNullOrBlank()) return null
        val upper = response.uppercase()
        if (upper.contains("NO DATA") || upper.contains("UNABLE") || upper.contains("ERROR")) {
            return null
        }
        val compact = upper.replace(Regex("[^0-9A-F]"), "")
        val marker = "41$pidHex"
        val idx = compact.indexOf(marker)
        if (idx < 0) return null
        val dataHex = compact.substring(idx + marker.length)
        if (dataHex.length < 2 || dataHex.length % 2 != 0) return null
        return dataHex.chunked(2).mapNotNull { hex ->
            hex.toIntOrNull(16)
        }.toIntArray().takeIf { it.isNotEmpty() }
    }

    fun speedKmh(bytes: IntArray): Float? =
        bytes.firstOrNull()?.toFloat()

    fun rpm(bytes: IntArray): Int? {
        if (bytes.size < 2) return null
        return ((bytes[0] * 256) + bytes[1]) / 4
    }

    fun coolantC(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { (it - 40).toFloat() }

    fun oilTempC(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { (it - 40).toFloat() }

    fun mapKpa(bytes: IntArray): Int? =
        bytes.firstOrNull()

    fun turboBar(mapKpa: Int?, baroKpa: Int?): Float? {
        val map = mapKpa ?: return null
        val baro = baroKpa ?: 101
        val boostKpa = (map - baro).coerceAtLeast(0)
        return boostKpa / 100f
    }

    fun timingAdvanceBtdc(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { it * 0.5f - 64f }

    fun engineLoadPercent(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { it * 100f / 255f }

    fun throttlePercent(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { it * 100f / 255f }

    fun o2VoltageV(bytes: IntArray): Float? =
        bytes.firstOrNull()?.let { it / 200f }

    fun afrFromLambda(bytes: IntArray): Float? {
        if (bytes.size < 2) return null
        val lambda = ((bytes[0] * 256) + bytes[1]) / 32768.0
        return (lambda * 14.7).toFloat()
    }

    fun afrFromCommandedEquiv(bytes: IntArray): Float? {
        if (bytes.size < 2) return null
        val ratio = ((bytes[0] * 256) + bytes[1]) / 32768.0
        return (ratio * 14.7).toFloat()
    }
}
