package com.gtrack.ui.automotive



import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color

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

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale



@Composable

fun EpurePageChrome(

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

fun EpurePageHeader(

    sessionActive: Boolean,

    modifier: Modifier = Modifier

) {

    var clock by remember { mutableStateOf(formatEpureClock()) }

    LaunchedEffect(Unit) {

        while (true) {

            clock = formatEpureClock()

            delay(1000)

        }

    }



    Row(

        modifier = modifier

            .fillMaxWidth()

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

                text = "G-Track",

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

                    EpureRecPulseDot()

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

private fun EpureRecPulseDot() {

    val transition = rememberInfiniteTransition(label = "epure_rec_pulse")

    val alpha by transition.animateFloat(

        initialValue = 0.35f,

        targetValue = 1f,

        animationSpec = infiniteRepeatable(

            animation = tween(durationMillis = 800),

            repeatMode = RepeatMode.Reverse

        ),

        label = "epure_rec_alpha"

    )

    Box(

        modifier = Modifier

            .size(7.dp)

            .alpha(alpha)

            .clip(CircleShape)

            .background(EpureColors.Red)

    )

}



@Composable

fun EpureWidgetCell(

    widgetType: WidgetType,

    telemetry: TelemetryData,

    modifier: Modifier = Modifier

) {

    if (widgetType.usesTireMonitorUi()) {
        TireWidgetCell(
            widgetType = widgetType,
            telemetry = telemetry,
            visualStyle = WidgetVisualStyle.EPURE,
            modifier = modifier
        )
        return
    }

    val parts = widgetType.valueParts(telemetry)

    val pres = widgetType.epurePresentation(telemetry)



    Column(

        modifier = modifier

            .fillMaxSize()

            .padding(horizontal = 16.dp, vertical = 14.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.SpaceBetween

    ) {

        Icon(

            painter = painterResource(widgetType.epureIconResId()),

            contentDescription = null,

            tint = pres.valueColor,

            modifier = Modifier.size(24.dp)

        )



        Text(

            text = parts.main,

            style = TextStyle(

                color = pres.valueColor,

                fontSize = 30.sp,

                fontWeight = FontWeight.Medium,

                fontFeatureSettings = "tnum"

            ),

            maxLines = 1,

            overflow = TextOverflow.Ellipsis,

            textAlign = TextAlign.Center,

            modifier = Modifier.fillMaxWidth()

        )



        Column(

            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.fillMaxWidth()

        ) {

            EpureGaugeBar(

                fraction = pres.fraction,

                color = pres.accentColor,

                modifier = Modifier.fillMaxWidth()

            )

            parts.unit?.let { unit ->

                Text(

                    text = unit.lowercase(),

                    color = EpureColors.TextSecondary,

                    fontSize = 11.sp,

                    modifier = Modifier.padding(top = 6.dp)

                )

            }

            Text(

                text = widgetType.displayTitle().uppercase(),

                color = EpureColors.TextLabel,

                fontSize = 10.sp,

                letterSpacing = 1.sp,

                modifier = Modifier.padding(top = 4.dp),

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

                textAlign = TextAlign.Center

            )

        }

    }

}



@Composable

fun EpureGaugeBar(

    fraction: Float,

    color: Color,

    modifier: Modifier = Modifier

) {

    Canvas(

        modifier = modifier.height(3.dp)

    ) {

        val w = size.width

        val h = size.height

        val radius = 2.dp.toPx()

        drawRoundRect(

            color = EpureColors.BarTrack,

            topLeft = Offset.Zero,

            size = androidx.compose.ui.geometry.Size(w, h),

            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)

        )

        val fillW = w * fraction.coerceIn(0f, 1f)

        if (fillW > 0f) {

            drawRoundRect(

                color = color,

                topLeft = Offset.Zero,

                size = androidx.compose.ui.geometry.Size(fillW, h),

                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)

            )

        }

    }

}



private fun formatEpureClock(): String =

    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())


