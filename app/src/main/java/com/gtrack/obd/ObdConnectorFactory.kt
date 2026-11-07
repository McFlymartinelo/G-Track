package com.gtrack.obd

import android.content.Context
import kotlinx.coroutines.CoroutineScope

object ObdConnectorFactory {
    fun create(context: Context, scope: CoroutineScope, linkType: ObdLinkType): ObdConnector =
        when (linkType) {
            ObdLinkType.BLE -> ObdBleConnector(context, scope)
            ObdLinkType.CLASSIC_SPP -> ObdBluetoothConnector(context, scope)
        }
}
