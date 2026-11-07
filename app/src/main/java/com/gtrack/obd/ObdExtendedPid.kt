package com.gtrack.obd

/**
 * Réponses ELM327 mode 21/22 (PIDs constructeur, ex. Alpine / Torque « ModeAndPID »).
 */
object ObdExtendedPid {

    fun parseBytes(response: String?, modeAndPid: String): IntArray? {
        if (response.isNullOrBlank() || modeAndPid.length < 4) return null
        val upper = response.uppercase()
        if (upper.contains("NO DATA") || upper.contains("UNABLE") || upper.contains("ERROR")) {
            return null
        }
        val reqMode = modeAndPid.take(2)
        val pid = modeAndPid.drop(2)
        val respMode = (reqMode.toIntOrNull(16) ?: return null) + 0x40
        val marker = "%02X%s".format(respMode, pid)
        val compact = upper.replace(Regex("[^0-9A-F]"), "")
        val idx = compact.indexOf(marker)
        if (idx < 0) return null
        val dataHex = compact.substring(idx + marker.length)
        if (dataHex.length < 2 || dataHex.length % 2 != 0) return null
        return dataHex.chunked(2).mapNotNull { it.toIntOrNull(16) }.toIntArray().takeIf { it.isNotEmpty() }
    }

    fun byteA(bytes: IntArray): Int? = bytes.firstOrNull()

    fun combineAb(bytes: IntArray): Int? = when {
        bytes.isEmpty() -> null
        bytes.size == 1 -> bytes[0]
        else -> (bytes[0] shl 8) or bytes[1]
    }
}
