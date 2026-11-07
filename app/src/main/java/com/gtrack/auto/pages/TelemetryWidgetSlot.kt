package com.gtrack.auto.pages

/**
 * Emplacement 2×3 d'un widget sur les pages Data 1 et Data 2.
 */
enum class TelemetryWidgetSlot {
    SPEED_ANALOG,
    RPM_LED_BAR,
    TURBO_ANALOG,
    WATER_TEMP,
    OIL_TEMP,
    G_FORCE_METER,
    AFR_BAR,
    IGNITION_ADVANCE,
    ENGINE_LOAD,
    THROTTLE,
    DELTA_CHRONO,
    O2_VOLTAGE,
    LAP_TIME;

    companion object {
        val DATA_1_LAYOUT = listOf(
            SPEED_ANALOG,
            RPM_LED_BAR,
            TURBO_ANALOG,
            WATER_TEMP,
            OIL_TEMP,
            G_FORCE_METER
        )

        val DATA_2_LAYOUT = listOf(
            AFR_BAR,
            IGNITION_ADVANCE,
            ENGINE_LOAD,
            THROTTLE,
            DELTA_CHRONO,
            O2_VOLTAGE
        )
    }
}
