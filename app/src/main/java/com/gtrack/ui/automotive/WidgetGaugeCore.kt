package com.gtrack.ui.automotive

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal data class GaugeLayout(
    val cx: Float,
    val cyDial: Float,
    val cyMid: Float,
    val dialRadius: Float,
    val w: Float,
    val h: Float
) {
    companion object {
        fun from(sizeW: Float, sizeH: Float): GaugeLayout {
            val w = sizeW
            val h = sizeH
            return GaugeLayout(
                cx = w * 0.5f,
                cyDial = h * 0.66f,
                cyMid = h * 0.58f,
                dialRadius = min(w, h) * 0.50f,
                w = w,
                h = h
            )
        }
    }
}

internal fun DrawScope.drawAnalogArcThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float,
    needleColor: Color = palette.needle,
    arcColor: Color = palette.accent
) {
    val sweep = 220f
    val start = 160f
    val arcSize = Size(radius * 2, radius * 2)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    drawArc(
        color = palette.track,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = radius * palette.arcStrokeFactor, cap = StrokeCap.Round)
    )
    val frac = ((value - valueMin) / (valueMax - valueMin)).coerceIn(0f, 1f)
    drawArc(
        color = arcColor,
        startAngle = start,
        sweepAngle = sweep * frac,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = radius * palette.arcStrokeFactor * 0.9f, cap = StrokeCap.Round)
    )
    val angleRad = Math.toRadians((start + sweep * frac).toDouble()).toFloat()
    val nx = cx + cos(angleRad) * radius * 0.78f
    val ny = cy + sin(angleRad) * radius * 0.78f
    drawLine(
        color = needleColor,
        start = Offset(cx, cy),
        end = Offset(nx, ny),
        strokeWidth = radius * palette.thinStrokeFactor,
        cap = StrokeCap.Round
    )
    drawCircle(color = needleColor, radius = radius * palette.thinStrokeFactor * 1.2f, center = Offset(cx, cy))
}

