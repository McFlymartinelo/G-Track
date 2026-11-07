package com.gtrack.ui.automotive



import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.gtrack.R

import com.gtrack.telemetry.TelemetryData

import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import com.gtrack.widgets.usesTireMonitorUi

import kotlinx.coroutines.delay

import kotlin.math.PI

import kotlin.math.abs

import kotlin.math.cos

import kotlin.math.min

import kotlin.math.sin

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale



private const val GTI_SPEED_MAX = 255f
private const val GTI_TURBO_MAX = 2f
private const val GTI_RPM_REDLINE = 7200
private const val GTI_RPM_IDLE = 850
private val GtiRpmBarHeights = floatArrayOf(0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f, 0.78f, 0.68f, 0.58f, 0.48f)



private val GtiUnitColor = Color(0xFF4A4A5E)

private val GtiLabelColor = Color(0xFF3A3A50)

private val GtiBarTrack = Color(0xFF1E1E28)

private val GtiGCardTitleColor = Color(0xFF707070)

private val GtiGRingColor = Color(0xFF2A2A2E)

private val GtiGCrossColor = Color(0xFF333336)

private val GtiArcTrack = Color(0xFF2C2C30)

private val GtiScaleLabelColor = Color(0xFF555555)



@Composable

fun GtiTrackPageChrome(

    pageNumber: Int,

    sessionActive: Boolean,

    modifier: Modifier = Modifier,

    content: @Composable () -> Unit

) {

    Column(

        modifier = modifier

            .fillMaxSize()

            .background(EpureColors.BgMain)

    ) {

        Box(

            modifier = Modifier

                .weight(1f)

                .fillMaxWidth()

                .background(EpureColors.GridGap)

        ) {

            content()

        }

    }

}



@Composable

fun GtiTrackPageHeader(

    pageNumber: Int,

    sessionActive: Boolean,

    modifier: Modifier = Modifier

) {

    var clock by remember { mutableStateOf(formatGtiClock()) }

    LaunchedEffect(Unit) {

        while (true) {

            clock = formatGtiClock()

            delay(1000)

        }

    }

    Row(

        modifier = modifier

            .height(44.dp)

            .background(EpureColors.BgHeader)

            .padding(horizontal = 20.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Row(

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(10.dp)

        ) {

            Box(

                modifier = Modifier

                    .clip(RoundedCornerShape(4.dp))

                    .background(EpureColors.Red)

                    .padding(horizontal = 9.dp, vertical = 3.dp)

            ) {

                Text(

                    text = stringResource(R.string.gti_badge),

                    color = EpureColors.TextPrimary,

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Medium

                )

            }

            Text(

                text = stringResource(R.string.gti_page_title, pageNumber),

                color = EpureColors.TextPrimary,

                fontSize = 13.sp,

                fontWeight = FontWeight.Medium

            )

        }

        Row(

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(14.dp)

        ) {

            if (sessionActive) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(6.dp)

                ) {

                    Box(

                        modifier = Modifier

                            .size(7.dp)

                            .clip(CircleShape)

                            .background(EpureColors.Red)

                    )

                    Text(

                        text = "REC",

                        color = EpureColors.Red,

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Medium,

                        letterSpacing = 0.8.sp

                    )

                }

            }

            Text(

                text = clock,

                color = EpureColors.Grey,

                style = TextStyle(

                    fontSize = 13.sp,

                    fontWeight = FontWeight.Medium,

                    fontFeatureSettings = "tnum"

                )

            )

        }

    }

}



@Composable

fun GtiTrackWidgetCell(

    widgetType: WidgetType,

    telemetry: TelemetryData,

    modifier: Modifier = Modifier

) {

    when (widgetType) {

        WidgetType.SPEED -> GtiSpeedWidgetCell(telemetry = telemetry, modifier = modifier)

        WidgetType.TURBO_PRESSURE -> GtiTurboWidgetCell(telemetry = telemetry, modifier = modifier)

        WidgetType.WATER_TEMP -> GtiWaterTempWidgetCell(telemetry = telemetry, modifier = modifier)

        WidgetType.OIL_TEMP -> GtiOilTempWidgetCell(telemetry = telemetry, modifier = modifier)

        WidgetType.RPM -> GtiRpmWidgetCell(telemetry = telemetry, modifier = modifier)

        WidgetType.LATERAL_G -> GtiLateralGWidgetCell(telemetry = telemetry, modifier = modifier)

        else -> if (widgetType.usesTireMonitorUi()) {
            TireWidgetCell(
                widgetType = widgetType,
                telemetry = telemetry,
                visualStyle = WidgetVisualStyle.GTI,
                modifier = modifier
            )
        } else {
            GtiDefaultWidgetCell(widgetType = widgetType, telemetry = telemetry, modifier = modifier)
        }

    }

}



