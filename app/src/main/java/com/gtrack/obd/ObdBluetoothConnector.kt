package com.gtrack.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Connexion ELM327 via Bluetooth SPP et envoi de commandes OBD mode 01.
 */
class ObdBluetoothConnector(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    private val scope: CoroutineScope
) : ObdConnector {
    companion object {
        private val ELM327_SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val INIT_COMMANDS = listOf(
            "ATZ",
            "ATE0",
            "ATL0",
            "ATH0",
            "ATS0",
            "ATAT2",
            "ATSP0"
        )
    }

    private val _connectionState = MutableStateFlow(ObdConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()

    private val commandMutex = Mutex()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var boundDeviceAddress: String? = null

    override fun bindDeviceAddress(address: String) {
        boundDeviceAddress = address
    }

    override fun connect() {
        if (_connectionState.value == ObdConnectionState.CONNECTING) return
        val address = boundDeviceAddress ?: run {
            _connectionState.value = ObdConnectionState.ERROR
            return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            _connectionState.value = ObdConnectionState.ERROR
            return
        }
        if (!adapter.isEnabled) {
            _connectionState.value = ObdConnectionState.ERROR
            return
        }
        scope.launch {
            _connectionState.value = ObdConnectionState.CONNECTING
            try {
                withContext(Dispatchers.IO) {
                    disconnectIo()
                    val device: BluetoothDevice = adapter.getRemoteDevice(address)
                    adapter.cancelDiscovery()
                    val s = openSocket(device)
                    socket = s
                    inputStream = s.inputStream
                    outputStream = s.outputStream
                    initializeElm()
                }
                _connectionState.value = ObdConnectionState.CONNECTED
                ObdDebugLog.logInfo("SPP connecté — init ELM327 terminée")
            } catch (e: IOException) {
                ObdDebugLog.logInfo("SPP échec connexion: ${e.message ?: "IO"}")
                disconnect()
                _connectionState.value = ObdConnectionState.ERROR
            } catch (_: SecurityException) {
                _connectionState.value = ObdConnectionState.ERROR
            }
        }
    }

    override fun disconnect() {
        scope.launch(Dispatchers.IO) {
            disconnectIo()
            _connectionState.value = ObdConnectionState.DISCONNECTED
        }
    }

    override suspend fun sendCommand(command: String): String? = commandMutex.withLock {
        val cmd = command.trimEnd('\r', '\n') + "\r"
        ObdDebugLog.logTx(cmd)
        withContext(Dispatchers.IO) {
            val out = outputStream ?: return@withContext null
            val input = inputStream ?: return@withContext null
            try {
                drainInput(input)
                out.write(cmd.toByteArray(Charsets.US_ASCII))
                out.flush()
                val response = readUntilPrompt(input, timeoutMs = 1_500L)
                ObdDebugLog.logRx(response.ifBlank { "(vide)" })
                response.ifBlank { null }
            } catch (_: IOException) {
                ObdDebugLog.logRx("erreur IO")
                disconnectIo()
                _connectionState.value = ObdConnectionState.ERROR
                null
            }
        }
    }

    private fun initializeElm() {
        val out = outputStream ?: return
        val input = inputStream ?: return
        INIT_COMMANDS.forEach { cmd ->
            val line = "$cmd\r"
            ObdDebugLog.logTx(line)
            out.write(line.toByteArray(Charsets.US_ASCII))
            out.flush()
            val timeout = if (cmd == "ATZ") 6_000L else 3_000L
            val response = readUntilPrompt(input, timeout)
            ObdDebugLog.logRx(response.ifBlank { "(vide)" })
        }
    }

    /**
     * Lit jusqu'au prompt « > ».
     * Sur SPP Android, [InputStream.available] est peu fiable : on lit par blocs avec une
     * granularité fine pour minimiser la latence (l'ELM327 répond en ~40-80 ms au ralenti).
     */
    private fun readUntilPrompt(input: InputStream, timeoutMs: Long): String {
        val sb = StringBuilder()
        val buffer = ByteArray(1024)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val pending = input.available()
                if (pending <= 0) {
                    Thread.sleep(4)
                    continue
                }
                val read = input.read(buffer, 0, minOf(pending, buffer.size))
                if (read == -1) break
                val chunk = String(buffer, 0, read, Charsets.US_ASCII)
                for (ch in chunk) {
                    if (ch == '\u0000') continue
                    sb.append(ch)
                    if (ch == '>') return formatElmResponse(sb)
                }
            } catch (_: IOException) {
                break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        return formatElmResponse(sb)
    }

    private fun formatElmResponse(raw: StringBuilder): String =
        raw.toString()
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != ">" }
            .joinToString(" ")
            .trim()

    private fun drainInput(input: InputStream) {
        try {
            val toSkip = input.available()
            if (toSkip > 0) input.skip(toSkip.toLong())
        } catch (_: IOException) {
        }
    }

    @Suppress("DiscouragedPrivateApi")
    private fun openSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val socket = device.createRfcommSocketToServiceRecord(ELM327_SPP_UUID)
            socket.connect()
            return socket
        } catch (primary: IOException) {
            try {
                val method = device.javaClass.getMethod(
                    "createRfcommSocket",
                    Int::class.javaPrimitiveType
                )
                @Suppress("UNCHECKED_CAST")
                val fallback = method.invoke(device, 1) as BluetoothSocket
                fallback.connect()
                return fallback
            } catch (_: Exception) {
                throw primary
            }
        }
    }

    private fun disconnectIo() {
        try {
            socket?.close()
        } catch (_: IOException) {
        } finally {
            socket = null
            inputStream = null
            outputStream = null
        }
    }
}
