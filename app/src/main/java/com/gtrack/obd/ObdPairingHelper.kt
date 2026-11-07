package com.gtrack.obd



import android.annotation.SuppressLint

import android.bluetooth.BluetoothDevice

import android.content.BroadcastReceiver

import android.content.Context

import android.content.Intent

import android.content.IntentFilter

import android.os.Build



/**

 * Injecte le PIN Bluetooth quand Android affiche (ou bloque) la demande d'appairage.

 */

object ObdPairingHelper {

    /** @hide — absent du SDK public, valeur documentée = 7 */
    private const val PAIRING_VARIANT_PIN_16_DIGITS = 7

    private var pairingReceiver: BroadcastReceiver? = null



    @SuppressLint("MissingPermission")

    fun registerPinReceiver(

        context: Context,

        pin: String,

        targetAddress: String? = null

    ) {

        unregisterPinReceiver(context)

        val filter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)

        pairingReceiver = object : BroadcastReceiver() {

            override fun onReceive(ctx: Context?, intent: Intent?) {

                if (intent?.action != BluetoothDevice.ACTION_PAIRING_REQUEST) return

                val device = pairingDeviceFromIntent(intent) ?: return

                if (targetAddress != null && !device.address.equals(targetAddress, ignoreCase = true)) {

                    return

                }

                val variant = intent.getIntExtra(

                    BluetoothDevice.EXTRA_PAIRING_VARIANT,

                    BluetoothDevice.ERROR

                )

                when (variant) {

                    BluetoothDevice.PAIRING_VARIANT_PIN,

                    PAIRING_VARIANT_PIN_16_DIGITS -> {

                        if (injectPin(device, pin)) {

                            abortBroadcast()

                        }

                    }

                    BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION -> {

                        if (confirmPairing(device, true)) {

                            abortBroadcast()

                        }

                    }

                }

            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            context.registerReceiver(pairingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        } else {

            context.registerReceiver(pairingReceiver, filter)

        }

    }



    fun unregisterPinReceiver(context: Context) {

        pairingReceiver?.let {

            try {

                context.unregisterReceiver(it)

            } catch (_: IllegalArgumentException) {

            }

        }

        pairingReceiver = null

    }



    @SuppressLint("MissingPermission")

    fun requestPairing(context: Context, device: BluetoothDevice, pin: String): Boolean {

        registerPinReceiver(context, pin, device.address)

        return ObdBluetoothHelper.requestPairing(device)

    }



    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")

    private fun injectPin(device: BluetoothDevice, pin: String): Boolean = try {

        val method = device.javaClass.getMethod("setPin", ByteArray::class.java)

        method.invoke(device, pin.toByteArray()) as Boolean

    } catch (_: Exception) {

        false

    }



    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")

    private fun confirmPairing(device: BluetoothDevice, confirm: Boolean): Boolean = try {

        val method = device.javaClass.getMethod(

            "setPairingConfirmation",

            Boolean::class.javaPrimitiveType

        )

        method.invoke(device, confirm) as Boolean

    } catch (_: Exception) {

        false

    }



    @Suppress("DEPRECATION")

    private fun pairingDeviceFromIntent(intent: Intent): BluetoothDevice? =

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)

        } else {

            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        }

}


