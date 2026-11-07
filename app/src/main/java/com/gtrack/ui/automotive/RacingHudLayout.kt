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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtrack.R
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import com.gtrack.widgets.usesTireMonitorUi
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val RacingHudBorder = Color(0xFF2E2E32)
private val RacingHudPageBg = Color(0xFF121212)

@Composable
fun RacingHudPageChrome(
    pageNumber: Int,
    sessionActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RacingHudPageBg)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun RacingHudPageHeader(
    pageNumber: Int,
    sessionActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.racing_hud_page_title, pageNumber),
            color = Color(0xFF8A8A8A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp
        )
        if (sessionActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
                Text(
                    text = stringResource(R.string.racing_hud_rec),
                    color = Color(0xFFE53935),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
fun RacingHudWidgetCell(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    if (widgetType.usesTireMonitorUi()) {
        TireWidgetCell(
            widgetType = widgetType,
            telemetry = telemetry,
            visualStyle = WidgetVisualStyle.RACING_HUD,
            modifier = modifier
        )
        return
    }
    val chrome = visualStylePalette(WidgetVisualStyle.RACING_HUD)
    val parts = widgetType.valueParts(telemetry)
    val pres = widgetType.conceptPresentation(telemetry)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = widgetType.displayTitle().uppercase(),
            color = chrome.label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = parts.main,
                        color = pres.valueColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    parts.unit?.let { unit ->
                        Text(
                            text = unit.lowercase(),
                            color = chrome.valueUnit,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
                        )
                    }
                }

                if (!pres.useGForceMeter) {
                    when {
                        pres.useRpmBars -> RpmHistogramBars(
                            fraction = pres.fraction,
                            activeColor = pres.accentColor,
                            trackColor = chrome.track,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        )
                        pres.useLinearBar -> RacingHudThinBar(
                            fraction = pres.fraction,
                            color = pres.accentColor,
                            trackColor = chrome.track,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (pres.useGForceMeter) {
                RacingHudGForceMeter(
                    gLat = telemetry.lateralGForce,
                    modifier = Modifier
                        .width(76.dp)
                        .height(58.dp)
                )
            } else {
                pres.footerLabel?.let { label ->
                    pres.footerDotColor?.let { dot ->
                        RacingHudFooter(dotColor = dot, label = label)
                    }
                }
            }
        }
    }
}

@Composable
fun RacingHudFooter(
    dotColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = Color(0xFF8A8A8A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun RacingHudThinBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(3.dp)) {
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
fun RacingHudGForceMeter(
    gLat: Float,
    modifier: Modifier = Modifier
) {
    val track = Color(0xFF2A2A2C)
    val accent = Color(0xFFE53935)
    Canvas(modifier = modifier) {
        val cx = size.width * 0.42f
        val cy = size.height * 0.52f
        val r = min(size.width, size.height) * 0.44f
        drawCircle(color = track, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawLine(track, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.2f)
        drawLine(track, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1.2f)
        val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
        val dotX = cx + sin(angle) * r * 0.72f
        val dotY = cy - kotlin.math.cos(angle) * r * 0.72f
        drawCircle(color = accent.copy(alpha = 0.35f), radius = 8f, center = Offset(dotX, dotY))
        drawCircle(color = accent, radius = 5f, center = Offset(dotX, dotY))
    }
}

private fun visualStylePalette(style: WidgetVisualStyle) = style.palette()
