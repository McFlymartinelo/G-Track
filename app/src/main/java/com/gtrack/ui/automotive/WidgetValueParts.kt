package com.gtrack.ui.automotive

import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.formatMs

data class WidgetValueParts(
    val main: String,
    val unit: String? = null
)

fun WidgetType.valueParts(data: TelemetryData): WidgetValueParts = when (this) {
    WidgetType.LAP_TIME -> WidgetValueParts(CarVisualTheme.formatLapTime(data.lapTimeMs), null)
    WidgetType.DELTA_CHRONO -> {
        val sign = if (data.deltaChronoMs >= 0) "+" else ""
        WidgetValueParts(sign + formatMs(kotlin.math.abs(data.deltaChronoMs)), null)
    }
    WidgetType.GEAR -> WidgetValueParts(if (data.gear <= 0) "N" else data.gear.toString(), null)
    WidgetType.GEAR_PRE -> WidgetValueParts(if (data.gearPre <= 0) "N" else data.gearPre.toString(), null)
    WidgetType.TPMS_FL, WidgetType.TPMS_FR, WidgetType.TPMS_RL, WidgetType.TPMS_RR ->
        WidgetValueParts(if (value(data) >= 0.5f) "LOW" else "OK", null)
    else -> WidgetValueParts("%.${decimals}f".format(value(data)), unit.ifEmpty { null })
}
