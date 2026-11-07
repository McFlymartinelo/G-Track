package com.gtrack.widgets

import com.gtrack.telemetry.TelemetryData

/** Position roue (vue conducteur). */
enum class TireCorner(val selectorLabel: String, val positionLabel: String) {
    FL("AG", "AVG"),
    FR("AD", "AVD"),
    RL("RG", "ARG"),
    RR("RD", "ARD");

    fun snapshot(telemetry: TelemetryData): TireWheelSnapshot = when (this) {
        FL -> TireWheelSnapshot(
            telemetry.tirePressureFLBar,
            telemetry.tireTempFLC,
            telemetry.tpmsFL >= 0.5f
        )
        FR -> TireWheelSnapshot(
            telemetry.tirePressureFRBar,
            telemetry.tireTempFRC,
            telemetry.tpmsFR >= 0.5f
        )
        RL -> TireWheelSnapshot(
            telemetry.tirePressureRLBar,
            telemetry.tireTempRLC,
            telemetry.tpmsRL >= 0.5f
        )
        RR -> TireWheelSnapshot(
            telemetry.tirePressureRRBar,
            telemetry.tireTempRRC,
            telemetry.tpmsRR >= 0.5f
        )
    }
}

data class TireWheelSnapshot(
    val pressureBar: Float,
    val tempC: Float,
    val tpmsLow: Boolean
) {
    val pressureText: String get() = "%.2f".format(pressureBar)
    val tempText: String get() = "%.1f".format(tempC)
}

/** Widgets affichés avec la jauge pneu unifiée (combinés + anciens pression/temp/tpms). */
fun WidgetType.usesTireMonitorUi(): Boolean = isTireWheel() || isLegacyTireWidget()

fun WidgetType.isTireWheel(): Boolean = when (this) {
    WidgetType.TIRE_MONITOR,
    WidgetType.TIRE_WHEEL_FL,
    WidgetType.TIRE_WHEEL_FR,
    WidgetType.TIRE_WHEEL_RL,
    WidgetType.TIRE_WHEEL_RR -> true
    else -> false
}

private fun WidgetType.isLegacyTireWidget(): Boolean = when (this) {
    WidgetType.TIRE_PRESS_FL,
    WidgetType.TIRE_PRESS_FR,
    WidgetType.TIRE_PRESS_RL,
    WidgetType.TIRE_PRESS_RR,
    WidgetType.TIRE_TEMP_FL,
    WidgetType.TIRE_TEMP_FR,
    WidgetType.TIRE_TEMP_RL,
    WidgetType.TIRE_TEMP_RR,
    WidgetType.TPMS_FL,
    WidgetType.TPMS_FR,
    WidgetType.TPMS_RL,
    WidgetType.TPMS_RR -> true
    else -> false
}

fun WidgetType.defaultTireCorner(): TireCorner = when (this) {
    WidgetType.TIRE_WHEEL_FR, WidgetType.TIRE_PRESS_FR,
    WidgetType.TIRE_TEMP_FR, WidgetType.TPMS_FR -> TireCorner.FR
    WidgetType.TIRE_WHEEL_RL, WidgetType.TIRE_PRESS_RL,
    WidgetType.TIRE_TEMP_RL, WidgetType.TPMS_RL -> TireCorner.RL
    WidgetType.TIRE_WHEEL_RR, WidgetType.TIRE_PRESS_RR,
    WidgetType.TIRE_TEMP_RR, WidgetType.TPMS_RR -> TireCorner.RR
    else -> TireCorner.FL
}

fun WidgetType.tireWheelSnapshot(telemetry: TelemetryData): TireWheelSnapshot? =
    if (usesTireMonitorUi()) defaultTireCorner().snapshot(telemetry) else null

fun WidgetType.tireWheelSnapshot(telemetry: TelemetryData, corner: TireCorner): TireWheelSnapshot =
    corner.snapshot(telemetry)
