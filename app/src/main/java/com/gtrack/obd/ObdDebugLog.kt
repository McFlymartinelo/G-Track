package com.gtrack.obd

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Journal circulant TX/RX ELM327 (visible dans l'écran OBD). */
object ObdDebugLog {
    private const val MAX_LINES = 100
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun clear() {
        _lines.value = emptyList()
    }

    fun logTx(command: String) {
        append("TX", command.replace("\r", "\\r").trim())
    }

    fun logRx(response: String?) {
        val text = response?.replace("\r", "\\r")?.replace("\n", " ")?.trim()
        append("RX", text ?: "(aucune réponse)")
    }

    fun logInfo(message: String) {
        append("··", message)
    }

    private fun append(prefix: String, text: String) {
        val line = "${timeFmt.format(Date())} $prefix  ${text.take(160)}"
        _lines.update { current -> (listOf(line) + current).take(MAX_LINES) }
    }
}
