package com.gtrack.obd



import android.annotation.SuppressLint

import android.bluetooth.BluetoothAdapter

import android.bluetooth.BluetoothDevice

import android.bluetooth.BluetoothGatt

import android.bluetooth.BluetoothGattCallback

import android.bluetooth.BluetoothGattCharacteristic

import android.bluetooth.BluetoothGattDescriptor

import android.bluetooth.BluetoothManager

import android.bluetooth.BluetoothProfile

import android.bluetooth.BluetoothStatusCodes

import android.content.Context

import android.os.Build

import kotlinx.coroutines.CompletableDeferred

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch

import kotlinx.coroutines.sync.Mutex

import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.withContext

import kotlinx.coroutines.withTimeoutOrNull

import java.util.UUID



/**

 * ELM327 via Bluetooth LE (GATT UART) — adaptateurs « OBD BLE ».

 */

class ObdBleConnector(

    private val context: Context,

    private val scope: CoroutineScope

) : ObdConnector {



    companion object {

        private val INIT_COMMANDS = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATS0", "ATSP0")

        private val CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")



        private val SERVICE_CANDIDATES = listOf(

            "0000fff0-0000-1000-8000-00805f9b34fb",

            "0000ffe0-0000-1000-8000-00805f9b34fb",

            "6e400001-b5a3-f393-e0a9-e50e24dcca9e"

        )

        private val WRITE_CANDIDATES = listOf(

            "0000fff1-0000-1000-8000-00805f9b34fb",

            "0000ffe1-0000-1000-8000-00805f9b34fb",

            "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

        )

        private val NOTIFY_CANDIDATES = listOf(

            "0000fff2-0000-1000-8000-00805f9b34fb",

            "0000ffe1-0000-1000-8000-00805f9b34fb",

            "0000ffe2-0000-1000-8000-00805f9b34fb",

            "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

        )

    }



    private val _connectionState = MutableStateFlow(ObdConnectionState.DISCONNECTED)

    override val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()



    private val commandMutex = Mutex()

    private val gattOpMutex = Mutex()

    private val responseBuffer = StringBuilder()

    private var deviceAddress: String? = null

    private var gatt: BluetoothGatt? = null

    private var writeChar: BluetoothGattCharacteristic? = null

    private var notifyChar: BluetoothGattCharacteristic? = null

    private var servicesReady = false

    private var elmReady = false

    private var pendingWrite: CompletableDeferred<Int>? = null

    private var pendingDescriptor: CompletableDeferred<Int>? = null

    private var mtuRequested = false



    override fun bindDeviceAddress(address: String) {

        deviceAddress = address

    }



    @SuppressLint("MissingPermission")

    override fun connect() {

        when (_connectionState.value) {

            ObdConnectionState.CONNECTING, ObdConnectionState.CONNECTED -> return

            else -> Unit

        }

        val address = deviceAddress ?: run {

            _connectionState.value = ObdConnectionState.ERROR

            return

        }

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

            ?: BluetoothAdapter.getDefaultAdapter()

        if (adapter == null || !adapter.isEnabled) {

            _connectionState.value = ObdConnectionState.ERROR

            return

        }

        scope.launch {

            _connectionState.value = ObdConnectionState.CONNECTING

            servicesReady = false

            elmReady = false

            mtuRequested = false

            withContext(Dispatchers.Main) {

                disconnectGatt()

                val device = adapter.getRemoteDevice(address)

                if (!ensureBonded(device)) {

                    ObdDebugLog.logInfo("Appairage BLE requis — validez le PIN puis reconnectez")

                    _connectionState.value = ObdConnectionState.ERROR

                    return@withContext

                }

                gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

                } else {

                    device.connectGatt(context, false, gattCallback)

                }

            }

        }

    }



    @SuppressLint("MissingPermission")

    override fun disconnect() {

        scope.launch(Dispatchers.Main) {

            disconnectGatt()

            _connectionState.value = ObdConnectionState.DISCONNECTED

        }

    }



    override suspend fun sendCommand(command: String): String? = commandMutex.withLock {

        if (!servicesReady || _connectionState.value != ObdConnectionState.CONNECTED) {

            ObdDebugLog.logInfo("commande ignorée (pas prêt): ${command.trim()}")

            return@withLock null

        }

        if (!elmReady && !command.trim().startsWith("AT", ignoreCase = true)) {

            ObdDebugLog.logInfo("commande ignorée (init ELM en cours): ${command.trim()}")

            return@withLock null

        }

        ObdDebugLog.logTx(command)

        val g = gatt ?: return@withLock null

        val write = writeChar ?: return@withLock null

        responseBuffer.clear()

        val payload = command.toByteArray(Charsets.US_ASCII)

        val written = writeGattWithRetry(g, write, payload)

        if (!written) {

            ObdDebugLog.logRx("échec écriture GATT")

            return@withLock null

        }

        val response = waitForElmResponse(4_000L)

        ObdDebugLog.logRx(response)

        response

    }



    private suspend fun waitForElmResponse(timeoutMs: Long): String? {

        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {

            val text = responseBuffer.toString()

            if (text.contains(">") || text.contains("OK") || text.contains("ELM") || text.contains("NO DATA")) {

                return text.trim()

            }

            delay(40)

        }

        return responseBuffer.toString().trim().takeIf { it.isNotEmpty() }

    }



    @SuppressLint("MissingPermission")

    private suspend fun initializeElm() {

        delay(350)

        var initOk = false

        for (cmd in INIT_COMMANDS) {

            val response = sendCommand("$cmd\r")

            if (!response.isNullOrBlank()) initOk = true

            delay(250)

        }

        elmReady = initOk

        if (initOk) {

            ObdDebugLog.logInfo("ELM327 initialisé (BLE)")

        } else {

            ObdDebugLog.logInfo("Échec init ELM327 — écritures GATT refusées")

        }

    }



    @SuppressLint("MissingPermission")

    private suspend fun ensureBonded(device: BluetoothDevice): Boolean {

        when (device.bondState) {

            BluetoothDevice.BOND_BONDED -> return true

            BluetoothDevice.BOND_BONDING -> return waitForBond(device, 60_000L)

            else -> {

                val pin = ObdSettingsStore(context).getPairingPin()

                ObdPairingHelper.registerPinReceiver(context, pin, device.address)

                ObdDebugLog.logInfo("Appairage BLE (PIN configuré)…")

                if (!device.createBond()) return false

                return waitForBond(device, 60_000L)

            }

        }

    }



    @SuppressLint("MissingPermission")

    private suspend fun waitForBond(device: BluetoothDevice, timeoutMs: Long): Boolean {

        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {

            when (device.bondState) {

                BluetoothDevice.BOND_BONDED -> return true

                BluetoothDevice.BOND_NONE -> {

                    if (System.currentTimeMillis() - start > 5_000L) return false

                }

            }

            delay(250)

        }

        return device.bondState == BluetoothDevice.BOND_BONDED

    }



    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                    }

                    gatt.discoverServices()

                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    servicesReady = false

                    elmReady = false

                    _connectionState.value = ObdConnectionState.DISCONNECTED

                }

            }

            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {

                _connectionState.value = ObdConnectionState.ERROR

            }

        }



        @SuppressLint("MissingPermission")

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                ObdDebugLog.logInfo("MTU BLE = $mtu")

            }

            if (!servicesReady) {

                scope.launch { enableNotificationsAsync(gatt) }

            }

        }



        @SuppressLint("MissingPermission")

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

            if (status != BluetoothGatt.GATT_SUCCESS) {

                _connectionState.value = ObdConnectionState.ERROR

                return

            }

            if (!resolveUartCharacteristics(gatt)) {

                ObdDebugLog.logInfo("UART GATT introuvable — essayez Bluetooth classique (SPP)")

                _connectionState.value = ObdConnectionState.ERROR

                return

            }

            if (!mtuRequested) {

                mtuRequested = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    gatt.requestMtu(517)

                } else {

                    scope.launch { enableNotificationsAsync(gatt) }

                }

            }

        }



        override fun onCharacteristicChanged(

            gatt: BluetoothGatt,

            characteristic: BluetoothGattCharacteristic,

            value: ByteArray

        ) {

            appendResponse(value)

        }



        @Deprecated("Deprecated in Java")

        override fun onCharacteristicChanged(

            gatt: BluetoothGatt,

            characteristic: BluetoothGattCharacteristic

        ) {

            characteristic.value?.let { appendResponse(it) }

        }



        override fun onCharacteristicWrite(

            gatt: BluetoothGatt,

            characteristic: BluetoothGattCharacteristic,

            status: Int

        ) {

            if (characteristic.uuid == writeChar?.uuid) {

                pendingWrite?.complete(status)

            }

        }



        override fun onDescriptorWrite(

            gatt: BluetoothGatt,

            descriptor: BluetoothGattDescriptor,

            status: Int

        ) {

            pendingDescriptor?.complete(status)

        }

    }



    private fun appendResponse(bytes: ByteArray) {

        responseBuffer.append(String(bytes, Charsets.US_ASCII))

    }



    @SuppressLint("MissingPermission")

    private fun resolveUartCharacteristics(gatt: BluetoothGatt): Boolean {

        val services = gatt.services ?: return false

        for (serviceId in SERVICE_CANDIDATES) {

            val service = services.find {

                it.uuid.toString().equals(serviceId, ignoreCase = true)

            } ?: continue

            val write = WRITE_CANDIDATES.firstNotNullOfOrNull { id ->

                service.getCharacteristic(UUID.fromString(id))

            }

            val notify = NOTIFY_CANDIDATES.firstNotNullOfOrNull { id ->

                service.getCharacteristic(UUID.fromString(id))

            } ?: write

            if (write != null && notify != null) {

                writeChar = write

                notifyChar = notify

                ObdDebugLog.logInfo(

                    "GATT UART: write=${write.uuid}, notify=${notify.uuid}, props=0x${write.properties.toString(16)}"

                )

                return true

            }

        }

        for (service in services) {

            val chars = service.characteristics ?: continue

            val write = chars.firstOrNull {

                it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||

                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0

            }

            val notify = chars.firstOrNull {

                it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||

                    it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

            }

            if (write != null && notify != null) {

                writeChar = write

                notifyChar = notify

                ObdDebugLog.logInfo("GATT UART (secours): write=${write.uuid}, notify=${notify.uuid}")

                return true

            }

        }

        return false

    }



    @SuppressLint("MissingPermission")

    private suspend fun enableNotificationsAsync(gatt: BluetoothGatt) = gattOpMutex.withLock {

        withContext(Dispatchers.Main) {

            val notify = notifyChar ?: run {

                _connectionState.value = ObdConnectionState.ERROR

                return@withContext

            }

            gatt.setCharacteristicNotification(notify, true)

            val cccd = notify.getDescriptor(CLIENT_CONFIG)

            if (cccd != null) {

                pendingDescriptor = CompletableDeferred()

                val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==

                        BluetoothStatusCodes.SUCCESS

                } else {

                    @Suppress("DEPRECATION")

                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                    gatt.writeDescriptor(cccd)

                }

                if (!started) {

                    pendingDescriptor?.complete(BluetoothGatt.GATT_FAILURE)

                    pendingDescriptor = null

                    _connectionState.value = ObdConnectionState.ERROR

                    return@withContext

                }

                val status = withTimeoutOrNull(5_000L) { pendingDescriptor?.await() }

                pendingDescriptor = null

                if (status != BluetoothGatt.GATT_SUCCESS) {

                    _connectionState.value = ObdConnectionState.ERROR

                    return@withContext

                }

                servicesReady = true

                _connectionState.value = ObdConnectionState.CONNECTED

                scope.launch { initializeElm() }

            } else {

                servicesReady = true

                _connectionState.value = ObdConnectionState.CONNECTED

                scope.launch { initializeElm() }

            }

        }

    }



    private fun writeTypesFor(properties: Int): List<Int> {

        val types = mutableListOf<Int>()

        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {

            types.add(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)

        }

        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {

            types.add(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

        }

        if (types.isEmpty()) types.add(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

        return types.distinct()

    }



    @SuppressLint("MissingPermission")

    private suspend fun writeGattWithRetry(

        gatt: BluetoothGatt,

        characteristic: BluetoothGattCharacteristic,

        payload: ByteArray,

        maxAttempts: Int = 5

    ): Boolean = gattOpMutex.withLock {

        val types = writeTypesFor(characteristic.properties)

        repeat(maxAttempts) { attempt ->

            for (writeType in types) {

                if (writeGattOnce(gatt, characteristic, payload, writeType)) return@withLock true

                delay(80L * (attempt + 1))

            }

        }

        false

    }



    @SuppressLint("MissingPermission")

    private suspend fun writeGattOnce(

        gatt: BluetoothGatt,

        characteristic: BluetoothGattCharacteristic,

        payload: ByteArray,

        writeType: Int

    ): Boolean = withContext(Dispatchers.Main) {

        val needsCallback = writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        if (needsCallback) pendingWrite = CompletableDeferred()



        val started = performWrite(gatt, characteristic, payload, writeType)

        if (!started) {

            pendingWrite?.complete(BluetoothGatt.GATT_FAILURE)

            pendingWrite = null

            return@withContext false

        }



        if (needsCallback) {

            val status = withTimeoutOrNull(4_000L) { pendingWrite?.await() }

            pendingWrite = null

            status == BluetoothGatt.GATT_SUCCESS

        } else {

            delay(150)

            true

        }

    }



    @SuppressLint("MissingPermission")

    private fun performWrite(

        gatt: BluetoothGatt,

        characteristic: BluetoothGattCharacteristic,

        payload: ByteArray,

        writeType: Int

    ): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            gatt.writeCharacteristic(characteristic, payload, writeType) ==

                BluetoothStatusCodes.SUCCESS

        } else {

            @Suppress("DEPRECATION")

            characteristic.value = payload

            characteristic.writeType = writeType

            gatt.writeCharacteristic(characteristic)

        }

    }



    @SuppressLint("MissingPermission")

    private fun disconnectGatt() {

        servicesReady = false

        elmReady = false

        mtuRequested = false

        pendingWrite?.complete(BluetoothGatt.GATT_FAILURE)

        pendingWrite = null

        pendingDescriptor?.complete(BluetoothGatt.GATT_FAILURE)

        pendingDescriptor = null

        writeChar = null

        notifyChar = null

        gatt?.close()

        gatt = null

    }

}