@Composable

private fun GtiDefaultWidgetCell(

    widgetType: WidgetType,

    telemetry: TelemetryData,

    modifier: Modifier = Modifier

) {

    val parts = widgetType.valueParts(telemetry)

    val pres = widgetType.gtiPresentation(telemetry)

    val unitText = parts.unit?.lowercase().orEmpty()

    val labelText = widgetType.displayTitle().uppercase()



    Column(

        modifier = modifier

            .fillMaxSize()

            .padding(horizontal = 16.dp, vertical = 14.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        Icon(

            painter = painterResource(widgetType.gtiIconResId()),

            contentDescription = null,

            tint = pres.sensorColor,

            modifier = Modifier.size(24.dp)

        )



        Spacer(modifier = Modifier.height(8.dp))



        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(

                text = parts.main,

                style = TextStyle(

                    color = pres.sensorColor,

                    fontSize = 30.sp,

                    fontWeight = FontWeight.Medium,

                    fontFeatureSettings = "tnum"

                ),

                maxLines = 1,

                overflow = TextOverflow.Ellipsis

            )

            if (unitText.isNotEmpty()) {

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = unitText, color = GtiUnitColor, fontSize = 11.sp)

            }

        }



        Spacer(modifier = Modifier.height(8.dp))



        LinearProgressIndicator(

            progress = { pres.fraction.coerceIn(0f, 1f) },

            modifier = Modifier

                .fillMaxWidth()

                .height(3.dp)

                .clip(RoundedCornerShape(2.dp)),

            color = pres.sensorColor,

            trackColor = GtiBarTrack

        )



        Spacer(modifier = Modifier.height(6.dp))



        if (unitText.isNotEmpty()) {

            Text(

                text = unitText,

                color = GtiUnitColor,

                fontSize = 11.sp,

                textAlign = TextAlign.Center

            )

        }



        Text(

            text = labelText,

            color = GtiLabelColor,

            fontSize = 10.sp,

            letterSpacing = 1.sp,

            textAlign = TextAlign.Center,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis

        )

    }

}



@Composable
private fun GtiArcScaleWidgetCell(
    title: String,
    valueMain: String,
    unit: String?,
    fraction: Float,
    accentColor: Color,
    maxLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = GtiGCardTitleColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GtiSemiArcGauge(
                fraction = fraction,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = valueMain,
                color = accentColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            unit?.let {
                Text(
                    text = it,
                    color = GtiUnitColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
                )
            }
        }

        GtiHorizontalScaleBar(
            fraction = fraction,
            color = accentColor,
            leftLabel = "0",
            rightLabel = maxLabel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GtiSpeedWidgetCell(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val parts = WidgetType.SPEED.valueParts(telemetry)
    GtiArcScaleWidgetCell(
        title = WidgetType.SPEED.displayTitle().uppercase(),
        valueMain = parts.main,
        unit = parts.unit,
        fraction = (telemetry.speedKmh / GTI_SPEED_MAX).coerceIn(0f, 1f),
        accentColor = EpureColors.Red,
        maxLabel = "max 255",
        modifier = modifier
    )
}

@Composable
fun GtiTurboWidgetCell(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val parts = WidgetType.TURBO_PRESSURE.valueParts(telemetry)
    GtiArcScaleWidgetCell(
        title = WidgetType.TURBO_PRESSURE.displayTitle().uppercase(),
        valueMain = parts.main,
        unit = parts.unit,
        fraction = (telemetry.turboPressureBar / GTI_TURBO_MAX).coerceIn(0f, 1f),
        accentColor = EpureColors.Blue,
        maxLabel = "max 2.0",
        modifier = modifier
    )
}

@Composable
fun GtiRpmWidgetCell(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val parts = WidgetType.RPM.valueParts(telemetry)
    val fraction = (telemetry.rpm.toFloat() / GTI_RPM_REDLINE).coerceIn(0f, 1f)
    val accentColor = if (telemetry.rpm >= 6000) EpureColors.Orange else EpureColors.Green

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gti_widget_rpm_title),
                color = GtiGCardTitleColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        GtiRpmMountainBars(
            fraction = fraction,
            activeColor = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = parts.main,
                color = accentColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Text(
                text = parts.unit.orEmpty().lowercase(),
                color = GtiUnitColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.gti_rpm_idle, GTI_RPM_IDLE),
                color = GtiScaleLabelColor,
                fontSize = 8.sp
            )
            Text(
                text = stringResource(R.string.gti_rpm_redline, GTI_RPM_REDLINE),
                color = GtiScaleLabelColor,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun GtiRpmMountainBars(
    fraction: Float,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val barCount = GtiRpmBarHeights.size
    val filled = (fraction * barCount).toInt().coerceIn(0, barCount)
    val barShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        GtiRpmBarHeights.forEachIndexed { index, heightFrac ->
            val active = index < filled
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFrac.coerceIn(0f, 1f))
                    .clip(barShape)
                    .background(if (active) activeColor else GtiBarTrack)
            )
        }
    }
}

