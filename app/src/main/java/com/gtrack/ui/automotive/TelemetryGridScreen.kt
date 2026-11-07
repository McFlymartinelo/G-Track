package com.gtrack.ui.automotive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtrack.telemetry.TelemetryData
import com.gtrack.track.TrackState
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle

private val RacingHudCardBorder = androidx.compose.ui.graphics.Color(0xFF2E2E32)

/**
 * Grille 3×2 plein écran — widgets Compose (titre, jauge, valeur).
 */
@Composable
fun TelemetryGridScreen(
    widgets: List<WidgetType>,
    telemetry: TelemetryData,
    track: TrackState,
    visualStyle: WidgetVisualStyle,
    edgeToEdge: Boolean = false,
    pageNumber: Int = 1,
    sessionActive: Boolean = false,
    modifier: Modifier = Modifier,
    onCellLongPress: (Int) -> Unit
) {
    val slotWidgets = widgets.take(WidgetType.WIDGETS_PER_PAGE)
    val isRacingHud = visualStyle == WidgetVisualStyle.RACING_HUD
    val isGtiTrack = visualStyle == WidgetVisualStyle.GTI
    val isEpure = visualStyle == WidgetVisualStyle.EPURE
    val isFlatGrid = isEpure || isGtiTrack
    val outerPadH = when {
        isFlatGrid -> 0.dp
        isRacingHud -> 10.dp
        edgeToEdge -> 4.dp
        else -> 8.dp
    }
    val outerPadV = when {
        isFlatGrid -> 0.dp
        isRacingHud -> 8.dp
        edgeToEdge -> 2.dp
        else -> 6.dp
    }
    val rowGap = when {
        isFlatGrid -> 1.dp
        isRacingHud -> 6.dp
        edgeToEdge -> 4.dp
        else -> 8.dp
    }
    val colGap = if (isFlatGrid) 1.dp else 8.dp

    val grid = @Composable {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = outerPadH, vertical = outerPadV),
            verticalArrangement = Arrangement.spacedBy(rowGap)
        ) {
            GridRow(
                slotIndices = listOf(0, 1, 2),
                slotWidgets = slotWidgets,
                telemetry = telemetry,
                visualStyle = visualStyle,
                columnGap = colGap,
                onCellLongPress = onCellLongPress,
                modifier = Modifier.weight(1f)
            )
            GridRow(
                slotIndices = listOf(3, 4, 5),
                slotWidgets = slotWidgets,
                telemetry = telemetry,
                visualStyle = visualStyle,
                columnGap = colGap,
                onCellLongPress = onCellLongPress,
                modifier = Modifier.weight(1f)
            )
        }
    }

    when {
        isEpure -> EpurePageChrome(
            sessionActive = sessionActive,
            modifier = modifier
        ) {
            grid()
        }
        isGtiTrack -> GtiTrackPageChrome(
            pageNumber = pageNumber,
            sessionActive = sessionActive,
            modifier = modifier
        ) {
            grid()
        }
        isRacingHud -> RacingHudPageChrome(
            pageNumber = pageNumber,
            sessionActive = sessionActive,
            modifier = modifier
        ) {
            grid()
        }
        else -> Box(modifier = modifier.fillMaxSize()) {
            grid()
        }
    }
}

@Composable
private fun GridRow(
    slotIndices: List<Int>,
    slotWidgets: List<WidgetType>,
    telemetry: TelemetryData,
    visualStyle: WidgetVisualStyle,
    columnGap: androidx.compose.ui.unit.Dp = 8.dp,
    onCellLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(columnGap)
    ) {
        slotIndices.forEach { index ->
            val widget = slotWidgets.getOrElse(index) { WidgetType.SPEED }
            WidgetGridCell(
                widgetType = widget,
                telemetry = telemetry,
                visualStyle = visualStyle,
                onLongPress = { onCellLongPress(index + 1) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetGridCell(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    visualStyle: WidgetVisualStyle,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRacingHud = visualStyle == WidgetVisualStyle.RACING_HUD
    val isGtiTrack = visualStyle == WidgetVisualStyle.GTI
    val isEpure = visualStyle == WidgetVisualStyle.EPURE
    val isFlatCard = isEpure || isGtiTrack
    val shape = RoundedCornerShape(
        when {
            isFlatCard -> 0.dp
            isRacingHud -> 12.dp
            else -> 14.dp
        }
    )
    val chrome = visualStyle.palette()
    val borderColor = when {
        isRacingHud -> RacingHudCardBorder
        chrome.useValueBorder -> widgetType.borderColor(telemetry)
        else -> chrome.accent.copy(alpha = 0.35f)
    }
    val valueParts = widgetType.valueParts(telemetry)
    val valueColor = widgetType.valueColor(telemetry)

    val cellBackground = if (isFlatCard) {
        Modifier.background(EpureColors.BgCard)
    } else {
        Modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(chrome.cellGradientTop, chrome.cellGradientBottom)
                )
            )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(cellBackground)
            .then(
                if (!isFlatCard && chrome.borderWidthDp > 0f) {
                    Modifier.border(chrome.borderWidthDp.dp, borderColor.copy(alpha = 0.9f), shape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = { }, onLongClick = onLongPress)
            .then(
                if (isFlatCard) Modifier else Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
    ) {
        if (visualStyle.usesConceptLayout()) {
            ConceptWidgetCell(
                widgetType = widgetType,
                telemetry = telemetry,
                visualStyle = visualStyle,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = widgetType.displayTitle(),
                    color = chrome.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetGaugeCanvas(
                        widgetType = widgetType,
                        telemetry = telemetry,
                        visualStyle = visualStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(
                                if (chrome.embedValueInGauge || chrome.embedIconInGauge) 1f else 0.92f
                            )
                            .padding(
                                top = if (chrome.embedValueInGauge || chrome.embedIconInGauge) 2.dp else 6.dp
                            )
                    )
                }

                if (!chrome.embedValueInGauge && !chrome.embedIconInGauge) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = valueParts.main,
                            color = valueColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        valueParts.unit?.let { unit ->
                            Text(
                                text = unit,
                                color = chrome.valueUnit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
