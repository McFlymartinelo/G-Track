package com.gtrack.ui.automotive

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal const val SportArcStart = 130f
internal const val SportArcSweep = 280f

internal fun DrawScope.drawGaugeIcon(
    icon: ImageBitmap?,
    cx: Float,
    cy: Float,
    size: Float,
    tint: Color = Color.White
) {
    if (icon == null) return
    val half = size / 2f
    drawImage(
        image = icon,
        dstOffset = IntOffset((cx - half).toInt(), (cy - half).toInt()),
        dstSize = IntSize(size.toInt(), size.toInt()),
        colorFilter = ColorFilter.tint(tint)
    )
}

internal fun DrawScope.drawGaugeScaleLabels(
    cx: Float,
    cy: Float,
    radius: Float,
    labels: List<String>,
    textColor: Color = Color.White.copy(alpha = 0.85f),
    startAngle: Float = SportArcStart,
    sweep: Float = SportArcSweep
) {
    if (labels.isEmpty()) return
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        labels.forEachIndexed { index, label ->
            val frac = index.toFloat() / (labels.size - 1).coerceAtLeast(1)
            val angleRad = Math.toRadians((startAngle + sweep * frac).toDouble()).toFloat()
            val labelR = radius * 0.72f
            val x = cx + cos(angleRad) * labelR
            val y = cy + sin(angleRad) * labelR + paint.textSize * 0.35f
            drawText(label, x, y, paint)
        }
    }
}

internal fun DrawScope.drawGaugeUnitLabel(
    cx: Float,
    cy: Float,
    radius: Float,
    unit: String,
    color: Color = Color.White.copy(alpha = 0.9f)
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawText(unit.uppercase(), cx, cy + paint.textSize * 0.35f, paint)
    }
}

internal fun DrawScope.drawGaugeValueLabel(
    cx: Float,
    cy: Float,
    radius: Float,
    value: String,
    unit: String?,
    valueColor: Color = Color.White,
    unitColor: Color = Color.White.copy(alpha = 0.75f)
) {
    drawContext.canvas.nativeCanvas.apply {
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valueY = cy + valuePaint.textSize * 0.12f
        drawText(value, cx, valueY, valuePaint)
        unit?.let {
            val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = unitColor.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = radius * 0.16f
            }
            drawText(it, cx, valueY + radius * 0.22f, unitPaint)
        }
    }
}

/** GTI RS : anneau rouge, arc blanc→orange, échelle, icône + unité au centre. */
internal fun DrawScope.drawGtiSportDial(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float,
    unitLabel: String,
    scaleLabels: List<String>,
    icon: ImageBitmap?,
    warningFraction: Float = 0.72f
) {
    val frac = ((value - valueMin) / (valueMax - valueMin)).coerceIn(0f, 1f)
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    val arcStroke = radius * 0.16f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A)),
            center = Offset(cx, cy),
            radius = radius * 0.92f
        ),
        radius = radius * 0.82f,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = palette.accent.copy(alpha = 0.35f),
        radius = radius * 0.98f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.05f)
    )
    drawCircle(
        color = palette.accent,
        radius = radius * 0.94f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
    )

    drawArc(
        color = Color(0xFF1E1E1E),
        startAngle = SportArcStart,
        sweepAngle = SportArcSweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = arcStroke * 0.45f, cap = StrokeCap.Round)
    )

    if (frac > 0.01f) {
        val progressSweep = SportArcSweep * frac
        val warnSweep = SportArcSweep * warningFraction.coerceIn(0.1f, 0.95f)
        val whiteSweep = min(progressSweep, warnSweep)
        val hotSweep = (progressSweep - warnSweep).coerceAtLeast(0f)

        if (whiteSweep > 0.5f) {
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = SportArcStart,
                sweepAngle = whiteSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStroke * 1.35f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = SportArcStart,
                sweepAngle = whiteSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )
        }
        if (hotSweep > 0.5f) {
            drawArc(
                color = palette.segmentYellow.copy(alpha = 0.25f),
                startAngle = SportArcStart + whiteSweep,
                sweepAngle = hotSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStroke * 1.35f, cap = StrokeCap.Round)
            )
            drawArc(
                color = palette.segmentYellow,
                startAngle = SportArcStart + whiteSweep,
                sweepAngle = hotSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )
        }
    }

    drawGaugeScaleLabels(cx, cy, radius, scaleLabels)
    drawGaugeIcon(icon, cx, cy - radius * 0.06f, radius * 0.3f)
    drawGaugeUnitLabel(cx, cy + radius * 0.24f, radius, unitLabel)
}