internal fun DrawScope.drawRpmSegmentsThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    rpm: Int
) {
    val segments = 12
    val gap = 4f
    val segW = (width - gap * (segments - 1)) / segments
    val left = cx - width / 2f
    val top = cy - height / 2f
    val filled = (rpm.toFloat() / CarVisualTheme.RPM_REDLINE * segments).toInt().coerceIn(0, segments)
    for (i in 0 until segments) {
        val color = when {
            i >= segments - 2 -> palette.segmentRed
            i >= segments - 4 -> palette.segmentYellow
            else -> palette.segmentGreen
        }
        drawRoundRect(
            color = if (i < filled) color else palette.track,
            topLeft = Offset(left + i * (segW + gap), top),
            size = Size(segW, height),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

internal fun DrawScope.drawThermometerThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    height: Float,
    temp: Float,
    oil: Boolean
) {
    val bulbR = height * 0.14f
    val tubeW = height * 0.12f
    val top = cy - height / 2f
    val bottom = cy + height / 2f
    val tubeTop = top + bulbR
    val tubeBottom = bottom - bulbR
    drawRoundRect(
        color = palette.track,
        topLeft = Offset(cx - tubeW / 2f, tubeTop),
        size = Size(tubeW, tubeBottom - tubeTop),
        cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f)
    )
    drawCircle(color = palette.track, radius = bulbR, center = Offset(cx, bottom - bulbR))
    val fillFrac = (temp / if (oil) 130f else 120f).coerceIn(0f, 1f)
    val fillColor = if (oil && temp >= CarVisualTheme.OIL_TEMP_WARNING_C) {
        palette.segmentYellow
    } else {
        palette.accent.copy(alpha = 0.85f)
    }
    val fillTop = tubeBottom - (tubeBottom - tubeTop) * fillFrac
    drawRoundRect(
        color = fillColor,
        topLeft = Offset(cx - tubeW / 2f + 2f, fillTop),
        size = Size(tubeW - 4f, tubeBottom - fillTop),
        cornerRadius = CornerRadius(tubeW / 3f, tubeW / 3f)
    )
    drawCircle(color = fillColor, radius = bulbR * 0.85f, center = Offset(cx, bottom - bulbR))
}

internal fun DrawScope.drawGForceMeterThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    gLat: Float
) {
    drawCircle(
        color = palette.track,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3f)
    )
    drawLine(
        palette.track,
        Offset(cx - radius, cy),
        Offset(cx + radius, cy),
        strokeWidth = 1.5f
    )
    drawLine(
        palette.track,
        Offset(cx, cy - radius),
        Offset(cx, cy + radius),
        strokeWidth = 1.5f
    )
    val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
    drawLine(
        color = palette.accent,
        start = Offset(cx, cy),
        end = Offset(cx + sin(angle) * radius * 0.85f, cy - cos(angle) * radius * 0.85f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
}

/** Cadran analogique GTI : cadran, graduations, arc lumineux et aiguille. */
internal fun DrawScope.drawGtiAnalogDial(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float,
    arcColor: Color = palette.accent
) {
    val sweep = 220f
    val start = 160f
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    val strokeW = radius * 0.11f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF242424), Color(0xFF141414), Color(0xFF080808)),
            center = Offset(cx, cy - radius * 0.12f),
            radius = radius * 1.1f
        ),
        radius = radius * 0.74f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFF454545),
        radius = radius * 0.9f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.035f)
    )
    drawCircle(
        color = Color(0xFF181818),
        radius = radius * 0.86f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.018f)
    )

    val tickCount = 22
    for (i in 0..tickCount) {
        val frac = i.toFloat() / tickCount
        val angleRad = Math.toRadians((start + sweep * frac).toDouble()).toFloat()
        val major = i % 2 == 0
        val inner = radius * if (major) 0.58f else 0.64f
        val outer = radius * if (major) 0.80f else 0.72f
        drawLine(
            color = if (major) Color(0xFF707070) else Color(0xFF3A3A3A),
            start = Offset(cx + cos(angleRad) * inner, cy + sin(angleRad) * inner),
            end = Offset(cx + cos(angleRad) * outer, cy + sin(angleRad) * outer),
            strokeWidth = if (major) 2.2f else 1.2f,
            cap = StrokeCap.Round
        )
    }

    drawArc(
        color = Color(0xFF111111),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Butt)
    )
    drawArc(
        color = palette.track,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeW * 0.72f, cap = StrokeCap.Round)
    )

    val frac = ((value - valueMin) / (valueMax - valueMin)).coerceIn(0f, 1f)
    if (frac > 0.01f) {
        drawArc(
            color = arcColor.copy(alpha = 0.3f),
            startAngle = start,
            sweepAngle = sweep * frac,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeW * 1.25f, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    arcColor.copy(alpha = 0.55f),
                    arcColor,
                    arcColor.copy(alpha = 0.9f)
                ),
                center = Offset(cx, cy)
            ),
            startAngle = start,
            sweepAngle = sweep * frac,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeW * 0.58f, cap = StrokeCap.Round)
        )
        val tipAngleRad = Math.toRadians((start + sweep * frac).toDouble()).toFloat()
        val tipR = radius * 0.9f
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = strokeW * 0.32f,
            center = Offset(cx + cos(tipAngleRad) * tipR, cy + sin(tipAngleRad) * tipR)
        )
    }

    val needleAngleRad = Math.toRadians((start + sweep * frac).toDouble()).toFloat()
    val needleLen = radius * 0.7f
    val nx = cx + cos(needleAngleRad) * needleLen
    val ny = cy + sin(needleAngleRad) * needleLen
    drawLine(
        color = Color.Black.copy(alpha = 0.45f),
        start = Offset(cx + 1.5f, cy + 1.5f),
        end = Offset(nx + 1.5f, ny + 1.5f),
        strokeWidth = radius * 0.042f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.needle,
        start = Offset(cx, cy),
        end = Offset(nx, ny),
        strokeWidth = radius * 0.032f,
        cap = StrokeCap.Round
    )
    val backAngle = needleAngleRad + PI.toFloat()
    drawLine(
        color = palette.needle.copy(alpha = 0.55f),
        start = Offset(cx, cy),
        end = Offset(
            cx + cos(backAngle) * radius * 0.16f,
            cy + sin(backAngle) * radius * 0.16f
        ),
        strokeWidth = radius * 0.048f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFF2C2C2C), radius = radius * 0.095f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF5A5A5A), radius = radius * 0.065f, center = Offset(cx, cy))
    drawCircle(color = arcColor, radius = radius * 0.032f, center = Offset(cx, cy))
}