@Composable
fun GtiWaterTempWidgetCell(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val parts = WidgetType.WATER_TEMP.valueParts(telemetry)
    val pres = WidgetType.WATER_TEMP.gtiTempPresentation(telemetry) ?: return
    GtiTempWidgetCell(
        title = WidgetType.WATER_TEMP.displayTitle().uppercase(),
        valueMain = parts.main,
        unit = parts.unit,
        presentation = pres,
        modifier = modifier
    )
}

@Composable
fun GtiOilTempWidgetCell(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val parts = WidgetType.OIL_TEMP.valueParts(telemetry)
    val pres = WidgetType.OIL_TEMP.gtiTempPresentation(telemetry) ?: return
    GtiTempWidgetCell(
        title = WidgetType.OIL_TEMP.displayTitle().uppercase(),
        valueMain = parts.main,
        unit = parts.unit,
        presentation = pres,
        modifier = modifier
    )
}

@Composable
private fun GtiTempWidgetCell(
    title: String,
    valueMain: String,
    unit: String?,
    presentation: GtiTempPresentation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = GtiGCardTitleColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(presentation.statusDotColor)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_car_temp),
                contentDescription = null,
                tint = presentation.accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueMain,
                    color = presentation.accentColor,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                unit?.let {
                    Text(
                        text = it,
                        color = GtiUnitColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
                    )
                }
            }
        }

        GtiTempScaleBar(
            fraction = presentation.fraction,
            color = presentation.accentColor,
            statusText = stringResource(presentation.statusTextRes),
            statusTextColor = presentation.statusTextColor,
            maxLabel = presentation.maxLabel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GtiTempScaleBar(
    fraction: Float,
    color: Color,
    statusText: String,
    statusTextColor: Color,
    maxLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            val w = size.width
            val h = size.height
            drawLine(
                GtiBarTrack,
                Offset(0f, h / 2f),
                Offset(w, h / 2f),
                strokeWidth = h,
                cap = StrokeCap.Round
            )
            drawLine(
                color,
                Offset(0f, h / 2f),
                Offset(w * fraction.coerceIn(0f, 1f), h / 2f),
                strokeWidth = h,
                cap = StrokeCap.Round
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(statusText, color = statusTextColor, fontSize = 8.sp)
            Text(maxLabel, color = GtiScaleLabelColor, fontSize = 8.sp)
        }
    }
}

@Composable
fun GtiSemiArcGauge(

    fraction: Float,

    accentColor: Color,

    modifier: Modifier = Modifier

) {

    Canvas(modifier = modifier) {

        val cx = size.width / 2f

        val cy = size.height * 0.92f

        val r = min(size.width, size.height) * 0.78f

        val startAngle = 180f

        val sweep = 180f

        val arcSize = Size(r * 2f, r * 2f)

        val topLeft = Offset(cx - r, cy - r)

        val strokeW = r * 0.13f

        val valueStrokeW = r * 0.095f

        val f = fraction.coerceIn(0f, 1f)



        drawArc(

            color = GtiArcTrack,

            startAngle = startAngle,

            sweepAngle = sweep,

            useCenter = false,

            topLeft = topLeft,

            size = arcSize,

            style = Stroke(width = strokeW, cap = StrokeCap.Round)

        )

        if (f > 0.01f) {

            drawArc(

                color = accentColor,

                startAngle = startAngle,

                sweepAngle = sweep * f,

                useCenter = false,

                topLeft = topLeft,

                size = arcSize,

                style = Stroke(width = valueStrokeW, cap = StrokeCap.Round)

            )

        }



        val angleRad = Math.toRadians((startAngle + sweep * f).toDouble()).toFloat()

        val needleLen = r * 0.72f

        val tipX = cx + cos(angleRad) * needleLen

        val tipY = cy + sin(angleRad) * needleLen



        drawArc(

            color = Color.White.copy(alpha = 0.9f),

            startAngle = 200f,

            sweepAngle = 140f,

            useCenter = false,

            topLeft = Offset(cx - r * 0.14f, cy - r * 0.06f),

            size = Size(r * 0.28f, r * 0.2f),

            style = Stroke(width = 2.5f, cap = StrokeCap.Round)

        )

        drawLine(

            color = Color.White,

            start = Offset(cx, cy),

            end = Offset(tipX, tipY),

            strokeWidth = 2.5f,

            cap = StrokeCap.Round

        )

        drawCircle(color = Color.White, radius = 3f, center = Offset(cx, cy))

    }

}



