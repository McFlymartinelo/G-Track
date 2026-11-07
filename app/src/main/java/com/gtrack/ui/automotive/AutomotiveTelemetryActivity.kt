package com.gtrack.ui.automotive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.gtrack.obd.ObdConnectionState
import com.gtrack.telemetry.TelemetryService
import com.gtrack.ui.TelemetryDashboardScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Tableau de bord AAOS (grille 3×2). */
class AutomotiveTelemetryActivity : ComponentActivity() {

    private var connectionState by mutableStateOf<ObdConnectionState?>(null)
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF0A0A0A)
                )
            ) {
                TelemetryDashboardScreen(
                    obdConnectionState = connectionState,
                    onRequestPermissions = { }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startObdPolling()
    }

    override fun onStop() {
        pollJob?.cancel()
        pollJob = null
        super.onStop()
    }

    private fun startObdPolling() {
        pollJob?.cancel()
        pollJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                connectionState = TelemetryService.getInstance()
                    ?.getObdConnector()
                    ?.connectionState
                    ?.value
                delay(400)
            }
        }
    }
}
