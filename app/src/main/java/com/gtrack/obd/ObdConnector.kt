package com.gtrack.obd

import kotlinx.coroutines.flow.StateFlow

/** Transport vers l'ELM327 (SPP classique ou BLE). */
interface ObdConnector {
    val connectionState: StateFlow<ObdConnectionState>
    fun bindDeviceAddress(address: String)
    fun connect()
    fun disconnect()
    suspend fun sendCommand(command: String): String?
    suspend fun queryPid(pidHex: String): String? =
        sendCommand("01${pidHex.uppercase()}")
}
