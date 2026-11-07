package com.gtrack.ui.automotive



import android.Manifest

import android.content.Context

import android.content.Intent

import android.content.pm.PackageManager

import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext

import androidx.core.content.ContextCompat

import com.gtrack.telemetry.TelemetryData

import com.gtrack.telemetry.TelemetryService

import com.gtrack.track.TrackState

import kotlinx.coroutines.delay



@Composable

fun rememberTelemetryData(): TelemetryData {

    var data by remember { mutableStateOf(TelemetryService.createIdleSample()) }



    LaunchedEffect(Unit) {

        while (true) {

            val service = TelemetryService.getInstance()

            if (service != null) {
                service.telemetryFlow.collect { sample -> data = sample }
            } else {
                data = TelemetryService.createIdleSample()
                delay(400)
            }

        }

    }

    return data

}



@Composable

fun rememberTrackState(): TrackState {

    var track by remember { mutableStateOf(TrackState()) }

    LaunchedEffect(Unit) {

        while (true) {

            val service = TelemetryService.getInstance()

            if (service != null) {
                service.trackState.collect { track = it }
            } else {
                track = TrackState()
                delay(400)
            }

        }

    }

    return track

}



@Composable

fun rememberTelemetrySessionFlags(): TelemetrySessionFlags {

    var flags by remember { mutableStateOf(TelemetrySessionFlags()) }

    LaunchedEffect(Unit) {

        while (true) {

            val service = TelemetryService.getInstance()

            flags = TelemetrySessionFlags(

                serviceRunning = service != null,

                demoActive = service?.isSimulationActive() == true,

                liveObdActive = service?.isLiveObdSession() == true,

                recording = service?.isRecording() == true

            )

            delay(400)

        }

    }

    return flags

}



data class TelemetrySessionFlags(

    val serviceRunning: Boolean = false,

    val demoActive: Boolean = false,

    val liveObdActive: Boolean = false,

    val recording: Boolean = false

)



fun telemetryPermissions(): List<String> = buildList {

    add(Manifest.permission.ACCESS_FINE_LOCATION)

    if (Build.VERSION.SDK_INT >= 31) {

        add(Manifest.permission.BLUETOOTH_CONNECT)

        add(Manifest.permission.BLUETOOTH_SCAN)

    }

    if (Build.VERSION.SDK_INT >= 33) {

        add(Manifest.permission.POST_NOTIFICATIONS)

    }

}



fun missingTelemetryPermissions(context: Context): List<String> =

    telemetryPermissions().filter {

        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED

    }



fun startDemoSession(context: Context) {

    try {

        context.startForegroundService(

            Intent(context, TelemetryService::class.java).apply {

                action = TelemetryService.ACTION_DEMO

            }

        )

    } catch (_: SecurityException) {

    } catch (_: IllegalStateException) {

    }

}



fun stopTelemetrySession(context: Context) {

    context.startService(

        Intent(context, TelemetryService::class.java).apply {

            action = TelemetryService.ACTION_STOP

        }

    )

}



/** Bascule l'enregistrement CSV (bouton REC). Sans session active, l'appel est ignoré. */

fun toggleTelemetryRecording() {

    TelemetryService.getInstance()?.toggleRecording()

}



@Composable

fun rememberTelemetryPermissionRequest(

    onRequestPermissions: () -> Unit

): () -> Unit {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(

        ActivityResultContracts.RequestMultiplePermissions()

    ) { _ -> }



    return remember {

        {

            val missing = missingTelemetryPermissions(context)

            if (missing.isEmpty()) {

                onRequestPermissions()

            } else {

                launcher.launch(missing.toTypedArray())

            }

        }

    }

}