/** G latéral GTI : un point lumineux qui se déplace (sans trait). */
internal fun DrawScope.drawGtiGForceDot(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    gLat: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF222222), Color(0xFF0C0C0C)),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius * 0.94f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFF3D3D3D),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = palette.track.copy(alpha = 0.45f),
        radius = radius * 0.52f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )
    val cross = palette.track.copy(alpha = 0.22f)
    drawLine(cross, Offset(cx - radius * 0.88f, cy), Offset(cx + radius * 0.88f, cy), strokeWidth = 1f)
    drawLine(cross, Offset(cx, cy - radius * 0.88f), Offset(cx, cy + radius * 0.88f), strokeWidth = 1f)

    val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
    val dotCenter = Offset(
        cx + sin(angle) * radius * 0.68f,
        cy - cos(angle) * radius * 0.68f
    )
    val dotR = radius * 0.1f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.accent.copy(alpha = 0.55f), Color.Transparent),
            center = dotCenter,
            radius = dotR * 3.2f
        ),
        radius = dotR * 2.8f,
        center = dotCenter
    )
    drawCircle(color = palette.accent, radius = dotR, center = dotCenter)
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = dotR * 0.38f,
        center = Offset(dotCenter.x - dotR * 0.22f, dotCenter.y - dotR * 0.22f)
    )
}

internal fun DrawScope.drawGtiRpmSegments(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    rpm: Int
) {
    val segments = 12
    val gap = 5f
    val segW = (width - gap * (segments - 1)) / segments
    val left = cx - width / 2f
    val top = cy - height / 2f
    val filled = (rpm.toFloat() / CarVisualTheme.RPM_REDLINE * segments).toInt().coerceIn(0, segments)
    val trayPad = 8f
    drawRoundRect(
        color = Color(0xFF101010),
        topLeft = Offset(left - trayPad, top - trayPad),
        size = Size(width + trayPad * 2f, height + trayPad * 2f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0xFF333333),
        topLeft = Offset(left - trayPad, top - trayPad),
        size = Size(width + trayPad * 2f, height + trayPad * 2f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 1.5f)
    )
    for (i in 0 until segments) {
        val color = when {
            i >= segments - 2 -> palette.segmentRed
            i >= segments - 4 -> palette.segmentYellow
            else -> palette.segmentGreen
        }
        val segLeft = left + i * (segW + gap)
        if (i < filled) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.55f))
                ),
                topLeft = Offset(segLeft, top),
                size = Size(segW, height),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(segLeft + 1f, top + 1f),
                size = Size(segW - 2f, height * 0.28f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        } else {
            drawRoundRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(segLeft, top),
                size = Size(segW, height),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
    }
}

internal fun DrawScope.drawGtiThermometer(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    height: Float,
    temp: Float,
    oil: Boolean
) {
    val bulbR = height * 0.15f
    val tubeW = height * 0.14f
    val top = cy - height / 2f
    val bottom = cy + height / 2f
    val tubeTop = top + bulbR
    val tubeBottom = bottom - bulbR
    val fillFrac = (temp / if (oil) 130f else 120f).coerceIn(0f, 1f)
    val fillColor = if (oil && temp >= CarVisualTheme.OIL_TEMP_WARNING_C) {
        palette.segmentYellow
    } else {
        palette.accent.copy(alpha = 0.9f)
    }

    drawRoundRect(
        color = Color(0xFF141414),
        topLeft = Offset(cx - tubeW * 0.75f - 14f, tubeTop - 6f),
        size = Size(tubeW * 1.5f + 28f, tubeBottom - tubeTop + bulbR + 12f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    for (i in 0..4) {
        val y = tubeBottom - (tubeBottom - tubeTop) * i / 4f
        drawLine(
            color = Color(0xFF4A4A4A),
            start = Offset(cx - tubeW * 0.75f - 8f, y),
            end = Offset(cx - tubeW * 0.75f - 2f, y),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
        ),
        topLeft = Offset(cx - tubeW / 2f, tubeTop),
        size = Size(tubeW, tubeBottom - tubeTop),
        cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f)
    )
    drawCircle(
        color = Color(0xFF2A2A2A),
        radius = bulbR,
        center = Offset(cx, bottom - bulbR)
    )
    val fillTop = tubeBottom - (tubeBottom - tubeTop) * fillFrac
    if (fillFrac > 0.02f) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(fillColor.copy(alpha = 0.7f), fillColor)
            ),
            topLeft = Offset(cx - tubeW / 2f + 2f, fillTop),
            size = Size(tubeW - 4f, tubeBottom - fillTop),
            cornerRadius = CornerRadius(tubeW / 3f, tubeW / 3f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fillColor, fillColor.copy(alpha = 0.75f))
            ),
            radius = bulbR * 0.88f,
            center = Offset(cx, bottom - bulbR)
        )
    }
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(cx - tubeW / 2f + 2f, tubeTop + 2f),
        size = Size(tubeW * 0.22f, (tubeBottom - tubeTop) * 0.55f),
        cornerRadius = CornerRadius(2f, 2f)
    )
}

