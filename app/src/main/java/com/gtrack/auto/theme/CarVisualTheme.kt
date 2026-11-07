package com.gtrack.auto.theme

import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType

/**
 * Palette et helpers visuels alignés sur les maquettes GTI Track Monitor.
 */
object CarVisualTheme {
    val ACCENT_RED = CarColor.RED
    val LIVE_YELLOW = CarColor.YELLOW
    val BEST_GREEN = CarColor.GREEN
    val WARNING_ORANGE = CarColor.DEFAULT
    val LABEL_WHITE = CarColor.DEFAULT
    val RICH_GREEN = CarColor.GREEN
    val LEAN_RED = CarColor.RED
    val STOICH_BLUE = CarColor.BLUE

    /** Zone rouge dynamique (essence 7000 / diesel 6000) — voir [RpmScale]. */
    val RPM_REDLINE: Int get() = RpmScale.redline
    const val OIL_TEMP_WARNING_C = 105f
    const val WATER_TEMP_WARNING_C = 100f

    fun formatLapTime(ms: Long): String {
        if (ms <= 0L) return "--:--.--"
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val centis = (ms % 1000) / 10
        return if (minutes > 0) {
            "%d:%02d.%02d".format(minutes, seconds, centis)
        } else {
            "%d.%02d".format(seconds, centis)
        }
    }

    fun coloredValue(text: String, color: CarColor): CarText =
        CarTextHelper.create(text, color)

    fun sectionLabel(label: String): CarText =
        CarTextHelper.create(label.uppercase(), LABEL_WHITE)

    fun horizontalBar(fraction: Float, segments: Int = 10): String {
        val filled = (fraction.coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)
        return "▮".repeat(filled) + "▯".repeat(segments - filled)
    }

    fun verticalBarHint(fraction: Float): String {
        val filled = (fraction.coerceIn(0f, 1f) * 5).toInt().coerceIn(0, 5)
        return buildString {
            repeat(5) { row ->
                append(if (4 - row < filled) "█" else "░")
                if (row < 4) append('\n')
            }
        }
    }

    fun widgetValueColor(type: WidgetType, data: TelemetryData): CarColor = when (type) {
        WidgetType.DELTA_CHRONO -> when {
            data.sectorDeltaPositive == true -> BEST_GREEN
            data.sectorDeltaPositive == false -> ACCENT_RED
            data.deltaChronoMs < 0 -> BEST_GREEN
            data.deltaChronoMs > 0 -> ACCENT_RED
            else -> LABEL_WHITE
        }
        WidgetType.OIL_TEMP -> if (data.oilTempC >= OIL_TEMP_WARNING_C) ACCENT_RED else LABEL_WHITE
        WidgetType.WATER_TEMP -> if (data.waterTempC >= WATER_TEMP_WARNING_C) WARNING_ORANGE else LABEL_WHITE
        WidgetType.LAP_TIME -> LIVE_YELLOW
        WidgetType.TURBO_PRESSURE -> LABEL_WHITE
        else -> LABEL_WHITE
    }

    fun widgetSubtitle(type: WidgetType, data: TelemetryData): String? = when (type) {
        WidgetType.SPEED -> horizontalBar(data.speedKmh / 250f)
        WidgetType.RPM -> horizontalBar(data.rpm.toFloat() / RPM_REDLINE)
        WidgetType.TURBO_PRESSURE -> "▮ ${"%.2f".format(data.turboPressureBar)} bar"
        WidgetType.WATER_TEMP, WidgetType.OIL_TEMP -> verticalBarHint(
            when (type) {
                WidgetType.WATER_TEMP -> data.waterTempC / 120f
                else -> data.oilTempC / 130f
            }
        )
        WidgetType.LATERAL_G -> horizontalBar((data.lateralGForce + 2f) / 4f)
        WidgetType.DELTA_CHRONO -> afrStyleHint(data)
        else -> null
    }

    private fun afrStyleHint(data: TelemetryData): String {
        val lean = "Lean"
        val stoich = "STOICH"
        val rich = "Rich"
        val marker = when {
            data.deltaChronoMs < 0 -> "│"
            data.deltaChronoMs > 0 -> "│"
            else -> "│"
        }
        return "$lean $marker $stoich $rich"
    }
}
