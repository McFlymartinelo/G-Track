package com.gtrack.ui.automotive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Jauge semi-circulaire 180° (arc en haut) + valeur en dessous (sans chevauchement).
 */
@Composable
fun TirePressureGauge(
    pressureBar: Float,
    status: TirePressureStatus,
    scale: TireWidgetScale,
    modifier: Modifier = Modifier
) {
    val cfg = TireGaugeConfig

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val stroke = scale.arcStrokeDp * density
            val inset = stroke / 2f + 2f
            val arcZoneH = size.height * 0.92f
            val availW = size.width - inset * 2f
            val diameter = min(availW, arcZoneH * 2f)
            val radius = diameter / 2f
            val cx = size.width / 2f
            val cy = inset + radius
            val topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius)
            val arcSize = Size(diameter, diameter)
            val arcStyle = Stroke(width = stroke, cap = StrokeCap.Round)

            val arcStart = 180f
            val arcSweep = 180f

            drawArc(
                color = TireGaugeColors.trackBg,
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = arcStyle
            )

            fun drawZone(minBar: Float, maxBar: Float, color: androidx.compose.ui.graphics.Color) {
                val (start, sweep) = cfg.arcSegment(minBar, maxBar)
                if (sweep <= 0f) return
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = arcStyle
                )
            }
            drawZone(cfg.SCALE_MIN_BAR, cfg.TARGET_MIN_BAR, TireGaugeColors.zoneRed())
            drawZone(cfg.TARGET_MIN_BAR, cfg.TARGET_MAX_BAR, TireGaugeColors.zoneGreen())
            drawZone(cfg.TARGET_MAX_BAR, cfg.SCALE_MAX_BAR, TireGaugeColors.zoneRed())

            val fillSweep = cfg.fillSweep(pressureBar)
            if (fillSweep > 0.5f) {
                drawArc(
                    color = status.color,
                    startAngle = arcStart,
                    sweepAngle = fillSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = arcStyle
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (scale.compact) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "%.2f".format(pressureBar),
                style = TextStyle(
                    color = status.color,
                    fontSize = scale.pressureFont,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
            Text(
                text = " bar",
                color = TireGaugeColors.labelSecondary,
                fontSize = scale.pressureUnitFont,
                modifier = Modifier.padding(bottom = if (scale.compact) 1.dp else 3.dp)
            )
        }
    }
}