internal fun DrawScope.drawAfrBarThemed(palette: WidgetThemePalette, w: Float, h: Float, afr: Float) {
    val bar = Rect(w * 0.06f, h * 0.44f, w * 0.94f, h * 0.68f)
    val third = bar.width / 3f
    drawRect(palette.segmentRed, topLeft = Offset(bar.left, bar.top), size = Size(third, bar.height))
    drawRect(palette.segmentGreen, topLeft = Offset(bar.left + third, bar.top), size = Size(third, bar.height))
    drawRect(palette.accent.copy(alpha = 0.7f), topLeft = Offset(bar.left + 2 * third, bar.top), size = Size(third, bar.height))
    val markerX = bar.left + ((afr - 10f) / 8f).coerceIn(0f, 1f) * bar.width
    drawLine(palette.needle, Offset(markerX, bar.top - 8f), Offset(markerX, bar.bottom + 8f), strokeWidth = 4f)
}

internal fun DrawScope.drawVerticalBarThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    height: Float,
    percent: Float
) {
    val barW = height * 0.22f
    val top = cy - height / 2f
    val bottom = cy + height / 2f
    drawRoundRect(
        color = palette.track,
        topLeft = Offset(cx - barW / 2f, top),
        size = Size(barW, bottom - top),
        cornerRadius = CornerRadius(8f, 8f)
    )
    val filled = (bottom - top) * (percent / 100f)
    drawRoundRect(
        color = palette.accent,
        topLeft = Offset(cx - barW / 2f, bottom - filled),
        size = Size(barW, filled),
        cornerRadius = CornerRadius(8f, 8f)
    )
}

internal fun DrawScope.drawDeltaBadgeThemed(palette: WidgetThemePalette, cx: Float, cy: Float, deltaMs: Long) {
    val slow = deltaMs > 0
    val color = if (slow) palette.segmentRed else palette.segmentGreen
    val r = min(size.width, size.height) * 0.38f
    drawCircle(color = color.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy))
    drawCircle(color = color, radius = r * 0.65f, center = Offset(cx, cy), style = Stroke(width = 4f))
}

internal fun DrawScope.drawO2IndicatorThemed(palette: WidgetThemePalette, cx: Float, cy: Float, volts: Float) {
    val lambda = 0.7f + volts * 0.35f
    val color = when {
        lambda < 0.95f -> palette.segmentRed
        lambda > 1.05f -> palette.accent.copy(alpha = 0.7f)
        else -> palette.segmentGreen
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.15f))),
        radius = min(size.width, size.height) * 0.42f,
        center = Offset(cx, cy)
    )
}

internal fun DrawScope.drawFallbackGaugeThemed(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    widgetType: WidgetType,
    telemetry: TelemetryData
) {
    val frac = when (widgetType) {
        WidgetType.IGNITION -> telemetry.ignitionAdvanceBtdc / 40f
        else -> 0.55f
    }.coerceIn(0.15f, 0.9f)
    drawArc(
        color = palette.accent.copy(alpha = 0.6f),
        startAngle = 200f,
        sweepAngle = 140f * frac,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = radius * palette.arcStrokeFactor, cap = StrokeCap.Round)
    )
}