/** OEM jaune : anneau métal, arc jaune, valeur + icône centrées. */
internal fun DrawScope.drawPremiumDial(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    value: Float,
    valueMin: Float,
    valueMax: Float,
    displayValue: String,
    displayUnit: String?,
    scaleLabels: List<String>,
    icon: ImageBitmap?
) {
    val frac = ((value - valueMin) / (valueMax - valueMin)).coerceIn(0f, 1f)
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(cx - radius, cy - radius)
    val arcStroke = radius * 0.14f
    val yellow = palette.accent

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A2A2A), Color(0xFF141414), Color(0xFF080808)),
            center = Offset(cx, cy - radius * 0.1f),
            radius = radius
        ),
        radius = radius * 0.8f,
        center = Offset(cx, cy)
    )

    listOf(
        Color(0xFF90A4AE) to radius * 0.06f,
        Color(0xFF607D8B) to radius * 0.035f,
        Color(0xFFB0BEC5) to 2.5f
    ).forEach { (color, width) ->
        drawCircle(
            color = color,
            radius = radius * 0.95f,
            center = Offset(cx, cy),
            style = Stroke(width = width)
        )
    }

    drawArc(
        color = Color(0xFF252525),
        startAngle = SportArcStart,
        sweepAngle = SportArcSweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = arcStroke * 0.5f, cap = StrokeCap.Round)
    )

    if (frac > 0.01f) {
        val progressSweep = SportArcSweep * frac
        drawArc(
            color = yellow.copy(alpha = 0.25f),
            startAngle = SportArcStart,
            sweepAngle = progressSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = arcStroke * 1.4f, cap = StrokeCap.Round)
        )
        drawArc(
            color = yellow,
            startAngle = SportArcStart,
            sweepAngle = progressSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = arcStroke * 0.85f, cap = StrokeCap.Round)
        )
        val tipRad = Math.toRadians((SportArcStart + progressSweep).toDouble()).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = arcStroke * 0.25f,
            center = Offset(cx + cos(tipRad) * radius * 0.92f, cy + sin(tipRad) * radius * 0.92f)
        )
    }

    val tickCount = scaleLabels.size.coerceAtLeast(2) - 1
    for (i in 0..tickCount) {
        val f = i.toFloat() / tickCount.coerceAtLeast(1)
        val angleRad = Math.toRadians((SportArcStart + SportArcSweep * f).toDouble()).toFloat()
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(cx + cos(angleRad) * radius * 0.82f, cy + sin(angleRad) * radius * 0.82f),
            end = Offset(cx + cos(angleRad) * radius * 0.9f, cy + sin(angleRad) * radius * 0.9f),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
    drawGaugeScaleLabels(cx, cy, radius, scaleLabels, textColor = Color.White.copy(alpha = 0.7f))
    drawGaugeValueLabel(cx, cy - radius * 0.06f, radius, displayValue, displayUnit)
    drawGaugeIcon(icon, cx, cy + radius * 0.34f, radius * 0.2f, tint = Color.White.copy(alpha = 0.85f))
}

internal fun DrawScope.drawGtiSportGForce(
    palette: WidgetThemePalette,
    cx: Float,
    cy: Float,
    radius: Float,
    gLat: Float,
    icon: ImageBitmap?
) {
    drawGtiSportDial(
        palette = palette,
        cx = cx,
        cy = cy,
        radius = radius,
        value = (gLat + 2f) / 4f,
        valueMin = 0f,
        valueMax = 1f,
        unitLabel = "G",
        scaleLabels = listOf("-2", "-1", "0", "1", "2"),
        icon = icon,
        warningFraction = 0.8f
    )
    val angle = ((gLat.coerceIn(-2f, 2f) / 2f) * (kotlin.math.PI / 2)).toFloat()
    val dotCenter = Offset(
        cx + sin(angle) * radius * 0.58f,
        cy - cos(angle) * radius * 0.58f
    )
    val dotR = radius * 0.07f
    drawCircle(color = palette.accent, radius = dotR * 2.2f, center = dotCenter)
    drawCircle(color = Color.White, radius = dotR, center = dotCenter)
}
