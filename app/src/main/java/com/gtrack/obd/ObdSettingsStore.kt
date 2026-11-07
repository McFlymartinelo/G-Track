package com.gtrack.obd

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.obdDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gtrack_obd"
)

class ObdSettingsStore(private val context: Context) {

    companion object {
        private val KEY_DEVICE_ADDRESS = stringPreferencesKey("elm327_bluetooth_address")
        private val KEY_PRESET_ID = stringPreferencesKey("obd_vehicle_preset_id")
        private val KEY_LINK_TYPE = stringPreferencesKey("obd_link_type")
        private val KEY_PAIRING_PIN = stringPreferencesKey("obd_pairing_pin")
        const val DEFAULT_PAIRING_PIN = "0000"
    }


    val deviceAddressFlow: Flow<String?> = context.obdDataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ADDRESS]?.takeIf { it.isNotBlank() }
    }

    suspend fun getDeviceAddress(): String? = deviceAddressFlow.first()

    suspend fun setDeviceAddress(address: String?) {
        context.obdDataStore.edit { prefs ->
            if (address.isNullOrBlank()) {
                prefs.remove(KEY_DEVICE_ADDRESS)
            } else {
                prefs[KEY_DEVICE_ADDRESS] = address
            }
        }
    }

    fun isObdConfigured(): Flow<Boolean> =
        deviceAddressFlow.map { !it.isNullOrBlank() }

    val presetIdFlow: Flow<String> = context.obdDataStore.data.map { prefs ->
        prefs[KEY_PRESET_ID] ?: ObdPresetCatalog.DEFAULT_PRESET_ID
    }

    suspend fun getPresetId(): String = presetIdFlow.first()

    suspend fun setPresetId(presetId: String) {
        context.obdDataStore.edit { prefs ->
            prefs[KEY_PRESET_ID] = presetId
        }
    }

    val linkTypeFlow: Flow<ObdLinkType> = context.obdDataStore.data.map { prefs ->
        ObdLinkType.fromStorageKey(prefs[KEY_LINK_TYPE])
    }

    suspend fun getLinkType(): ObdLinkType = linkTypeFlow.first()

    suspend fun setLinkType(type: ObdLinkType) {
        context.obdDataStore.edit { prefs ->
            prefs[KEY_LINK_TYPE] = type.storageKey
        }
    }

    suspend fun getPairingPin(): String =
        context.obdDataStore.data.first()[KEY_PAIRING_PIN] ?: DEFAULT_PAIRING_PIN

    suspend fun setPairingPin(pin: String) {
        context.obdDataStore.edit { prefs ->
            prefs[KEY_PAIRING_PIN] = pin.filter { it.isDigit() }.take(6).ifBlank { DEFAULT_PAIRING_PIN }
        }
    }
}
