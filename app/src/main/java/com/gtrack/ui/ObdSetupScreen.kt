package com.gtrack.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.content.BroadcastReceiver
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.obd.ObdBleHelper
import com.gtrack.obd.ObdBluetoothHelper
import com.gtrack.obd.ObdConnectionState
import com.gtrack.obd.ObdLinkType
import com.gtrack.obd.ObdPairingHelper
import com.gtrack.obd.ObdSettingsStore
import com.gtrack.telemetry.TelemetryService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ObdSetupScreen(
    connectionState: ObdConnectionState?,
    onRequestPermissions: () -> Unit,
    onPermissionsGranted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { ObdSettingsStore(context) }

    var linkType by remember { mutableStateOf(ObdLinkType.BLE) }
    var bondedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var sessionRunning by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var pairingMessage by remember { mutableStateOf<String?>(null) }
    var pairingPin by remember { mutableStateOf(ObdSettingsStore.DEFAULT_PAIRING_PIN) }
    var dataSourceHint by remember { mutableStateOf<String?>(null) }
    var discoveryReceiver by remember { mutableStateOf<BroadcastReceiver?>(null) }
    var bleScanCallback by remember { mutableStateOf<ScanCallback?>(null) }

    fun refreshBonded() {
        bondedDevices = ObdBluetoothHelper.loadBondedDevices(context)
    }

    LaunchedEffect(Unit) {
        linkType = settings.getLinkType()
        selectedAddress = settings.getDeviceAddress()
        pairingPin = settings.getPairingPin()
        refreshBonded()
    }

    val selectedBonded = remember(selectedAddress, bondedDevices, discoveredDevices) {
        val address = selectedAddress ?: return@remember false
        bondedDevices.any { it.address == address } ||
            discoveredDevices.any {
                it.address == address && it.bondState == BluetoothDevice.BOND_BONDED
            }
    }

    LaunchedEffect(connectionState) {
        val service = TelemetryService.getInstance()
        sessionRunning = service?.isLiveObdSession() == true ||
            service?.isSimulationActive() == true
    }

    LaunchedEffect(sessionRunning, connectionState) {
        while (sessionRunning) {
            val service = TelemetryService.getInstance()
            dataSourceHint = when {
                service == null -> context.getString(R.string.obd_data_source_idle)
                service.isSimulationActive() -> context.getString(R.string.obd_data_source_demo)
                !service.isLiveObdSession() -> context.getString(R.string.obd_data_source_idle)
                connectionState != ObdConnectionState.CONNECTED -> null
                else -> {
                    val (matched, _) = service.getObdPollStats()
                    if (matched > 0) {
                        context.getString(R.string.obd_data_source_live, matched)
                    } else {
                        context.getString(
                            R.string.obd_data_source_no_pid,
                            service.getActivePresetId()
                        )
                    }
                }
            }
            delay(500)
        }
        dataSourceHint = null
    }

    DisposableEffect(Unit) {
        val bondReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    refreshBonded()
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    pairingMessage = when (device?.bondState) {
                        BluetoothDevice.BOND_BONDED ->
                            context.getString(R.string.obd_bond_bonded)
                        BluetoothDevice.BOND_BONDING ->
                            context.getString(R.string.obd_bond_bonding)
                        else -> context.getString(R.string.obd_pairing_updated)
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bondReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(bondReceiver, filter)
        }
        ObdPairingHelper.registerPinReceiver(context, pairingPin, selectedAddress)
        onDispose {
            ObdPairingHelper.unregisterPinReceiver(context)
            ObdBluetoothHelper.stopDiscovery(context, discoveryReceiver)
            ObdBleHelper.stopBleScan(context, bleScanCallback)
            try {
                context.unregisterReceiver(bondReceiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    LaunchedEffect(pairingPin, selectedAddress) {
        ObdPairingHelper.registerPinReceiver(context, pairingPin, selectedAddress)
    }

    fun startScan() {
        if (!ObdBluetoothHelper.isBluetoothEnabled(context)) {
            pairingMessage = context.getString(R.string.obd_bluetooth_disabled)
            return
        }
        onRequestPermissions()
        discoveredDevices.clear()
        isScanning = true
        pairingMessage = context.getString(R.string.obd_scanning)

        if (linkType == ObdLinkType.BLE) {
            ObdBluetoothHelper.stopDiscovery(context, discoveryReceiver)
            discoveryReceiver = null
            ObdBleHelper.stopBleScan(context, bleScanCallback)
            bleScanCallback = ObdBleHelper.startBleScan(
                context = context,
                onDeviceFound = { device ->
                    if (discoveredDevices.none { it.address == device.address }) {
                        discoveredDevices.add(device)
                    }
                },
                onScanFailed = {
                    pairingMessage = context.getString(R.string.obd_state_error)
                }
            )
            scope.launch {
                delay(12_000)
                ObdBleHelper.stopBleScan(context, bleScanCallback)
                bleScanCallback = null
                isScanning = false
                pairingMessage = context.getString(R.string.obd_scan_done)
            }
        } else {
            ObdBleHelper.stopBleScan(context, bleScanCallback)
            bleScanCallback = null
            ObdBluetoothHelper.stopDiscovery(context, discoveryReceiver)
            discoveryReceiver = ObdBluetoothHelper.startDiscovery(
                context = context,
                onDeviceFound = { device ->
                    if (discoveredDevices.none { it.address == device.address }) {
                        discoveredDevices.add(device)
                    }
                },
                onFinished = {
                    isScanning = false
                    pairingMessage = context.getString(R.string.obd_scan_done)
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.obd_setup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.obd_pairing_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFCC80),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                    connectionState?.let { state ->
                        Text(
                            text = stringResource(
                                R.string.obd_connection_status,
                                obdStateLabel(context, state)
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = when (state) {
                                ObdConnectionState.CONNECTED -> Color(0xFF81C784)
                                ObdConnectionState.ERROR -> Color(0xFFE57373)
                                else -> Color.White.copy(alpha = 0.85f)
                            }
                        )
                    }
                    pairingMessage?.let {
                        Text(text = it, color = Color(0xFF81C784), style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = pairingPin,
                        onValueChange = { value ->
                            val digits = value.filter { it.isDigit() }.take(6)
                            pairingPin = digits
                            scope.launch { settings.setPairingPin(digits) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.obd_pairing_pin_label)) },
                        placeholder = { Text(stringResource(R.string.obd_pairing_pin_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF555555),
                            focusedLabelColor = Color(0xFF64B5F6),
                            unfocusedLabelColor = Color(0xFF888888)
                        )
                    )
                    if (linkType == ObdLinkType.BLE && selectedAddress != null && !selectedBonded) {
                        Text(
                            text = stringResource(R.string.obd_pairing_required),
                            color = Color(0xFFFFB74D),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    dataSourceHint?.let { hint ->
                        Text(
                            text = hint,
                            color = Color(0xFFFFB74D),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
            }

            if (sessionRunning && connectionState == ObdConnectionState.CONNECTED) {
                item {
                    ObdDebugPanel()
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinkTypeChip(
                        label = stringResource(R.string.obd_link_ble),
                        selected = linkType == ObdLinkType.BLE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            linkType = ObdLinkType.BLE
                            scope.launch { settings.setLinkType(ObdLinkType.BLE) }
                        }
                    )
                    LinkTypeChip(
                        label = stringResource(R.string.obd_link_classic),
                        selected = linkType == ObdLinkType.CLASSIC_SPP,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            linkType = ObdLinkType.CLASSIC_SPP
                            scope.launch { settings.setLinkType(ObdLinkType.CLASSIC_SPP) }
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onRequestPermissions()
                            onPermissionsGranted()
                            refreshBonded()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Text(stringResource(R.string.obd_grant_permissions))
                    }
                    Button(
                        onClick = { startScan() },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.obd_scan_devices))
                        }
                    }
                }
            }

            if (linkType == ObdLinkType.BLE) {
                item {
                    Text(
                        text = stringResource(R.string.obd_ble_hint),
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            if (discoveredDevices.isNotEmpty()) {
                item {
                    SectionTitle(
                        if (linkType == ObdLinkType.BLE) {
                            stringResource(R.string.obd_ble_devices)
                        } else {
                            stringResource(R.string.obd_nearby_devices)
                        }
                    )
                }
                items(discoveredDevices, key = { "disc-${it.address}" }) { device ->
                    DeviceRow(
                        device = device,
                        selected = device.address == selectedAddress,
                        subtitle = deviceSubtitle(context, device, linkType),
                        actionLabel = if (device.bondState != BluetoothDevice.BOND_BONDED) {
                            stringResource(R.string.obd_pair_action)
                        } else {
                            null
                        },
                        onAction = {
                            onRequestPermissions()
                            val started = ObdPairingHelper.requestPairing(
                                context,
                                device,
                                pairingPin.ifBlank { ObdSettingsStore.DEFAULT_PAIRING_PIN }
                            )
                            pairingMessage = if (started) {
                                context.getString(
                                    R.string.obd_pairing_started,
                                    device.name ?: device.address
                                ) + " — " + context.getString(R.string.obd_pairing_pin_injected)
                            } else {
                                context.getString(R.string.obd_pairing_failed)
                            }
                        },
                        onSelect = {
                            scope.launch {
                                settings.setDeviceAddress(device.address)
                                selectedAddress = device.address
                            }
                        }
                    )
                }
            }

            if (bondedDevices.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.obd_paired_devices))
                }
                items(bondedDevices, key = { "bond-${it.address}" }) { device ->
                    if (linkType == ObdLinkType.CLASSIC_SPP || ObdBleHelper.looksLikeObdBleName(device.name)) {
                        DeviceRow(
                            device = device,
                            selected = device.address == selectedAddress,
                            subtitle = device.address,
                            actionLabel = null,
                            onAction = { },
                            onSelect = {
                                scope.launch {
                                    settings.setDeviceAddress(device.address)
                                    selectedAddress = device.address
                                }
                            }
                        )
                    }
                }
            }

            if (linkType == ObdLinkType.CLASSIC_SPP && bondedDevices.isEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.obd_paired_devices))
                }

                if (bondedDevices.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.obd_no_paired_devices),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (selectedAddress == null) {
                        onRequestPermissions()
                        return@Button
                    }
                    if (linkType == ObdLinkType.BLE && !selectedBonded) {
                        pairingMessage = context.getString(R.string.obd_pairing_required)
                        return@Button
                    }
                    if (sessionRunning) {
                        context.startService(
                            Intent(context, TelemetryService::class.java).apply {
                                action = TelemetryService.ACTION_STOP
                            }
                        )
                        sessionRunning = false
                    } else {
                        scope.launch {
                            try {
                                if (TelemetryService.getInstance()?.isSimulationActive() == true) {
                                    context.startService(
                                        Intent(context, TelemetryService::class.java).apply {
                                            action = TelemetryService.ACTION_STOP
                                        }
                                    )
                                    delay(400)
                                }
                                context.startForegroundService(
                                    Intent(context, TelemetryService::class.java).apply {
                                        action = TelemetryService.ACTION_START
                                    }
                                )
                                sessionRunning = true
                            } catch (_: SecurityException) {
                                onRequestPermissions()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAddress != null &&
                    (linkType != ObdLinkType.BLE || selectedBonded || sessionRunning),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionRunning) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            ) {
                Text(
                    if (sessionRunning) stringResource(R.string.stop_session)
                    else stringResource(R.string.start_session)
                )
            }
        }
    }
}

@Composable
private fun LinkTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF1565C0) else Color(0xFF37474F)
        )
    ) {
        Text(label, maxLines = 2)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun DeviceRow(
    device: BluetoothDevice,
    selected: Boolean,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name ?: stringResource(R.string.obd_unknown_device),
                color = if (selected) Color(0xFFFFEB3B) else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (selected) {
            Text("✓", color = Color(0xFFFFEB3B), style = MaterialTheme.typography.titleLarge)
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = Color(0xFF64B5F6),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

private fun deviceSubtitle(
    context: android.content.Context,
    device: BluetoothDevice,
    linkType: ObdLinkType
): String {
    val bond = ObdBluetoothHelper.bondStateLabel(context, device)
    return if (linkType == ObdLinkType.BLE) "$bond · ${device.address}" else bond
}

private fun obdStateLabel(context: android.content.Context, state: ObdConnectionState): String {
    val res = when (state) {
        ObdConnectionState.DISCONNECTED -> R.string.obd_state_disconnected
        ObdConnectionState.SCANNING -> R.string.obd_state_scanning
        ObdConnectionState.CONNECTING -> R.string.obd_state_connecting
        ObdConnectionState.CONNECTED -> R.string.obd_state_connected
        ObdConnectionState.ERROR -> R.string.obd_state_error
    }
    return context.getString(res)
}
