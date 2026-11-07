package com.gtrack.ui.automotive

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.gtrack.R
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.usesTireMonitorUi

@StringRes
fun WidgetType.titleResId(): Int = titleRes

@Composable
fun WidgetType.displayTitle(): String = stringResource(titleResId())

fun WidgetType.borderColor(data: TelemetryData): Color = when (this) {
    WidgetType.OIL_TEMP ->
        if (data.oilTempC >= CarVisualTheme.OIL_TEMP_WARNING_C) Color(0xFFFF8C00) else Color.Red
    WidgetType.WATER_TEMP ->
        if (data.waterTempC >= CarVisualTheme.WATER_TEMP_WARNING_C) Color(0xFFFF8C00) else Color.Red
    WidgetType.DELTA_CHRONO ->
        if (data.deltaChronoMs > 0) Color.Red else Color(0xFF00C853)
    else -> Color.Red
}

fun WidgetType.iconResId(): Int = when (this) {
    WidgetType.SPEED -> R.drawable.ic_car_speed
    WidgetType.RPM -> R.drawable.ic_car_rpm
    WidgetType.TURBO_PRESSURE -> R.drawable.ic_car_turbo
    WidgetType.WATER_TEMP, WidgetType.OIL_TEMP -> R.drawable.ic_car_temp
    WidgetType.LATERAL_G -> R.drawable.ic_car_gforce
    WidgetType.LAP_TIME -> R.drawable.ic_car_lap
    WidgetType.DELTA_CHRONO -> R.drawable.ic_car_delta
    WidgetType.TIRE_WHEEL_FL, WidgetType.TIRE_WHEEL_FR,
    WidgetType.TIRE_WHEEL_RL, WidgetType.TIRE_WHEEL_RR,
    WidgetType.TIRE_MONITOR -> R.drawable.ic_car_tire
    else -> R.drawable.ic_car_speed
}

/** Seuil d'alerte de pression des pneus (bar) au-delà duquel la valeur passe en rouge. */
const val TIRE_PRESSURE_ALERT_BAR = 2.3f

fun WidgetType.isTirePressure(): Boolean =
    usesTireMonitorUi() ||
        this == WidgetType.TIRE_PRESS_FL || this == WidgetType.TIRE_PRESS_FR ||
        this == WidgetType.TIRE_PRESS_RL || this == WidgetType.TIRE_PRESS_RR

/** Vrai quand la pression du pneu dépasse [TIRE_PRESSURE_ALERT_BAR]. */
fun WidgetType.tirePressureAlert(data: TelemetryData): Boolean =
    isTirePressure() && value(data) > TIRE_PRESSURE_ALERT_BAR

fun WidgetType.valueColor(data: TelemetryData): Color = when (this) {
    WidgetType.DELTA_CHRONO ->
        if (data.deltaChronoMs > 0) Color.Red else Color(0xFF00C853)
    WidgetType.LAP_TIME -> Color(0xFFFFEB3B)
    WidgetType.TIRE_PRESS_FL, WidgetType.TIRE_PRESS_FR,
    WidgetType.TIRE_PRESS_RL, WidgetType.TIRE_PRESS_RR ->
        if (tirePressureAlert(data)) Color.Red else Color.White
    else -> Color.White
}
