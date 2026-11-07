package com.gtrack.ui.automotive

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import kotlin.math.min

@Composable
fun WidgetGaugeCanvas(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    visualStyle: WidgetVisualStyle,
    modifier: Modifier = Modifier
) {
    if (visualStyle.usesConceptLayout()) return
    val palette = visualStyle.palette()
    val context = LocalContext.current
    val needsIcon = palette.embedIconInGauge || visualStyle == WidgetVisualStyle.PREMIUM
    val iconBitmap = remember(widgetType, needsIcon) {
        if (needsIcon) loadGaugeIconBitmap(context, widgetType.iconResId()) else null
    }
    Canvas(modifier = modifier) {
        val layout = GaugeLayout.from(size.width, size.height)
        drawWidgetGauge(widgetType, telemetry, visualStyle, palette, layout, iconBitmap)
    }
}

private fun DrawScope.drawWidgetGauge(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    visualStyle: WidgetVisualStyle,
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    icon: ImageBitmap?
) {
    when (visualStyle) {
        WidgetVisualStyle.GTI -> drawGtiWidget(widgetType, telemetry, palette, layout, icon)
        WidgetVisualStyle.PREMIUM -> drawPremiumWidget(widgetType, telemetry, palette, layout, icon)
        WidgetVisualStyle.NEON -> drawNeonWidget(widgetType, telemetry, palette, layout)
        WidgetVisualStyle.VINTAGE -> drawVintageWidget(widgetType, telemetry, palette, layout)
        WidgetVisualStyle.NIGHT -> drawNightWidget(widgetType, telemetry, palette, layout)
        WidgetVisualStyle.EPURE,
        WidgetVisualStyle.RACING_HUD,
        WidgetVisualStyle.STRUCTURED -> Unit
    }
}

private fun DrawScope.drawNeonWidget(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    palette: WidgetThemePalette,
    layout: GaugeLayout
) {
    val cx = layout.cx
    val cy = layout.cyMid
    val r = min(layout.w, layout.h) * 0.47f
    val cyan = palette.accent
    val blue = NeonBlueAccent
    when (widgetType) {
        WidgetType.SPEED -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.speedKmh, 0f, 240f,
            "%.0f".format(telemetry.speedKmh), "km/h",
            arcColor = cyan
        )
        WidgetType.RPM -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.rpm.toFloat(), 0f, CarVisualTheme.RPM_REDLINE.toFloat(),
            "%d".format(telemetry.rpm), "RPM",
            arcColor = blue
        )
        WidgetType.TURBO_PRESSURE -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.turboPressureBar, 0f, 1.8f,
            "%.1f".format(telemetry.turboPressureBar), "bar",
            arcColor = blue
        )
        WidgetType.WATER_TEMP -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.waterTempC, 0f, 120f,
            "%.0f".format(telemetry.waterTempC), "°C",
            arcColor = cyan
        )
        WidgetType.OIL_TEMP -> {
            val oilAccent = if (telemetry.oilTempC >= CarVisualTheme.OIL_TEMP_WARNING_C) {
                palette.segmentYellow
            } else {
                cyan
            }
            drawNeonRingGauge(
                palette, cx, cy, r,
                telemetry.oilTempC, 0f, 130f,
                "%.0f".format(telemetry.oilTempC), "°C",
                arcColor = oilAccent
            )
        }
        WidgetType.LATERAL_G -> drawNeonGForceDot(
            palette, cx, cy, r, telemetry.lateralGForce
        )
        WidgetType.THROTTLE -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.throttlePercent, 0f, 100f,
            "%.0f".format(telemetry.throttlePercent), "%",
            arcColor = cyan
        )
        WidgetType.ENGINE_LOAD -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.engineLoadPercent, 0f, 100f,
            "%.0f".format(telemetry.engineLoadPercent), "%",
            arcColor = blue
        )
        WidgetType.AFR -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.afr, 10f, 18f,
            "%.1f".format(telemetry.afr), "AFR",
            arcColor = cyan
        )
        WidgetType.O2_VOLTAGE -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.o2VoltageV, 0f, 1f,
            "%.2f".format(telemetry.o2VoltageV), "V",
            arcColor = blue
        )
        WidgetType.IGNITION -> drawNeonRingGauge(
            palette, cx, cy, r,
            telemetry.ignitionAdvanceBtdc, 0f, 40f,
            "%.0f".format(telemetry.ignitionAdvanceBtdc), "°",
            arcColor = cyan
        )
        WidgetType.DELTA_CHRONO -> {
            val slow = telemetry.deltaChronoMs > 0
            val arc = if (slow) palette.segmentRed else palette.segmentGreen
            val sign = if (telemetry.deltaChronoMs >= 0) "+" else ""
            drawNeonRingGauge(
                palette, cx, cy, r,
                kotlin.math.abs(telemetry.deltaChronoMs).toFloat(), 0f, 5000f,
                sign + "%.1f".format(telemetry.deltaChronoMs / 1000f), "s",
                arcColor = arc
            )
        }
        else -> drawNeonRingGauge(
            palette, cx, cy, r,
            0.5f, 0f, 1f,
            "--", null,
            arcColor = cyan
        )
    }
}