internal fun DrawScope.drawLcdPanel(
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    fillFraction: Float
) {
    val panelW = layout.w * 0.82f
    val panelH = layout.h * 0.42f
    val left = layout.cx - panelW / 2f
    val top = layout.cyMid - panelH / 2f
    drawRoundRect(
        color = palette.track,
        topLeft = Offset(left, top),
        size = Size(panelW, panelH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = palette.track.copy(alpha = 0.5f),
        topLeft = Offset(left + 4f, top + 4f),
        size = Size(panelW - 8f, panelH - 8f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 2f)
    )
    val barH = panelH * 0.28f
    val barTop = top + panelH * 0.55f
    val barLeft = left + 10f
    val barWidth = panelW - 20f
    drawRoundRect(
        color = palette.track.copy(alpha = 0.8f),
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth, barH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = palette.accent,
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth * fillFraction.coerceIn(0f, 1f), barH),
        cornerRadius = CornerRadius(3f, 3f)
    )
}

internal fun DrawScope.drawVintageDialFace(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val faceColor = Color(0xFFE8DCC4)
    drawCircle(color = faceColor, radius = radius, center = Offset(cx, cy))
    drawCircle(color = palette.track, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))
    for (i in 0 until 12) {
        val angle = Math.toRadians((180 + i * 15).toDouble()).toFloat()
        val inner = radius * 0.78f
        val outer = radius * 0.92f
        drawLine(
            color = palette.track,
            start = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner),
            end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
            strokeWidth = 2f
        )
    }
}

internal fun DrawScope.drawHudRpmLine(
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    rpm: Int
) {
    val y = layout.cyMid
    val left = layout.w * 0.08f
    val right = layout.w * 0.92f
    val width = right - left
    drawLine(palette.track, Offset(left, y), Offset(right, y), strokeWidth = 2f)
    val frac = (rpm.toFloat() / CarVisualTheme.RPM_REDLINE).coerceIn(0f, 1f)
    drawLine(palette.segmentGreen, Offset(left, y), Offset(left + width * frac, y), strokeWidth = 4f, cap = StrokeCap.Round)
    for (i in 0..10) {
        val x = left + width * i / 10f
        drawLine(
            palette.track.copy(alpha = 0.8f),
            Offset(x, y - 10f),
            Offset(x, y + 10f),
            strokeWidth = 1.5f
        )
    }
}

internal fun DrawScope.drawHudTempBar(
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    temp: Float,
    maxTemp: Float
) {
    val barW = layout.w * 0.78f
    val barH = layout.h * 0.12f
    val left = layout.cx - barW / 2f
    val top = layout.cyMid - barH / 2f
    drawRoundRect(
        color = palette.track,
        topLeft = Offset(left, top),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val frac = (temp / maxTemp).coerceIn(0f, 1f)
    drawRoundRect(
        color = palette.accent,
        topLeft = Offset(left, top),
        size = Size(barW * frac, barH),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

internal fun DrawScope.drawNightDoubleArc(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float
) {
    drawOval(
        color = palette.accent.copy(alpha = 0.35f),
        topLeft = Offset(cx - radius * 1.1f, cy - radius * 0.65f),
        size = Size(radius * 2.2f, radius * 1.3f),
        style = Stroke(width = 2f)
    )
    drawAnalogArcThemed(palette, cx, cy, radius * 0.9f, value, valueMin, valueMax)
}

internal val NeonBlueAccent = Color(0xFF448AFF)

/** Anneau néon avec piste, arc lumineux et valeur centrée (sans aiguille). */
internal fun DrawScope.drawNeonRingGauge(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float,
    displayValue: String,
    displayUnit: String?,
    arcColor: Color = palette.accent,
    startAngle: Float = 135f,
    sweepTotal: Float = 270f
) {
    val frac = ((value - valueMin) / (valueMax - valueMin)).coerceIn(0f, 1f)
    drawNeonRingTrack(palette, cx, cy, radius, arcColor)
    drawNeonProgressArc(cx, cy, radius, frac, arcColor, startAngle, sweepTotal)
    drawNeonCenterText(cx, cy, radius, displayValue, displayUnit, palette)
}

internal fun DrawScope.drawNeonRingTrack(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    arcColor: Color,
    startAngle: Float = 135f,
    sweepTotal: Float = 270f
) {
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    val trackStroke = radius * 0.13f

    drawCircle(
        color = arcColor.copy(alpha = 0.1f),
        radius = radius * 1.08f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.06f)
    )
    drawCircle(
        color = arcColor.copy(alpha = 0.28f),
        radius = radius * 1.02f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.8f)
    )
    drawArc(
        color = palette.track.copy(alpha = 0.9f),
        startAngle = startAngle,
        sweepAngle = sweepTotal,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = trackStroke, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF0D1528),
        startAngle = startAngle,
        sweepAngle = sweepTotal,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = trackStroke * 0.55f, cap = StrokeCap.Round)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF141E32), Color(0xFF080E18), Color(0xFF050810)),
            center = Offset(cx, cy - radius * 0.08f),
            radius = radius * 0.85f
        ),
        radius = radius * 0.7f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = arcColor.copy(alpha = 0.12f),
        radius = radius * 0.52f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f)
    )
}

