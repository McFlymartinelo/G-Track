package com.gtrack.auto

import com.gtrack.auto.pages.AutoTelemetryPage
import com.gtrack.widgets.WidgetType

object CarWidgetDefaults {

    fun layoutForPage(page: AutoTelemetryPage): List<WidgetType> = when (page) {
        AutoTelemetryPage.DATA_1 -> data1()
        AutoTelemetryPage.DATA_2 -> data2()
        AutoTelemetryPage.LIVE_MAP -> data1()
    }

    fun data1(): List<WidgetType> = listOf(
        WidgetType.SPEED,
        WidgetType.RPM,
        WidgetType.TURBO_PRESSURE,
        WidgetType.WATER_TEMP,
        WidgetType.OIL_TEMP,
        WidgetType.LATERAL_G
    )

    fun data2(): List<WidgetType> = listOf(
        WidgetType.AFR,
        WidgetType.IGNITION,
        WidgetType.ENGINE_LOAD,
        WidgetType.THROTTLE,
        WidgetType.DELTA_CHRONO,
        WidgetType.O2_VOLTAGE
    )

    /** Deux pages de 6 widgets personnalisables (Data 1 / Data 2). */
    fun twoCarPages(): List<List<WidgetType>> = listOf(
        data1(),
        data2()
    )
}