private fun DrawScope.drawGtiWidget(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    icon: ImageBitmap?
) {
    val cx = layout.cx
    val cy = layout.cyMid
    val r = min(layout.w, layout.h) * 0.46f
    when (widgetType) {
        WidgetType.SPEED -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.speedKmh, 0f, 240f,
            "km/h", listOf("0", "60", "120", "180", "240"),
            icon, warningFraction = 0.75f
        )
        WidgetType.RPM -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.rpm.toFloat(), 0f, CarVisualTheme.RPM_REDLINE.toFloat(),
            "RPM", listOf("0", "2k", "4k", "6k", "7k"),
            icon, warningFraction = 0.7f
        )
        WidgetType.TURBO_PRESSURE -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.turboPressureBar, 0f, 1.8f,
            "bar", listOf("0", "0.5", "1", "1.5", "1.8"),
            icon, warningFraction = 0.8f
        )
        WidgetType.WATER_TEMP -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.waterTempC, 0f, 120f,
            "°C", listOf("0", "40", "80", "120"),
            icon, warningFraction = 0.75f
        )
        WidgetType.OIL_TEMP -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.oilTempC, 0f, 130f,
            "°C", listOf("0", "50", "100", "130"),
            icon, warningFraction = 0.72f
        )
        WidgetType.LATERAL_G -> drawGtiSportGForce(
            palette, cx, cy, r, telemetry.lateralGForce, icon
        )
        WidgetType.THROTTLE -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.throttlePercent, 0f, 100f,
            "%", listOf("0", "25", "50", "75", "100"),
            icon
        )
        WidgetType.ENGINE_LOAD -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.engineLoadPercent, 0f, 100f,
            "%", listOf("0", "25", "50", "75", "100"),
            icon
        )
        WidgetType.AFR -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.afr, 10f, 18f,
            "AFR", listOf("10", "12", "14", "16", "18"),
            icon
        )
        WidgetType.O2_VOLTAGE -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.o2VoltageV, 0f, 1f,
            "V", listOf("0", "0.25", "0.5", "0.75", "1"),
            icon
        )
        WidgetType.IGNITION -> drawGtiSportDial(
            palette, cx, cy, r,
            telemetry.ignitionAdvanceBtdc, 0f, 40f,
            "° BTDC", listOf("0", "10", "20", "30", "40"),
            icon
        )
        WidgetType.DELTA_CHRONO -> drawDeltaBadgeThemed(palette, cx, cy, telemetry.deltaChronoMs)
        else -> drawGtiSportDial(
            palette, cx, cy, r,
            0.5f, 0f, 1f,
            "", listOf("0", "50", "100"),
            icon
        )
    }
}

private fun DrawScope.drawPremiumWidget(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    palette: WidgetThemePalette,
    layout: GaugeLayout,
    icon: ImageBitmap?
) {
    val cx = layout.cx
    val cy = layout.cyMid
    val r = min(layout.w, layout.h) * 0.46f
    when (widgetType) {
        WidgetType.SPEED -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.speedKmh, 0f, 240f,
            "%.0f".format(telemetry.speedKmh), "km/h",
            listOf("0", "60", "120", "180", "240"), icon
        )
        WidgetType.RPM -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.rpm.toFloat(), 0f, CarVisualTheme.RPM_REDLINE.toFloat(),
            "%d".format(telemetry.rpm), "RPM",
            listOf("0", "2k", "4k", "6k", "7k"), icon
        )
        WidgetType.TURBO_PRESSURE -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.turboPressureBar, 0f, 1.8f,
            "%.1f".format(telemetry.turboPressureBar), "bar",
            listOf("0", "0.5", "1", "1.5", "1.8"), icon
        )
        WidgetType.WATER_TEMP -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.waterTempC, 0f, 120f,
            "%.0f".format(telemetry.waterTempC), "°C",
            listOf("0", "40", "80", "120"), icon
        )
        WidgetType.OIL_TEMP -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.oilTempC, 0f, 130f,
            "%.0f".format(telemetry.oilTempC), "°C",
            listOf("0", "50", "100", "130"), icon
        )
        WidgetType.LATERAL_G -> {
            drawPremiumDial(
                palette, cx, cy, r,
                (telemetry.lateralGForce + 2f) / 4f, 0f, 1f,
                "%.1f".format(telemetry.lateralGForce), "G",
                listOf("-2", "-1", "0", "1", "2"), icon
            )
            val angle = ((telemetry.lateralGForce.coerceIn(-2f, 2f) / 2f) * (kotlin.math.PI / 2)).toFloat()
            val dot = Offset(
                cx + kotlin.math.sin(angle) * r * 0.58f,
                cy - kotlin.math.cos(angle) * r * 0.58f
            )
            drawCircle(color = palette.accent, radius = r * 0.06f, center = dot)
        }
        WidgetType.THROTTLE -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.throttlePercent, 0f, 100f,
            "%.0f".format(telemetry.throttlePercent), "%",
            listOf("0", "25", "50", "75", "100"), icon
        )
        WidgetType.ENGINE_LOAD -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.engineLoadPercent, 0f, 100f,
            "%.0f".format(telemetry.engineLoadPercent), "%",
            listOf("0", "25", "50", "75", "100"), icon
        )
        WidgetType.AFR -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.afr, 10f, 18f,
            "%.1f".format(telemetry.afr), "AFR",
            listOf("10", "12", "14", "16", "18"), icon
        )
        WidgetType.O2_VOLTAGE -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.o2VoltageV, 0f, 1f,
            "%.2f".format(telemetry.o2VoltageV), "V",
            listOf("0", "0.25", "0.5", "0.75", "1"), icon
        )
        WidgetType.IGNITION -> drawPremiumDial(
            palette, cx, cy, r,
            telemetry.ignitionAdvanceBtdc, 0f, 40f,
            "%.0f".format(telemetry.ignitionAdvanceBtdc), "°",
            listOf("0", "10", "20", "30", "40"), icon
        )
        WidgetType.DELTA_CHRONO -> drawDeltaBadgeThemed(palette, cx, cy, telemetry.deltaChronoMs)
        else -> drawPremiumDial(
            palette, cx, cy, r,
            0.5f, 0f, 1f,
            "--", null,
            listOf("0", "50", "100"), icon
        )
    }
}

