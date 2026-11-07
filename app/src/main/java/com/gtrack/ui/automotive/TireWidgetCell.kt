package com.gtrack.ui.automotive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.TireCorner
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import com.gtrack.widgets.defaultTireCorner
import com.gtrack.widgets.usesTireMonitorUi
import com.gtrack.widgets.tireWheelSnapshot

@Composable
fun TireWidgetCell(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    @Suppress("UNUSED_PARAMETER") visualStyle: WidgetVisualStyle,
    modifier: Modifier = Modifier
) {
    if (!widgetType.usesTireMonitorUi()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val scale = rememberTireWidgetScale(maxWidth, maxHeight)
        var selectedCorner by remember(widgetType) {
            mutableStateOf(widgetType.defaultTireCorner())
        }
        val snap = widgetType.tireWheelSnapshot(telemetry, selectedCorner)
        val status = TireGaugeConfig.status(snap.pressureBar, snap.tpmsLow)
        val tpmsShort = when {
            scale.compact && status == TirePressureStatus.CRITIQUE -> "!"
            scale.compact && status == TirePressureStatus.ATTENTION -> "?"
            else -> status.label
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = scale.cellPaddingH,
                    vertical = scale.cellPaddingV
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PNEU ${selectedCorner.positionLabel}",
                    color = TireGaugeColors.labelSecondary,
                    fontSize = scale.headerFont,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                TireCornerSelector(
                    selected = selectedCorner,
                    onSelect = { selectedCorner = it },
                    scale = scale
                )
            }

            TirePressureGauge(
                pressureBar = snap.pressureBar,
                status = status,
                scale = scale,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = TireGaugeColors.divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(
                        top = scale.footerPaddingTop,
                        bottom = scale.footerPaddingTop
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = snap.tempText,
                            style = TextStyle(
                                color = TireGaugeColors.textPrimary,
                                fontSize = scale.tempFont,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = " °C",
                            color = TireGaugeColors.labelSecondary,
                            fontSize = scale.tempUnitFont,
                            modifier = Modifier.padding(
                                bottom = if (scale.compact) 1.dp else 3.dp
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (scale.compact) 4.dp else 6.dp
                        )
                    ) {
                        if (!scale.compact) {
                            Text(
                                text = "TPMS",
                                color = TireGaugeColors.labelSecondary,
                                fontSize = scale.tpmsLabelFont,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(scale.tpmsDotSize)
                                .clip(CircleShape)
                                .background(status.color)
                        )
                        Text(
                            text = tpmsShort,
                            color = status.color,
                            fontSize = scale.tpmsStatusFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TireCornerSelector(
    selected: TireCorner,
    onSelect: (TireCorner) -> Unit,
    scale: TireWidgetScale,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(scale.selectorGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TireCorner.entries.forEach { corner ->
            val active = corner == selected
            Box(
                modifier = Modifier
                    .size(scale.selectorSize)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (active) TireGaugeColors.selectorAccent else TireGaugeColors.selectorInactive,
                        shape = CircleShape
                    )
                    .background(
                        if (active) TireGaugeColors.selectorAccent.copy(alpha = 0.12f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(corner) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = corner.selectorLabel,
                    color = if (active) TireGaugeColors.selectorAccent else TireGaugeColors.selectorInactive,
                    fontSize = scale.selectorFont,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