internal fun DrawScope.drawNeonProgressArc(
    cx: Float,
    cy: Float,
    radius: Float,
    frac: Float,
    arcColor: Color,
    startAngle: Float = 135f,
    sweepTotal: Float = 270f
) {
    if (frac <= 0.005f) return
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    val trackStroke = radius * 0.13f
    val progressSweep = sweepTotal * frac
    val glowLayers = listOf(
        trackStroke * 2.4f to 0.07f,
        trackStroke * 1.7f to 0.16f,
        trackStroke * 1.15f to 0.32f,
        trackStroke * 0.68f to 1f
    )
    glowLayers.forEach { (width, alpha) ->
        drawArc(
            color = arcColor.copy(alpha = alpha),
            startAngle = startAngle,
            sweepAngle = progressSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = width, cap = StrokeCap.Round)
        )
    }
    val tipRad = Math.toRadians((startAngle + progressSweep).toDouble()).toFloat()
    val tipX = cx + cos(tipRad) * radius
    val tipY = cy + sin(tipRad) * radius
    drawCircle(color = arcColor.copy(alpha = 0.45f), radius = trackStroke * 0.55f, center = Offset(tipX, tipY))
    drawCircle(color = Color.White, radius = trackStroke * 0.22f, center = Offset(tipX, tipY))
}

internal fun DrawScope.drawNeonCenterText(
    cx: Float,
    cy: Float,
    radius: Float,
    value: String,
    unit: String?,
    palette: WidgetThemePalette
) {
    drawContext.canvas.nativeCanvas.apply {
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valueY = cy + valuePaint.textSize * 0.14f
        drawText(value, cx, valueY, valuePaint)
        unit?.let {
            val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.valueUnit.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = radius * 0.19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            drawText(it, cx, valueY + radius * 0.26f, unitPaint)
        }
    }
}

/** G latéral néon : anneau + point mobile + valeur au centre. */
internal fun DrawScope.drawNeonGForceDot(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    gLat: Float
) {
    val arcColor = palette.accent
    drawNeonRingTrack(palette, cx, cy, radius, arcColor)
    val magFrac = (kotlin.math.abs(gLat) / 2f).coerceIn(0f, 1f)
    drawNeonProgressArc(cx, cy, radius, magFrac, arcColor)

    val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
    val dotCenter = Offset(
        cx + sin(angle) * radius * 0.76f,
        cy - cos(angle) * radius * 0.76f
    )
    val dotR = radius * 0.09f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(arcColor.copy(alpha = 0.6f), Color.Transparent),
            center = dotCenter,
            radius = dotR * 3.5f
        ),
        radius = dotR * 3f,
        center = dotCenter
    )
    drawCircle(color = arcColor, radius = dotR, center = dotCenter)
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = dotR * 0.38f,
        center = Offset(dotCenter.x - dotR * 0.2f, dotCenter.y - dotR * 0.2f)
    )
    drawNeonCenterText(cx, cy, radius * 0.62f, "%.1f".format(gLat), "G", palette)
}