@Composable

fun GtiHorizontalScaleBar(

    fraction: Float,

    color: Color,

    leftLabel: String,

    rightLabel: String,

    modifier: Modifier = Modifier

) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {

        Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {

            val w = size.width

            val h = size.height

            drawLine(

                GtiBarTrack,

                Offset(0f, h / 2f),

                Offset(w, h / 2f),

                strokeWidth = h,

                cap = StrokeCap.Round

            )

            drawLine(

                color,

                Offset(0f, h / 2f),

                Offset(w * fraction.coerceIn(0f, 1f), h / 2f),

                strokeWidth = h,

                cap = StrokeCap.Round

            )

        }

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text(leftLabel, color = GtiScaleLabelColor, fontSize = 8.sp)

            Text(rightLabel, color = GtiScaleLabelColor, fontSize = 8.sp)

        }

    }

}



@Composable

fun GtiLateralGWidgetCell(

    telemetry: TelemetryData,

    modifier: Modifier = Modifier

) {

    val parts = WidgetType.LATERAL_G.valueParts(telemetry)

    val g = telemetry.lateralGForce

    val statusDotColor = gtiLateralGStatusDotColor(g)

    val accentColor = gtiLateralGAccentColor(g)



    Column(

        modifier = modifier

            .fillMaxSize()

            .padding(horizontal = 12.dp, vertical = 10.dp),

        verticalArrangement = Arrangement.SpaceBetween

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Text(

                text = WidgetType.LATERAL_G.displayTitle().uppercase(),

                color = GtiGCardTitleColor,

                fontSize = 9.sp,

                fontWeight = FontWeight.Medium,

                letterSpacing = 0.9.sp,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

                modifier = Modifier.weight(1f)

            )

            Box(

                modifier = Modifier

                    .size(6.dp)

                    .clip(CircleShape)

                    .background(statusDotColor)

            )

        }



        Box(

            modifier = Modifier

                .weight(1f)

                .fillMaxWidth(),

            contentAlignment = Alignment.Center

        ) {

            GtiGForceRadar(

                gLat = g,

                accentColor = accentColor,

                modifier = Modifier.size(88.dp)

            )

        }



        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.Bottom

        ) {

            Text(

                text = parts.main,

                color = EpureColors.TextPrimary,

                fontSize = 34.sp,

                fontWeight = FontWeight.Bold,

                style = TextStyle(fontFeatureSettings = "tnum")

            )

            Text(

                text = parts.unit.orEmpty(),

                color = GtiUnitColor,

                fontSize = 14.sp,

                modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)

            )

        }

    }

}



@Composable

fun GtiGForceRadar(

    gLat: Float,

    modifier: Modifier = Modifier,

    accentColor: Color = EpureColors.Red

) {

    Canvas(modifier = modifier) {

        val cx = size.width / 2f

        val cy = size.height / 2f

        val r = min(size.width, size.height) / 2f * 0.88f



        listOf(1f, 0.58f).forEach { scale ->

            drawCircle(

                color = GtiGRingColor,

                radius = r * scale,

                center = Offset(cx, cy),

                style = Stroke(width = 1.2f)

            )

        }

        drawLine(GtiGCrossColor, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1f)

        drawLine(GtiGCrossColor, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1f)



        val clamped = gLat.coerceIn(-2f, 2f)

        val angle = (clamped / 2f) * (PI / 2).toFloat()

        val dotX = cx + sin(angle) * r * 0.68f

        val dotY = cy - cos(angle) * r * 0.68f



        drawLine(

            color = accentColor.copy(alpha = 0.45f),

            start = Offset(cx, cy),

            end = Offset(dotX, dotY),

            strokeWidth = 2f,

            cap = StrokeCap.Round

        )

        drawCircle(

            color = accentColor.copy(alpha = 0.35f),

            radius = 7f,

            center = Offset(dotX, dotY)

        )

        drawCircle(

            color = accentColor,

            radius = 4.5f,

            center = Offset(dotX, dotY)

        )

    }

}



private fun gtiLateralGStatusDotColor(g: Float): Color = when {

    abs(g) > 1f -> EpureColors.Red

    abs(g) >= 0.5f -> EpureColors.Orange

    else -> EpureColors.Grey

}



private fun gtiLateralGAccentColor(g: Float): Color = when {

    abs(g) > 1f -> EpureColors.Red

    abs(g) >= 0.5f -> EpureColors.Orange

    else -> EpureColors.Red

}



private fun formatGtiClock(): String =

    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())


