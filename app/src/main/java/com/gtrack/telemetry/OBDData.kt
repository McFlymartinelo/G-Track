package com.gtrack.telemetry

import kotlin.math.roundToInt

data class OBDData(
    val speed: Int = 0,
    val rpm: Int = 0,
    val turbo: Float = 0f,
    val waterTemp: Int = 0,
    val oilTemp: Int = 0,
    val gForce: Float = 0f
)

fun OBDData.toTelemetryData(base: TelemetryData = TelemetryService.createIdleSample()): TelemetryData =
    base.copy(
        speedKmh = speed.toFloat(),
        rpm = rpm,
        turboPressureBar = turbo,
        waterTempC = waterTemp.toFloat(),
        oilTempC = oilTemp.toFloat(),
        lateralGForce = gForce
    )

fun TelemetryData.toOBDData(): OBDData = OBDData(
    speed = speedKmh.roundToInt(),
    rpm = rpm,
    turbo = turboPressureBar,
    waterTemp = waterTempC.roundToInt(),
    oilTemp = oilTempC.roundToInt(),
    gForce = lateralGForce
)
