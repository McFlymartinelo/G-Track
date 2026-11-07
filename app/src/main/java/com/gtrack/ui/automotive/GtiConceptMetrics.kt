package com.gtrack.ui.automotive



import androidx.compose.ui.graphics.Color

import com.gtrack.auto.theme.CarVisualTheme

import com.gtrack.telemetry.TelemetryData

import com.gtrack.widgets.WidgetType



data class GtiWidgetPresentation(

    val fraction: Float,

    val sensorColor: Color

)



data class GtiTempPresentation(

    val fraction: Float,

    val accentColor: Color,

    val statusDotColor: Color,

    val statusTextRes: Int,

    val statusTextColor: Color,

    val maxLabel: String

)



fun WidgetType.gtiPresentation(telemetry: TelemetryData): GtiWidgetPresentation {

    val epure = epurePresentation(telemetry)

    return GtiWidgetPresentation(

        fraction = epure.fraction,

        sensorColor = epure.valueColor

    )

}



fun WidgetType.gtiTempPresentation(telemetry: TelemetryData): GtiTempPresentation? = when (this) {

    WidgetType.WATER_TEMP -> waterTempGtiPresentation(telemetry.waterTempC)

    WidgetType.OIL_TEMP -> oilTempGtiPresentation(telemetry.oilTempC)

    else -> null

}



private fun waterTempGtiPresentation(tempC: Float): GtiTempPresentation {

    val warn = tempC >= CarVisualTheme.WATER_TEMP_WARNING_C

    val critical = tempC > 110f

    val accentColor = when {

        critical -> EpureColors.Red

        warn -> EpureColors.Orange

        else -> EpureColors.Red

    }

    val statusDotColor = when {

        critical -> EpureColors.Red

        warn -> EpureColors.Orange

        else -> EpureColors.Green

    }

    val statusTextRes = if (warn || critical) {

        com.gtrack.R.string.gti_status_attention

    } else {

        com.gtrack.R.string.gti_status_normal

    }

    val statusTextColor = when {

        critical -> EpureColors.Red

        warn -> EpureColors.Orange

        else -> EpureColors.Green

    }

    return GtiTempPresentation(

        fraction = (tempC / 120f).coerceIn(0f, 1f),

        accentColor = accentColor,

        statusDotColor = statusDotColor,

        statusTextRes = statusTextRes,

        statusTextColor = statusTextColor,

        maxLabel = "max 120°C"

    )

}



private fun oilTempGtiPresentation(tempC: Float): GtiTempPresentation {

    val warn = tempC >= CarVisualTheme.OIL_TEMP_WARNING_C

    val critical = tempC > 120f

    val accentColor = when {

        critical -> EpureColors.Red

        else -> EpureColors.Orange

    }

    val statusDotColor = when {

        critical -> EpureColors.Red

        warn -> EpureColors.Orange

        else -> EpureColors.Orange

    }

    val statusTextRes = if (warn || critical) {

        com.gtrack.R.string.gti_status_attention

    } else {

        com.gtrack.R.string.gti_status_normal

    }

    val statusTextColor = when {

        critical -> EpureColors.Red

        warn -> EpureColors.Orange

        else -> EpureColors.Green

    }

    return GtiTempPresentation(

        fraction = (tempC / 135f).coerceIn(0f, 1f),

        accentColor = accentColor,

        statusDotColor = statusDotColor,

        statusTextRes = statusTextRes,

        statusTextColor = statusTextColor,

        maxLabel = "max 135°C"

    )

}



fun WidgetType.gtiIconResId(): Int = epureIconResId()


