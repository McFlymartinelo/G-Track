package com.gtrack.ui.automotive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import com.gtrack.widgets.usesTireMonitorUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ConceptWidgetCell(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    visualStyle: WidgetVisualStyle,
    modifier: Modifier = Modifier
) {
    when (visualStyle) {
        WidgetVisualStyle.GTI -> GtiTrackWidgetCell(widgetType, telemetry, modifier)
        WidgetVisualStyle.EPURE -> EpureWidgetCell(widgetType, telemetry, modifier)
        WidgetVisualStyle.RACING_HUD -> RacingHudWidgetCell(widgetType, telemetry, modifier)
        WidgetVisualStyle.STRUCTURED -> StructuredWidgetCell(widgetType, telemetry, modifier)
        else -> Unit
    }
}

@Composable
fun StructuredWidgetCell(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    if (widgetType.usesTireMonitorUi()) {
        TireWidgetCell(
            widgetType = widgetType,
            telemetry = telemetry,
            visualStyle = WidgetVisualStyle.STRUCTURED,
            modifier = modifier
        )
        return
    }
    val chrome = visualStylePalette(WidgetVisualStyle.STRUCTURED)
    val parts = widgetType.valueParts(telemetry)
    val pres = widgetType.conceptPresentation(telemetry)
    val iconTint = pres.accentColor.copy(alpha = 0.25f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = widgetType.displayTitle(),
                color = chrome.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(widgetType.iconResId()),
                    contentDescription = null,
                    tint = pres.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = parts.main,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                parts.unit?.let { unit ->
                    Text(
                        text = unit,
                        color = chrome.label,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                    )
                }
            }
            pres.subtitle?.let { sub ->
                Text(
                    text = sub,
                    color = pres.statusColor ?: chrome.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when {
            pres.useRpmBars -> RpmHistogramBars(
                fraction = pres.fraction,
                activeColor = pres.accentColor,
                trackColor = chrome.track,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
            pres.useGForceMeter -> GForceCrosshair(
                gLat = telemetry.lateralGForce,
                accent = pres.accentColor,
                track = chrome.track,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
            pres.useLinearBar -> ThinProgressBar(
                fraction = pres.fraction,
                color = pres.accentColor,
                trackColor = chrome.track,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ThinProgressBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.height(4.dp)
    ) {
        val w = size.width
        val h = size.height
        drawLine(trackColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = h, cap = StrokeCap.Round)
        drawLine(
            color,
            Offset(0f, h / 2f),
            Offset(w * fraction.coerceIn(0f, 1f), h / 2f),
            strokeWidth = h,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun RpmHistogramBars(
    fraction: Float,
    activeColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val segments = 10
    val filled = (fraction * segments).toInt().coerceIn(0, segments)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until segments) {
            val active = i < filled
            val heightFrac = 0.35f + (i + 1) * 0.065f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFrac.coerceAtMost(1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) activeColor else trackColor)
            )
        }
    }
}

@Composable
fun GForceCrosshair(
    gLat: Float,
    accent: Color,
    track: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(size.width, size.height) * 0.42f
        drawCircle(color = track, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawLine(track, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.5f)
        drawLine(track, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1.5f)
        val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
        val dotX = cx + sin(angle) * r * 0.75f
        val dotY = cy - cos(angle) * r * 0.75f
        drawCircle(color = accent, radius = 6f, center = Offset(dotX, dotY))
    }
}

private fun visualStylePalette(style: WidgetVisualStyle) = style.palette()
