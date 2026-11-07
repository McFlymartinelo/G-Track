package com.gtrack.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build

object ObdBleHelper {

    private val NAME_HINTS = listOf("OBD", "ELM", "VEEPEAK", "V-LINK", "OBDII", "OBD2", "CAR")

    fun isBluetoothEnabled(context: Context): Boolean =
        ObdBluetoothHelper.isBluetoothEnabled(context)

    @SuppressLint("MissingPermission")
    fun startBleScan(
        context: Context,
        onDeviceFound: (BluetoothDevice) -> Unit,
        onScanFailed: () -> Unit = {}
    ): ScanCallback? {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) return null
        val scanner = adapter.bluetoothLeScanner ?: return null

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                onDeviceFound(result?.device ?: return)
            }

            override fun onScanFailed(errorCode: Int) {
                onScanFailed()
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, callback)
        return callback
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan(context: Context, callback: ScanCallback?) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
        callback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
    }

    fun looksLikeObdBleName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val upper = name.uppercase()
        return NAME_HINTS.any { upper.contains(it) }
    }
}
