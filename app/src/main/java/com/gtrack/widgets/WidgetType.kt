package com.gtrack.widgets

import androidx.annotation.StringRes
import com.gtrack.R
import com.gtrack.auto.pages.TelemetryWidgetSlot
import com.gtrack.telemetry.TelemetryData

/** Famille de capteur, utilisée pour le regroupement du sélecteur et la couleur générique. */
enum class WidgetCategory(@StringRes val titleRes: Int) {
    VITESSE(R.string.widget_cat_vitesse),
    MOTEUR(R.string.widget_cat_moteur),
    TEMPERATURE(R.string.widget_cat_temperature),
    PRESSION(R.string.widget_cat_pression),
    PNEU(R.string.widget_cat_pneu),
    BATTERIE(R.string.widget_cat_batterie),
    DYNAMIQUE(R.string.widget_cat_dynamique),
    CHRONO(R.string.widget_cat_chrono)
}

/**
 * Métrique affichable, pilotée par métadonnées (unité, bornes, décimales, catégorie, extracteur).
 * Ajouter une métrique = ajouter une entrée ici (+ champ télémétrie + décodeur PID).
 */
enum class WidgetType(
    val id: String,
    @StringRes val titleRes: Int,
    val unit: String,
    val min: Float,
    val max: Float,
    val decimals: Int,
    val category: WidgetCategory,
    val isTime: Boolean = false,
    val value: (TelemetryData) -> Float
) {
    SPEED("speed", R.string.widget_speed, "km/h", 0f, 255f, 0, WidgetCategory.VITESSE, value = { it.speedKmh }),
    RPM("rpm", R.string.widget_rpm, "RPM", 0f, 7500f, 0, WidgetCategory.MOTEUR, value = { it.rpm.toFloat() }),
    WATER_TEMP("water_temp", R.string.widget_water_temp, "°C", 40f, 120f, 0, WidgetCategory.TEMPERATURE, value = { it.waterTempC }),
    OIL_TEMP("oil_temp", R.string.widget_oil_temp, "°C", 50f, 150f, 0, WidgetCategory.TEMPERATURE, value = { it.oilTempC }),
    TURBO_PRESSURE("turbo", R.string.widget_turbo, "bar", 0f, 2f, 1, WidgetCategory.PRESSION, value = { it.turboPressureBar }),
    LATERAL_G("lateral_g", R.string.widget_lateral_g, "G", -2f, 2f, 1, WidgetCategory.DYNAMIQUE, value = { it.lateralGForce }),
    LAP_TIME("lap_time", R.string.widget_lap_time, "", 0f, 1f, 0, WidgetCategory.CHRONO, isTime = true, value = { it.lapTimeMs.toFloat() }),
    DELTA_CHRONO("delta_chrono", R.string.widget_delta_chrono, "", 0f, 1f, 0, WidgetCategory.CHRONO, isTime = true, value = { it.deltaChronoMs.toFloat() }),
    AFR("afr", R.string.widget_afr, "AFR", 8f, 20f, 1, WidgetCategory.MOTEUR, value = { it.afr }),
    IGNITION("ignition", R.string.widget_ignition, "° BTDC", -10f, 60f, 0, WidgetCategory.MOTEUR, value = { it.ignitionAdvanceBtdc }),
    ENGINE_LOAD("engine_load", R.string.widget_engine_load, "%", 0f, 100f, 0, WidgetCategory.MOTEUR, value = { it.engineLoadPercent }),
    THROTTLE("throttle", R.string.widget_throttle, "%", 0f, 100f, 0, WidgetCategory.MOTEUR, value = { it.throttlePercent }),
    O2_VOLTAGE("o2", R.string.widget_o2, "V", 0f, 1f, 2, WidgetCategory.MOTEUR, value = { it.o2VoltageV }),
    PEDAL("pedal", R.string.widget_pedal, "%", 0f, 100f, 0, WidgetCategory.MOTEUR, value = { it.pedalPercent }),
    STEERING("steering", R.string.widget_steering, "°", -720f, 720f, 0, WidgetCategory.DYNAMIQUE, value = { it.steeringAngleDeg }),
    BATT_CURRENT("batt_current", R.string.widget_batt_current, "A", 0f, 275f, 0, WidgetCategory.BATTERIE, value = { it.battCurrentA }),
    BATT_SOC("batt_soc", R.string.widget_batt_soc, "%", 0f, 100f, 0, WidgetCategory.BATTERIE, value = { it.battSocPct }),
    BATT_TEMP("batt_temp", R.string.widget_batt_temp, "°C", 0f, 80f, 0, WidgetCategory.BATTERIE, value = { it.battTempC }),
    BATT_VOLT("batt_volt", R.string.widget_batt_volt, "V", 8f, 16f, 2, WidgetCategory.BATTERIE, value = { it.battVoltV }),
    FUEL_LEVEL("fuel_level", R.string.widget_fuel_level, "L", 0f, 45f, 1, WidgetCategory.MOTEUR, value = { it.fuelLiters }),
    TORQUE("torque", R.string.widget_torque, "Nm", 0f, 340f, 0, WidgetCategory.MOTEUR, value = { it.engineTorqueNm }),
    INTAKE_PRESSURE("intake_pressure", R.string.widget_intake_pressure, "mbar", 0f, 3000f, 0, WidgetCategory.PRESSION, value = { it.intakePressureMbar }),
    ATMO_PRESSURE("atmo_pressure", R.string.widget_atmo_pressure, "mbar", 980f, 1080f, 0, WidgetCategory.PRESSION, value = { it.atmoPressureMbar }),
    BRAKE_PRESSURE("brake_pressure", R.string.widget_brake_pressure, "bar", 0f, 50f, 0, WidgetCategory.PRESSION, value = { it.brakePressureBar }),
    OIL_PRESSURE("oil_pressure", R.string.widget_oil_pressure, "bar", 0f, 10f, 1, WidgetCategory.PRESSION, value = { it.oilPressureBar }),
    GEAR("gear", R.string.widget_gear, "", 0f, 8f, 0, WidgetCategory.MOTEUR, value = { it.gear.toFloat() }),
    GEAR_PRE("gear_pre", R.string.widget_gear_pre, "", 0f, 8f, 0, WidgetCategory.MOTEUR, value = { it.gearPre.toFloat() }),
    INTAKE_TEMP("intake_temp", R.string.widget_intake_temp, "°C", 0f, 80f, 0, WidgetCategory.TEMPERATURE, value = { it.intakeTempC }),
    CAT_TEMP("cat_temp", R.string.widget_cat_temp, "°C", 0f, 800f, 0, WidgetCategory.TEMPERATURE, value = { it.catTempC }),
    EXT_TEMP("ext_temp", R.string.widget_ext_temp, "°C", -20f, 70f, 1, WidgetCategory.TEMPERATURE, value = { it.extTempC }),
    EGT("egt", R.string.widget_egt, "°C", 0f, 800f, 0, WidgetCategory.TEMPERATURE, value = { it.egtC }),
    GEARBOX_OIL_TEMP("gearbox_oil_temp", R.string.widget_gearbox_oil_temp, "°C", 20f, 150f, 0, WidgetCategory.TEMPERATURE, value = { it.gearboxOilTempC }),
    TIRE_MONITOR("tire_monitor", R.string.widget_tire_monitor, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureFLBar }),
    TIRE_WHEEL_FL("tire_wheel_fl", R.string.widget_tire_wheel_fl, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureFLBar }),
    TIRE_WHEEL_FR("tire_wheel_fr", R.string.widget_tire_wheel_fr, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureFRBar }),
    TIRE_WHEEL_RL("tire_wheel_rl", R.string.widget_tire_wheel_rl, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureRLBar }),
    TIRE_WHEEL_RR("tire_wheel_rr", R.string.widget_tire_wheel_rr, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureRRBar }),
    TIRE_PRESS_FL("tire_press_fl", R.string.widget_tire_press_fl, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureFLBar }),
    TIRE_PRESS_FR("tire_press_fr", R.string.widget_tire_press_fr, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureFRBar }),
    TIRE_PRESS_RL("tire_press_rl", R.string.widget_tire_press_rl, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureRLBar }),
    TIRE_PRESS_RR("tire_press_rr", R.string.widget_tire_press_rr, "bar", 0f, 2.5f, 2, WidgetCategory.PNEU, value = { it.tirePressureRRBar }),
    TIRE_TEMP_FL("tire_temp_fl", R.string.widget_tire_temp_fl, "°C", 0f, 70f, 1, WidgetCategory.PNEU, value = { it.tireTempFLC }),
    TIRE_TEMP_FR("tire_temp_fr", R.string.widget_tire_temp_fr, "°C", 0f, 70f, 1, WidgetCategory.PNEU, value = { it.tireTempFRC }),
    TIRE_TEMP_RL("tire_temp_rl", R.string.widget_tire_temp_rl, "°C", 0f, 70f, 1, WidgetCategory.PNEU, value = { it.tireTempRLC }),
    TIRE_TEMP_RR("tire_temp_rr", R.string.widget_tire_temp_rr, "°C", 0f, 70f, 1, WidgetCategory.PNEU, value = { it.tireTempRRC }),
    TPMS_FL("tpms_fl", R.string.widget_tpms_fl, "", 0f, 1f, 0, WidgetCategory.PNEU, value = { it.tpmsFL }),
    TPMS_FR("tpms_fr", R.string.widget_tpms_fr, "", 0f, 1f, 0, WidgetCategory.PNEU, value = { it.tpmsFR }),
    TPMS_RL("tpms_rl", R.string.widget_tpms_rl, "", 0f, 1f, 0, WidgetCategory.PNEU, value = { it.tpmsRL }),
    TPMS_RR("tpms_rr", R.string.widget_tpms_rr, "", 0f, 1f, 0, WidgetCategory.PNEU, value = { it.tpmsRR });

    val fraction: (TelemetryData) -> Float
        get() = { data ->
            val v = value(data)
            if (max <= min) 0f else ((v - min) / (max - min)).coerceIn(0f, 1f)
        }

    companion object {
        const val WIDGETS_PER_PAGE = 6

        fun fromId(id: String): WidgetType? = entries.find { it.id == id }

        /** Tous les types proposés dans le sélecteur d'une case. */
        fun pickerChoices(): List<WidgetType> = entries.toList()

        /** Types regroupés par catégorie (ordre des catégories puis ordre d'apparition). */
        fun groupedPickerChoices(): List<Pair<WidgetCategory, List<WidgetType>>> =
            WidgetCategory.entries.mapNotNull { cat ->
                val types = entries.filter { it.category == cat }
                if (types.isEmpty()) null else cat to types
            }

        fun defaultPage(): List<WidgetType> = listOf(
            SPEED, RPM, TURBO_PRESSURE, WATER_TEMP, OIL_TEMP, LATERAL_G
        )

        fun allAvailable(): List<WidgetType> = entries.toList()
    }

    /** Rendu skeuomorphique Car App. Les nouveaux types retombent sur une jauge générique. */
    fun toTelemetrySlot(): TelemetryWidgetSlot = when (this) {
        SPEED -> TelemetryWidgetSlot.SPEED_ANALOG
        RPM -> TelemetryWidgetSlot.RPM_LED_BAR
        TURBO_PRESSURE -> TelemetryWidgetSlot.TURBO_ANALOG
        WATER_TEMP -> TelemetryWidgetSlot.WATER_TEMP
        OIL_TEMP -> TelemetryWidgetSlot.OIL_TEMP
        LATERAL_G -> TelemetryWidgetSlot.G_FORCE_METER
        AFR -> TelemetryWidgetSlot.AFR_BAR
        IGNITION -> TelemetryWidgetSlot.IGNITION_ADVANCE
        ENGINE_LOAD -> TelemetryWidgetSlot.ENGINE_LOAD
        THROTTLE -> TelemetryWidgetSlot.THROTTLE
        DELTA_CHRONO -> TelemetryWidgetSlot.DELTA_CHRONO
        O2_VOLTAGE -> TelemetryWidgetSlot.O2_VOLTAGE
        LAP_TIME -> TelemetryWidgetSlot.LAP_TIME
        else -> TelemetryWidgetSlot.ENGINE_LOAD
    }
}

