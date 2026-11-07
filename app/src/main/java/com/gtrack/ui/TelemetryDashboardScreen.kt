package com.gtrack.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.auto.CarWidgetDefaults
import com.gtrack.auto.pages.AutoTelemetryPage
import com.gtrack.obd.ObdConnectionState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gtrack.telemetry.toTelemetryData
import com.gtrack.ui.automotive.EpureDashboardViewModel
import com.gtrack.ui.automotive.LiveMapScreen
import com.gtrack.ui.automotive.TelemetryGridScreen
import com.gtrack.ui.automotive.WidgetPickerOverlay
import com.gtrack.widgets.WidgetVisualStyle
import com.gtrack.ui.automotive.missingTelemetryPermissions
import com.gtrack.ui.automotive.rememberTelemetryData
import com.gtrack.ui.automotive.rememberTelemetrySessionFlags
import com.gtrack.ui.automotive.rememberTrackState
import com.gtrack.ui.automotive.startDemoSession
import com.gtrack.ui.automotive.stopTelemetrySession
import com.gtrack.ui.automotive.toggleTelemetryRecording
import com.gtrack.widgets.WidgetManager
import com.gtrack.widgets.WidgetType
import kotlinx.coroutines.launch
import com.gtrack.telemetry.TelemetryData
import com.gtrack.track.TrackState

private data class PickerTarget(val pageIndex: Int, val slotIndex: Int)

