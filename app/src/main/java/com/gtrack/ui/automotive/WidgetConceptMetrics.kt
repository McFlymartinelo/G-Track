package com.gtrack.ui.automotive

import androidx.compose.ui.graphics.Color
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType

data class WidgetConceptPresentation(
    val fraction: Float,
    val accentColor: Color,
    val valueColor: Color,
    val footerLabel: String? = null,
    val footerDotColor: Color? = null,
    val statusLabel: String? = null,
    val statusColor: Color? = null,
    val subtitle: String? = null,
    val useRpmBars: Boolean = false,
    val useGForceMeter: Boolean = false,
    val useLinearBar: Boolean = true
)

fun WidgetType.conceptPresentation(telemetry: TelemetryData): WidgetConceptPresentation {
    return when (this) {
        WidgetType.SPEED -> WidgetConceptPresentation(
            fraction = (telemetry.speedKmh / 255f).coerceIn(0f, 1f),
            accentColor = Color(0xFFE53935),
            valueColor = Color(0xFFE53935),
            footerLabel = "max 255",
            footerDotColor = Color(0xFFE53935)
        )
        WidgetType.RPM -> WidgetConceptPresentation(
            fraction = (telemetry.rpm.toFloat() / CarVisualTheme.RPM_REDLINE).coerceIn(0f, 1f),
            accentColor = Color(0xFF43A047),
            valueColor = Color(0xFF43A047),
            useRpmBars = true,
            useLinearBar = false
        )
        WidgetType.TURBO_PRESSURE -> WidgetConceptPresentation(
            fraction = (telemetry.turboPressureBar / 2f).coerceIn(0f, 1f),
            accentColor = Color(0xFF42A5F5),
            valueColor = Color.White,
            footerLabel = "max 2.0",
            footerDotColor = Color(0xFF42A5F5)
        )
        WidgetType.WATER_TEMP -> {
            val warn = telemetry.waterTempC >= CarVisualTheme.WATER_TEMP_WARNING_C
            WidgetConceptPresentation(
                fraction = (telemetry.waterTempC / 120f).coerceIn(0f, 1f),
                accentColor = if (warn) Color(0xFFFF8C00) else Color(0xFFE53935),
                valueColor = if (warn) Color(0xFFFF8C00) else Color(0xFFE53935),
                footerLabel = if (warn) "attention" else "normal",
                footerDotColor = if (warn) Color(0xFFFF8C00) else Color(0xFF43A047)
            )
        }
        WidgetType.OIL_TEMP -> {
            val warn = telemetry.oilTempC >= CarVisualTheme.OIL_TEMP_WARNING_C
            WidgetConceptPresentation(
                fraction = (telemetry.oilTempC / 130f).coerceIn(0f, 1f),
                accentColor = Color(0xFFFF8C00),
                valueColor = Color(0xFFFF8C00),
                footerLabel = if (warn) "attention" else "normal",
                footerDotColor = if (warn) Color(0xFFFF8C00) else Color(0xFF43A047)
            )
        }
        WidgetType.LATERAL_G -> WidgetConceptPresentation(
            fraction = ((telemetry.lateralGForce + 2f) / 4f).coerceIn(0f, 1f),
            accentColor = Color(0xFFE53935),
            valueColor = Color.White,
            useGForceMeter = true,
            useLinearBar = false
        )
        WidgetType.DELTA_CHRONO -> {
            val slow = telemetry.deltaChronoMs > 0
            WidgetConceptPresentation(
                fraction = 0.5f,
                accentColor = if (slow) Color(0xFFE53935) else Color(0xFF43A047),
                valueColor = if (slow) Color(0xFFE53935) else Color(0xFF43A047),
                subtitle = "Delta chrono",
                useLinearBar = false
            )
        }
        else -> {
            val alert = tirePressureAlert(telemetry)
            WidgetConceptPresentation(
                fraction = fraction(telemetry),
                accentColor = if (alert) Color(0xFFE53935) else Color(0xFF90A4AE),
                valueColor = if (alert) Color(0xFFE53935) else Color.White,
                subtitle = displayTitleNonComposable(),
                useLinearBar = true
            )
        }
    }
}

/** Évite @Composable dans le helper métier. */
private fun WidgetType.displayTitleNonComposable(): String = when (this) {
    WidgetType.AFR -> "AFR"
    WidgetType.IGNITION -> "Avance"
    WidgetType.ENGINE_LOAD -> "Charge"
    WidgetType.THROTTLE -> "Papillon"
    WidgetType.O2_VOLTAGE -> "O2"
    WidgetType.LAP_TIME -> "Tour"
    else -> name
}