private fun gearLabel(g: Int): String = when {
    g <= 0 -> "N"
    else -> g.toString()
}

private fun tpmsLabel(v: Float): String = if (v >= 0.5f) "LOW" else "OK"

/** Texte formaté complet (valeur + unité), utilisé par les pickers et le Car App. */
fun WidgetType.formatValue(data: TelemetryData): String = when (this) {
    WidgetType.LAP_TIME -> formatMs(data.lapTimeMs)
    WidgetType.DELTA_CHRONO -> {
        val sign = if (data.deltaChronoMs >= 0) "+" else ""
        "$sign${formatMs(kotlin.math.abs(data.deltaChronoMs))}"
    }
    WidgetType.GEAR -> gearLabel(data.gear)
    WidgetType.GEAR_PRE -> gearLabel(data.gearPre)
    WidgetType.TPMS_FL, WidgetType.TPMS_FR, WidgetType.TPMS_RL, WidgetType.TPMS_RR ->
        tpmsLabel(value(data))
    WidgetType.TIRE_MONITOR, WidgetType.TIRE_WHEEL_FL, WidgetType.TIRE_WHEEL_FR,
    WidgetType.TIRE_WHEEL_RL, WidgetType.TIRE_WHEEL_RR,
    WidgetType.TIRE_PRESS_FL, WidgetType.TIRE_PRESS_FR,
    WidgetType.TIRE_PRESS_RL, WidgetType.TIRE_PRESS_RR,
    WidgetType.TIRE_TEMP_FL, WidgetType.TIRE_TEMP_FR,
    WidgetType.TIRE_TEMP_RL, WidgetType.TIRE_TEMP_RR,
    WidgetType.TPMS_FL, WidgetType.TPMS_FR, WidgetType.TPMS_RL, WidgetType.TPMS_RR ->
        tireWheelSnapshot(data)?.let { snap ->
            "${snap.pressureText} bar · ${snap.tempText}°C"
        } ?: "—"
    else -> {
        val num = "%.${decimals}f".format(value(data))
        if (unit.isEmpty()) num else "$num $unit"
    }
}

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = (ms % 1000) / 10
    return if (minutes > 0) {
        "%d:%02d.%02d".format(minutes, seconds, millis)
    } else {
        "%d.%02d".format(seconds, millis)
    }
}