@Composable
fun TelemetryDashboardScreen(
    obdConnectionState: ObdConnectionState?,
    onRequestPermissions: () -> Unit,
    phoneFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var showObdPopup by remember { mutableStateOf(false) }
    var showPresetPopup by remember { mutableStateOf(false) }
    var showThemePopup by remember { mutableStateOf(false) }

    val pageCount = AutoTelemetryPage.PAGES.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val pageIndex = pagerState.currentPage
    val telemetry = rememberTelemetryData()
    val track = rememberTrackState()
    val sessionFlags = rememberTelemetrySessionFlags()
    val context = LocalContext.current
    val widgetManager = remember { WidgetManager(context) }
    val scope = rememberCoroutineScope()

    var pages by remember {
        mutableStateOf(
            listOf(
                CarWidgetDefaults.data1(),
                CarWidgetDefaults.data2()
            )
        )
    }

    LaunchedEffect(Unit) {
        widgetManager.ensureCarTelemetryPages()
        widgetManager.pagesFlow.collect { pages = it }
    }

    val visualStyle by widgetManager.visualStyleFlow.collectAsState(
        initial = WidgetVisualStyle.GTI
    )

    val epureViewModel: EpureDashboardViewModel = viewModel()
    val obdData by epureViewModel.obdData.collectAsState()
    val epureSimActive by epureViewModel.simulationActive.collectAsState()

    val isEpureTheme = visualStyle == WidgetVisualStyle.EPURE
    val displayTelemetry = if (isEpureTheme) {
        obdData.toTelemetryData(telemetry)
    } else {
        telemetry
    }
    val epureSessionActive = sessionFlags.demoActive ||
        sessionFlags.liveObdActive ||
        epureSimActive

    LaunchedEffect(visualStyle, sessionFlags.demoActive, sessionFlags.liveObdActive) {
        if (!isEpureTheme) {
            epureViewModel.stopSimulation()
            return@LaunchedEffect
        }
        val liveSession = sessionFlags.demoActive || sessionFlags.liveObdActive
        if (liveSession) {
            epureViewModel.stopSimulation()
        } else {
            epureViewModel.startSimulation()
        }
    }

    LaunchedEffect(telemetry, isEpureTheme, sessionFlags.demoActive, sessionFlags.liveObdActive) {
        if (isEpureTheme && (sessionFlags.demoActive || sessionFlags.liveObdActive)) {
            epureViewModel.syncFromTelemetry(telemetry)
        }
    }

    LaunchedEffect(isEpureTheme) {
        val activity = context as? Activity ?: return@LaunchedEffect
        activity.requestedOrientation = if (isEpureTheme) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val overlayOpen = pickerTarget != null || showObdPopup || showPresetPopup || showThemePopup

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        val onDemoClick: () -> Unit = {
            if (missingTelemetryPermissions(context).isNotEmpty()) {
                onRequestPermissions()
            } else if (sessionFlags.demoActive) {
                stopTelemetrySession(context)
            } else {
                startDemoSession(context)
            }
        }

        val sessionActive = if (isEpureTheme) {
            epureSessionActive
        } else {
            sessionFlags.demoActive || sessionFlags.liveObdActive
        }

        Column(modifier = Modifier.fillMaxSize()) {
            DashboardTopBar(
                visualStyle = visualStyle,
                pageNumber = pageIndex + 1,
                recording = sessionFlags.recording,
                sessionActive = sessionActive,
                demoActive = sessionFlags.demoActive,
                liveObdActive = sessionFlags.liveObdActive,
                obdConnectionState = obdConnectionState,
                onRecClick = { toggleTelemetryRecording() },
                onObdClick = { showObdPopup = true },
                onThemeClick = { showThemePopup = true },
                onPresetClick = { showPresetPopup = true },
                onDemoClick = onDemoClick
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = !overlayOpen,
                beyondViewportPageCount = 1
            ) { index ->
                val pagerPage = AutoTelemetryPage.fromIndex(index)
                val widgets = pages.getOrElse(index) {
                    CarWidgetDefaults.layoutForPage(pagerPage)
                }
                TelemetryDashboardPageContent(
                    page = pagerPage,
                    widgets = widgets,
                    telemetry = displayTelemetry,
                    track = track,
                    visualStyle = visualStyle,
                    phoneFullscreen = phoneFullscreen,
                    displayPageNumber = index + 1,
                    sessionActive = sessionActive,
                    onCellLongPress = { cellNumber ->
                        pickerTarget = PickerTarget(
                            pageIndex = index,
                            slotIndex = cellNumber - 1
                        )
                    }
                )
            }
        }

        pickerTarget?.let { target ->
            val currentType = pages.getOrNull(target.pageIndex)?.getOrNull(target.slotIndex)
            WidgetPickerOverlay(
                slotIndex = target.slotIndex,
                telemetry = telemetry,
                currentType = currentType,
                onSelect = { type ->
                    scope.launch {
                        widgetManager.replaceWidget(
                            target.pageIndex,
                            target.slotIndex,
                            type
                        )
                        pickerTarget = null
                    }
                },
                onDismiss = { pickerTarget = null }
            )
        }

        if (showObdPopup) {
            ObdConnectionOverlay(
                connectionState = obdConnectionState,
                onRequestPermissions = onRequestPermissions,
                onDismiss = { showObdPopup = false }
            )
        }

        if (showPresetPopup) {
            ObdPresetOverlay(onDismiss = { showPresetPopup = false })
        }

        if (showThemePopup) {
            WidgetThemeOverlay(onDismiss = { showThemePopup = false })
        }
    }
}

@Composable
private fun TelemetryDashboardPageContent(
    page: AutoTelemetryPage,
    widgets: List<WidgetType>,
    telemetry: TelemetryData,
    track: TrackState,
    visualStyle: com.gtrack.widgets.WidgetVisualStyle,
    phoneFullscreen: Boolean,
    displayPageNumber: Int = 1,
    sessionActive: Boolean = false,
    onCellLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (page) {
            AutoTelemetryPage.DATA_1, AutoTelemetryPage.DATA_2 -> {
                TelemetryGridScreen(
                    widgets = widgets,
                    telemetry = telemetry,
                    track = track,
                    visualStyle = visualStyle,
                    edgeToEdge = phoneFullscreen,
                    pageNumber = displayPageNumber,
                    sessionActive = sessionActive,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onCellLongPress = onCellLongPress
                )
                if (!phoneFullscreen &&
                    visualStyle != com.gtrack.widgets.WidgetVisualStyle.GTI &&
                    visualStyle != com.gtrack.widgets.WidgetVisualStyle.EPURE
                ) {
                    Text(
                        text = stringResource(R.string.grid_long_press_hint),
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            AutoTelemetryPage.LIVE_MAP -> {
                LiveMapScreen(
                    telemetry = telemetry,
                    track = track,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