private fun DrawScope.drawVintageWidget(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    palette: WidgetThemePalette,
    layout: GaugeLayout
) {
    val cx = layout.cx
    when (widgetType) {
        WidgetType.SPEED, WidgetType.TURBO_PRESSURE -> {
            val r = layout.dialRadius * 0.92f
            drawVintageDialFace(palette, cx, layout.cyMid, r)
            val value = if (widgetType == WidgetType.SPEED) {
                telemetry.speedKmh
            } else {
                telemetry.turboPressureBar
            }
            val max = if (widgetType == WidgetType.SPEED) 240f else 1.8f
            drawAnalogArcThemed(
                palette,
                cx,
                layout.cyMid,
                r * 0.85f,
                value,
                0f,
                max,
                needleColor = palette.needle,
                arcColor = palette.accent
            )
        }
        WidgetType.RPM -> drawAnalogArcThemed(
            palette,
            cx,
            layout.cyDial,
            layout.dialRadius,
            telemetry.rpm.toFloat(),
            0f,
            CarVisualTheme.RPM_REDLINE.toFloat()
        )
        WidgetType.WATER_TEMP, WidgetType.OIL_TEMP -> drawHudTempBar(
            palette,
            layout,
            if (widgetType == WidgetType.WATER_TEMP) telemetry.waterTempC else telemetry.oilTempC,
            if (widgetType == WidgetType.WATER_TEMP) 120f else 130f
        )
        WidgetType.LATERAL_G -> {
            drawVintageDialFace(palette, cx, layout.cyMid, min(layout.w, layout.h) * 0.42f)
            drawGForceMeterThemed(
                palette, cx, layout.cyMid, min(layout.w, layout.h) * 0.38f, telemetry.lateralGForce
            )
        }
        else -> drawGtiWidget(widgetType, telemetry, palette, layout, icon = null)
    }
}

private fun DrawScope.drawNightWidget(
    widgetType: WidgetType,
    telemetry: TelemetryData,
    palette: WidgetThemePalette,
    layout: GaugeLayout
) {
    val cx = layout.cx
    when (widgetType) {
        WidgetType.SPEED -> drawNightDoubleArc(
            palette, cx, layout.cyDial, layout.dialRadius,
            telemetry.speedKmh, 0f, 240f
        )
        WidgetType.TURBO_PRESSURE -> drawNightDoubleArc(
            palette, cx, layout.cyDial, layout.dialRadius,
            telemetry.turboPressureBar, 0f, 1.8f
        )
        WidgetType.RPM -> drawRpmSegmentsThemed(
            palette, cx, layout.cyMid, layout.w * 0.96f, layout.h * 0.48f, telemetry.rpm
        )
        WidgetType.WATER_TEMP -> drawThermometerThemed(
            palette, cx, layout.cyMid, layout.h * 0.78f, telemetry.waterTempC, oil = false
        )
        WidgetType.OIL_TEMP -> drawThermometerThemed(
            palette, cx, layout.cyMid, layout.h * 0.78f, telemetry.oilTempC, oil = true
        )
        WidgetType.LATERAL_G -> drawGForceMeterThemed(
            palette, cx, layout.cyMid, min(layout.w, layout.h) * 0.46f, telemetry.lateralGForce
        )
        else -> drawGtiWidget(widgetType, telemetry, palette, layout, icon = null)
    }
}
