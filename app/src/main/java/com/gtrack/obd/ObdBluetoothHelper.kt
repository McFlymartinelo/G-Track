package com.gtrack.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/**
 * Découverte et appairage Bluetooth classique (ELM327 SPP).
 */
object ObdBluetoothHelper {

    fun getAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return null
        return manager.adapter
    }

    fun isBluetoothEnabled(context: Context): Boolean =
        getAdapter(context)?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun loadBondedDevices(context: Context): List<BluetoothDevice> {
        val adapter = getAdapter(context) ?: return emptyList()
        return try {
            adapter.bondedDevices?.sortedBy { it.name ?: it.address } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(
        context: Context,
        onDeviceFound: (BluetoothDevice) -> Unit,
        onFinished: () -> Unit
    ): BroadcastReceiver? {
        val adapter = getAdapter(context) ?: return null
        if (!adapter.isEnabled) return null

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let { onDeviceFound(it) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onFinished()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        adapter.cancelDiscovery()
        adapter.startDiscovery()
        return receiver
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery(context: Context, receiver: BroadcastReceiver?) {
        getAdapter(context)?.cancelDiscovery()
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestPairing(device: BluetoothDevice): Boolean = try {
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> true
            BluetoothDevice.BOND_BONDING -> true
            else -> device.createBond()
        }
    } catch (_: SecurityException) {
        false
    }

    fun bondStateLabel(context: Context, device: BluetoothDevice): String {
        val res = when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> com.gtrack.R.string.obd_bond_bonded
            BluetoothDevice.BOND_BONDING -> com.gtrack.R.string.obd_bond_bonding
            else -> com.gtrack.R.string.obd_bond_none
        }
        return context.getString(res)
    }
}
