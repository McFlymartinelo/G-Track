package com.gtrack.obd

enum class ObdLinkType(val storageKey: String) {
    /** ELM327 Bluetooth Low Energy (sans appairage système classique). */
    BLE("ble"),

    /** ELM327 Bluetooth classique (SPP, appairage dans Réglages). */
    CLASSIC_SPP("classic_spp");

    companion object {
        fun fromStorageKey(key: String?): ObdLinkType =
            entries.find { it.storageKey == key } ?: BLE
    }
}
