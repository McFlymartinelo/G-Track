package com.gtrack.ui.automotive



import androidx.compose.ui.graphics.Color

import com.gtrack.telemetry.TelemetryData

import com.gtrack.widgets.WidgetType



object EpureColors {

    val BgMain = Color(0xFF0F0F12)

    val BgCard = Color(0xFF13131A)

    val BgHeader = Color(0xFF111111)

    val HeaderBorder = Color(0xFF1E1E1E)

    val GridGap = Color(0xFF1A1A22)

    val BarTrack = Color(0xFF1E1E28)

    val Red = Color(0xFFC0392B)

    val Green = Color(0xFF27AE60)

    val Blue = Color(0xFF2980B9)

    val Orange = Color(0xFFE67E22)

    val TextPrimary = Color(0xFFFFFFFF)

    val TextSecondary = Color(0xFF4A4A5E)

    val TextLabel = Color(0xFF3A3A50)

    val Grey = Color(0xFF888888)

}



data class EpureWidgetPresentation(

    val fraction: Float,

    val accentColor: Color,

    val valueColor: Color

)



fun WidgetType.epurePresentation(telemetry: TelemetryData): EpureWidgetPresentation = when (this) {

    WidgetType.SPEED -> EpureWidgetPresentation(

        fraction = (telemetry.speedKmh / 255f).coerceIn(0f, 1f),

        accentColor = EpureColors.Red,

        valueColor = EpureColors.Red

    )

    WidgetType.RPM -> {

        val color = if (telemetry.rpm >= 6000) EpureColors.Orange else EpureColors.Green

        EpureWidgetPresentation(

            fraction = (telemetry.rpm.toFloat() / 7200f).coerceIn(0f, 1f),

            accentColor = color,

            valueColor = color

        )

    }

    WidgetType.TURBO_PRESSURE -> EpureWidgetPresentation(

        fraction = (telemetry.turboPressureBar / 2f).coerceIn(0f, 1f),

        accentColor = EpureColors.Blue,

        valueColor = EpureColors.Blue

    )

    WidgetType.WATER_TEMP -> {

        val color = when {

            telemetry.waterTempC > 110f -> EpureColors.Red

            telemetry.waterTempC >= 100f -> EpureColors.Orange

            else -> EpureColors.Green

        }

        EpureWidgetPresentation(

            fraction = (telemetry.waterTempC / 130f).coerceIn(0f, 1f),

            accentColor = color,

            valueColor = color

        )

    }

    WidgetType.OIL_TEMP -> {

        val color = when {

            telemetry.oilTempC > 120f -> EpureColors.Red

            telemetry.oilTempC >= 100f -> EpureColors.Orange

            else -> EpureColors.Green

        }

        EpureWidgetPresentation(

            fraction = (telemetry.oilTempC / 140f).coerceIn(0f, 1f),

            accentColor = color,

            valueColor = color

        )

    }

    WidgetType.LATERAL_G -> {

        val g = kotlin.math.abs(telemetry.lateralGForce)

        val color = when {

            g > 1f -> EpureColors.Red

            g >= 0.5f -> EpureColors.Orange

            else -> EpureColors.Grey

        }

        EpureWidgetPresentation(

            fraction = (g / 2f).coerceIn(0f, 1f),

            accentColor = color,

            valueColor = color

        )

    }

    else -> {

        val color = if (tirePressureAlert(telemetry)) EpureColors.Red else categoryColor(category)

        EpureWidgetPresentation(

            fraction = fraction(telemetry),

            accentColor = color,

            valueColor = color

        )

    }

}



private fun categoryColor(category: com.gtrack.widgets.WidgetCategory): Color = when (category) {

    com.gtrack.widgets.WidgetCategory.VITESSE -> EpureColors.Red

    com.gtrack.widgets.WidgetCategory.MOTEUR -> EpureColors.Green

    com.gtrack.widgets.WidgetCategory.TEMPERATURE -> EpureColors.Orange

    com.gtrack.widgets.WidgetCategory.PRESSION -> EpureColors.Blue

    com.gtrack.widgets.WidgetCategory.PNEU -> EpureColors.Blue

    com.gtrack.widgets.WidgetCategory.BATTERIE -> EpureColors.Green

    com.gtrack.widgets.WidgetCategory.DYNAMIQUE -> EpureColors.Red

    com.gtrack.widgets.WidgetCategory.CHRONO -> EpureColors.Grey

}



fun WidgetType.epureIconResId(): Int = when (this) {

    WidgetType.SPEED -> com.gtrack.R.drawable.ic_pure_speed

    WidgetType.RPM -> com.gtrack.R.drawable.ic_pure_rpm

    WidgetType.TURBO_PRESSURE -> com.gtrack.R.drawable.ic_pure_turbo

    WidgetType.WATER_TEMP -> com.gtrack.R.drawable.ic_pure_water

    WidgetType.OIL_TEMP -> com.gtrack.R.drawable.ic_pure_oil

    WidgetType.LATERAL_G -> com.gtrack.R.drawable.ic_pure_g_lateral

    else -> com.gtrack.R.drawable.ic_pure_speed

}


