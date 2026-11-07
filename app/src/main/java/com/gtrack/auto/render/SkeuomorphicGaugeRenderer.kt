package com.gtrack.auto.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.gtrack.R
import com.gtrack.auto.pages.TelemetryWidgetSlot
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.track.GeoPoint
import com.gtrack.track.TrackState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Widgets skeuomorphiques 2×2 (style GTI Track Monitor) rendus en bitmap pour [GridTemplate].
 */
class SkeuomorphicGaugeRenderer(private val context: Context) {

    private val titleTypeface: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val valueTypeface: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = titleTypeface
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        typeface = valueTypeface
        textAlign = Paint.Align.LEFT
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 200, 120)
        textSize = 22f
        typeface = valueTypeface
        textAlign = Paint.Align.LEFT
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 18, 18, 18)
        style = Paint.Style.FILL
    }

    /**
     * Grille 3×2 complète (1→6, gauche→droite, haut→bas) pour affichage hero [Pane.setImage].
     */
    fun renderNumberedGridComposite(
        cellSize: Int = COMPOSITE_CELL_PX,
        cols: Int = 3,
        rows: Int = 2
    ): Bitmap {
        val width = cols * cellSize
        val height = rows * cellSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, width, height)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 255, 40, 40)
            strokeWidth = 4f
        }
        for (cellNumber in 1..(cols * rows)) {
            val col = (cellNumber - 1) % cols
            val row = (cellNumber - 1) / cols
            val cell = renderNumberedCell(cellNumber, cellSize)
            val left = col * cellSize
            val top = row * cellSize
            canvas.drawBitmap(cell, left.toFloat(), top.toFloat(), null)
            if (col < cols - 1) {
                val x = (left + cellSize).toFloat()
                canvas.drawLine(x, top.toFloat(), x, (top + cellSize).toFloat(), divider)
            }
            if (row < rows - 1) {
                val y = (top + cellSize).toFloat()
                canvas.drawLine(left.toFloat(), y, (left + cellSize).toFloat(), y, divider)
            }
        }
        return bitmap
    }

    /** Une ligne de la grille (3 cases) — rowIndex 0 = cases 1–3, 1 = cases 4–6. */
    fun renderNumberedGridRow(rowIndex: Int, cellSize: Int = ROW_CELL_PX): Bitmap {
        val cols = 3
        val width = cols * cellSize
        val height = cellSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, width, height)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 255, 40, 40)
            strokeWidth = 4f
        }
        for (col in 0 until cols) {
            val cellNumber = rowIndex * cols + col + 1
            val cell = renderNumberedCell(cellNumber, cellSize)
            val left = col * cellSize
            canvas.drawBitmap(cell, left.toFloat(), 0f, null)
            if (col < cols - 1) {
                val x = (left + cellSize).toFloat()
                canvas.drawLine(x, 0f, x, height.toFloat(), divider)
            }
        }
        return bitmap
    }

    /** Une case de la grille 3×2 avec son numéro (1 = haut gauche, 4 = bas gauche). */
    fun renderNumberedCell(cellNumber: Int, size: Int = 320): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, size, size)
        val frame = RectF(8f, 8f, size - 8f, size - 8f)
        canvas.drawRoundRect(frame, 12f, 12f, cardFillPaint)
        canvas.drawRoundRect(frame, 12f, 12f, borderPaint)

        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 120f
            typeface = valueTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$cellNumber", size / 2f, size / 2f + 20f, numberPaint)

        titlePaint.textSize = 16f
        canvas.drawText("ZONE", size / 2f, size * 0.22f, titlePaint)
        titlePaint.textSize = 22f
        return bitmap
    }

    /**
     * Grille **3 colonnes × 2 lignes** (6 zones égales) en une seule image — layout maîtrisé côté app.
     */
    fun renderSixCellGrid(
        slots: List<TelemetryWidgetSlot>,
        data: TelemetryData,
        track: TrackState
    ): Bitmap {
        val cols = 3
        val rows = 2
        val cellSize = 500
        val width = cols * cellSize
        val height = rows * cellSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, width, height)
        val divider = Paint().apply {
            color = Color.argb(120, 255, 40, 40)
            strokeWidth = 3f
        }
        slots.take(6).forEachIndexed { index, slot ->
            val cell = renderWidget(slot, data, track)
            val col = index % cols
            val row = index / cols
            val scaled = Bitmap.createScaledBitmap(cell, cellSize, cellSize, true)
            val left = col * cellSize
            val top = row * cellSize
            canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
            if (col < cols - 1) {
                val x = (left + cellSize).toFloat()
                canvas.drawLine(x, top.toFloat(), x, (top + cellSize).toFloat(), divider)
            }
            if (row < rows - 1) {
                val y = (top + cellSize).toFloat()
                canvas.drawLine(left.toFloat(), y, (left + cellSize).toFloat(), y, divider)
            }
        }
        return bitmap
    }

    fun renderWidget(
        slot: TelemetryWidgetSlot,
        data: TelemetryData,
        track: TrackState,
        sizePx: Int = 400
    ): Bitmap {
        val size = sizePx.coerceIn(280, 640)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, size, size)
        val frame = RectF(6f, 6f, size - 6f, size - 6f)
        val borderColor = borderColorFor(slot, data)
        borderPaint.color = borderColor
        canvas.drawRoundRect(frame, 14f, 14f, cardFillPaint)
        canvas.drawRoundRect(frame, 14f, 14f, borderPaint)
        drawWidgetContent(canvas, slot, data, size)
        return bitmap
    }

    private fun drawWidgetContent(
        canvas: Canvas,
        slot: TelemetryWidgetSlot,
        data: TelemetryData,
        size: Int
    ) {
        when (slot) {
            TelemetryWidgetSlot.SPEED_ANALOG -> drawSpeedWidget(canvas, size, data.speedKmh)
            TelemetryWidgetSlot.RPM_LED_BAR -> drawRpmWidget(canvas, size, data.rpm)
            TelemetryWidgetSlot.TURBO_ANALOG -> drawTurboWidget(canvas, size, data.turboPressureBar)
            TelemetryWidgetSlot.WATER_TEMP -> drawTempWidget(canvas, size, data.waterTempC, false)
            TelemetryWidgetSlot.OIL_TEMP -> drawTempWidget(canvas, size, data.oilTempC, true)
            TelemetryWidgetSlot.G_FORCE_METER -> drawGForceWidget(canvas, size, data.lateralGForce)
            TelemetryWidgetSlot.AFR_BAR -> drawAfrWidget(canvas, size, data.afr)
            TelemetryWidgetSlot.IGNITION_ADVANCE -> drawTextWidget(
                canvas, size, context.getString(R.string.widget_ignition),
                "%.0f° BTDC".format(data.ignitionAdvanceBtdc)
            )
            TelemetryWidgetSlot.ENGINE_LOAD -> drawVerticalBarWidget(
                canvas, size, context.getString(R.string.widget_engine_load),
                data.engineLoadPercent
            )
            TelemetryWidgetSlot.THROTTLE -> drawVerticalBarWidget(
                canvas, size, context.getString(R.string.widget_throttle),
                data.throttlePercent
            )
            TelemetryWidgetSlot.DELTA_CHRONO -> drawDeltaWidget(canvas, size, data)
            TelemetryWidgetSlot.O2_VOLTAGE -> drawO2Widget(canvas, size, data.o2VoltageV)
            TelemetryWidgetSlot.LAP_TIME -> drawTextWidget(
                canvas,
                size,
                context.getString(R.string.widget_lap_time),
                CarVisualTheme.formatLapTime(data.lapTimeMs)
            )
        }
    }

    fun renderTrackMap(data: TelemetryData, track: TrackState, circuitPoints: List<GeoPoint>): Bitmap {
        val w = 520
        val h = 300
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCarbonBackground(canvas, w, h)
        val frame = RectF(6f, 6f, w - 6f, h - 6f)
        canvas.drawRoundRect(frame, 14f, 14f, cardFillPaint)
        canvas.drawRoundRect(frame, 14f, 14f, borderPaint)

        if (circuitPoints.size < 2) return bitmap

        val bounds = circuitBounds(circuitPoints)
        val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
        }
        val third = circuitPoints.size / 3
        for (i in 0 until circuitPoints.size - 1) {
            val sectorIdx = (i / third).coerceIn(0, 2)
            mapPaint.color = sectorColor(track.sectorFaster.getOrNull(sectorIdx))
            val p1 = toCanvas(circuitPoints[i], bounds, w, h, 36f)
            val p2 = toCanvas(circuitPoints[i + 1], bounds, w, h, 36f)
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, mapPaint)
        }

        val start = toCanvas(circuitPoints.first(), bounds, w, h, 36f)
        drawCheckeredFlag(canvas, start.first, start.second)

        val car = toCanvas(GeoPoint(data.latitude, data.longitude), bounds, w, h, 36f)
        canvas.drawCircle(car.first, car.second, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        canvas.drawCircle(car.first, car.second, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })

        titlePaint.textSize = 20f
        canvas.drawText(context.getString(R.string.track_red_bull_ring), w / 2f, h - 14f, titlePaint)
        return bitmap
    }

    private fun borderColorFor(slot: TelemetryWidgetSlot, data: TelemetryData): Int = when (slot) {
        TelemetryWidgetSlot.OIL_TEMP ->
            if (data.oilTempC >= CarVisualTheme.OIL_TEMP_WARNING_C) Color.rgb(255, 140, 0) else Color.RED
        TelemetryWidgetSlot.WATER_TEMP ->
            if (data.waterTempC >= CarVisualTheme.WATER_TEMP_WARNING_C) Color.rgb(255, 140, 0) else Color.RED
        else -> Color.RED
    }

    private fun drawSpeedWidget(canvas: Canvas, size: Int, speed: Float) {
        drawTitle(canvas, size, context.getString(R.string.widget_speed))
        drawAnalogNeedle(canvas, size * 0.38f, size * 0.58f, size * 0.26f, speed, 0f, 220f, Color.WHITE)
        drawValueRight(canvas, size, "%.0f".format(speed), "km/h", Color.WHITE)
    }

    private fun drawTurboWidget(canvas: Canvas, size: Int, bar: Float) {
        drawTitle(canvas, size, context.getString(R.string.widget_turbo))
        drawAnalogNeedle(canvas, size * 0.38f, size * 0.58f, size * 0.26f, bar, 0f, 1.8f, Color.RED)
        drawValueRight(canvas, size, "%.1f".format(bar), "bar", Color.WHITE)
    }

    private fun drawRpmWidget(canvas: Canvas, size: Int, rpm: Int) {
        drawTitle(canvas, size, context.getString(R.string.widget_rpm))
        drawShiftSegments(canvas, size, rpm, size * 0.40f, size * 0.52f, size - 48f)
        valuePaint.textAlign = Paint.Align.CENTER
        valuePaint.textSize = 36f
        canvas.drawText("%d RPM".format(rpm), size / 2f, size * 0.82f, valuePaint)
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = 44f
    }

    private fun drawTempWidget(canvas: Canvas, size: Int, temp: Float, oil: Boolean) {
        val label = if (oil) context.getString(R.string.widget_oil_temp) else context.getString(R.string.widget_water_temp)
        drawTitle(canvas, size, label)
        drawFluidIcon(canvas, size * 0.32f, size * 0.58f, oil)
        val accent = if (oil && temp >= CarVisualTheme.OIL_TEMP_WARNING_C) {
            Color.rgb(255, 140, 0)
        } else {
            Color.WHITE
        }
        drawValueRight(canvas, size, "%.0f".format(temp), "°C", accent)
    }

    private fun drawGForceWidget(canvas: Canvas, size: Int, gLat: Float) {
        drawTitle(canvas, size, context.getString(R.string.widget_lateral_g))
        val cx = size * 0.35f
        val cy = size * 0.58f
        val r = size * 0.22f
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
        })
        val angle = (gLat.coerceIn(-2f, 2f) / 2f) * (PI / 2).toFloat()
        canvas.drawLine(
            cx, cy,
            cx + sin(angle) * r * 0.9f,
            cy - cos(angle) * r * 0.9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                strokeWidth = 5f
                strokeCap = Paint.Cap.ROUND
            }
        )
        drawValueRight(canvas, size, "%.1f".format(gLat), "G", Color.WHITE)
    }

    private fun drawAfrWidget(canvas: Canvas, size: Int, afr: Float) {
        drawTitle(canvas, size, context.getString(R.string.widget_afr))
        valuePaint.textAlign = Paint.Align.CENTER
        valuePaint.textSize = 48f
        canvas.drawText("%.1f".format(afr), size * 0.38f, size * 0.58f, valuePaint)
        unitPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("AFR", size * 0.58f, size * 0.58f, unitPaint)
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = 44f

        val bar = RectF(36f, size * 0.66f, size - 36f, size * 0.74f)
        val third = bar.width() / 3f
        canvas.drawRect(bar.left, bar.top, bar.left + third, bar.bottom, Paint().apply { color = Color.RED })
        canvas.drawRect(bar.left + third, bar.top, bar.left + 2 * third, bar.bottom, Paint().apply { color = Color.GREEN })
        canvas.drawRect(bar.left + 2 * third, bar.top, bar.right, bar.bottom, Paint().apply { color = Color.BLUE })
        titlePaint.textSize = 14f
        canvas.drawText("Lean", bar.left + third / 2, bar.bottom + 18f, titlePaint)
        canvas.drawText("STOICH", bar.left + 1.5f * third, bar.bottom + 18f, titlePaint)
        canvas.drawText("Rich", bar.left + 2.5f * third, bar.bottom + 18f, titlePaint)
        titlePaint.textSize = 22f
        val markerX = bar.left + ((afr - 10f) / 8f).coerceIn(0f, 1f) * bar.width()
        canvas.drawRect(markerX - 2f, bar.top - 8f, markerX + 2f, bar.bottom + 8f, Paint().apply { color = Color.WHITE })
        val mix = when {
            afr < 14.2f -> "Lean" to Color.RED
            afr > 15.2f -> "Rich" to Color.rgb(100, 180, 255)
            else -> "STOICH" to Color.GREEN
        }
        unitPaint.color = mix.second
        canvas.drawText(mix.first, size * 0.72f, size * 0.58f, unitPaint)
        unitPaint.color = Color.rgb(0, 200, 120)
    }

    private fun drawVerticalBarWidget(canvas: Canvas, size: Int, title: String, percent: Float) {
        drawTitle(canvas, size, title)
        val barH = size * 0.42f
        val barW = 28f
        val left = 44f
        val top = size * 0.40f
        val filled = (percent / 100f * barH).coerceIn(0f, barH)
        canvas.drawRoundRect(left, top, left + barW, top + barH, 6f, 6f, Paint().apply { color = Color.DKGRAY })
        canvas.drawRoundRect(
            left, top + barH - filled, left + barW, top + barH, 6f, 6f,
            Paint().apply { color = Color.LTGRAY }
        )
        titlePaint.textSize = 16f
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("%.0f%%".format(percent), left + barW / 2, top + barH + 20f, titlePaint)
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 22f
        drawValueRight(canvas, size, "%.0f".format(percent), "%", Color.WHITE)
    }

    private fun drawTextWidget(canvas: Canvas, size: Int, title: String, value: String) {
        drawTitle(canvas, size, title)
        valuePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, size / 2f, size * 0.58f, valuePaint)
        valuePaint.textAlign = Paint.Align.LEFT
    }

    private fun drawO2Widget(canvas: Canvas, size: Int, volts: Float) {
        drawTitle(canvas, size, context.getString(R.string.widget_o2))
        valuePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("%.2fV".format(volts), size / 2f, size * 0.55f, valuePaint)
        valuePaint.textAlign = Paint.Align.LEFT
        val lambda = 0.7f + volts * 0.35f
        unitPaint.textAlign = Paint.Align.CENTER
        val tag = when {
            lambda < 0.95f -> "Lean" to Color.RED
            lambda > 1.05f -> "Rich" to Color.rgb(100, 180, 255)
            else -> "STOICH" to Color.GREEN
        }
        unitPaint.color = tag.second
        canvas.drawText("%.2fλ  ${tag.first}".format(lambda), size / 2f, size * 0.72f, unitPaint)
        unitPaint.color = Color.rgb(0, 200, 120)
        unitPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawDeltaWidget(canvas: Canvas, size: Int, data: TelemetryData) {
        drawTitle(canvas, size, context.getString(R.string.widget_delta_chrono))
        val slow = data.deltaChronoMs > 0
        val color = if (slow) Color.RED else Color.GREEN
        val sign = if (data.deltaChronoMs >= 0) "+" else ""
        valuePaint.textAlign = Paint.Align.CENTER
        valuePaint.color = color
        canvas.drawText("%s%.1fs".format(sign, data.deltaChronoMs / 1000f), size / 2f, size * 0.55f, valuePaint)
        canvas.drawText(if (slow) "SLOW" else "FAST", size / 2f, size * 0.72f, valuePaint)
        valuePaint.color = Color.WHITE
        valuePaint.textAlign = Paint.Align.LEFT
    }

    private fun drawTitle(canvas: Canvas, size: Int, text: String) {
        canvas.drawText(text, size / 2f, 34f, titlePaint)
    }

    private fun drawValueRight(canvas: Canvas, size: Int, value: String, unit: String, valueColor: Int) {
        valuePaint.color = valueColor
        val x = size * 0.52f
        val y = size * 0.58f
        canvas.drawText(value, x, y, valuePaint)
        val w = valuePaint.measureText(value)
        canvas.drawText(unit, x + w + 6f, y, unitPaint)
        valuePaint.color = Color.WHITE
    }

    private fun drawAnalogNeedle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        value: Float,
        min: Float,
        max: Float,
        color: Int
    ) {
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, 135f, 270f, false, arc)
        val sweep = 270f * ((value - min) / (max - min)).coerceIn(0f, 1f)
        arc.color = color
        canvas.drawArc(rect, 135f, sweep, false, arc)
        val angleRad = Math.toRadians((135.0 + sweep).toDouble()).toFloat()
        val nx = cx + cos(angleRad) * r * 0.82f
        val ny = cy + sin(angleRad) * r * 0.82f
        canvas.drawLine(cx, cy, nx, ny, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        })
    }

    private fun drawShiftSegments(
        canvas: Canvas,
        size: Int,
        rpm: Int,
        top: Float,
        left: Float,
        width: Float
    ) {
        val segments = 16
        val lit = (rpm.toFloat() / CarVisualTheme.RPM_REDLINE * segments).toInt().coerceIn(0, segments)
        val segW = width / segments
        for (i in 0 until segments) {
            val paint = Paint().apply {
                color = when {
                    i < lit && i < segments / 3 -> Color.GREEN
                    i < lit && i < 2 * segments / 3 -> Color.YELLOW
                    i < lit -> Color.RED
                    else -> Color.rgb(40, 40, 40)
                }
            }
            canvas.drawRect(left + i * segW, top, left + (i + 1) * segW - 2f, top + 22f, paint)
        }
    }

    private fun drawFluidIcon(canvas: Canvas, cx: Float, cy: Float, oil: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        if (oil) {
            canvas.drawOval(cx - 18f, cy - 14f, cx + 18f, cy + 10f, p)
            canvas.drawRect(cx - 22f, cy - 22f, cx - 10f, cy - 10f, p)
        } else {
            canvas.drawRect(cx - 8f, cy - 20f, cx + 8f, cy + 16f, p)
            canvas.drawLine(cx - 20f, cy + 8f, cx + 20f, cy + 8f, p)
        }
    }

    private fun drawCarbonBackground(canvas: Canvas, w: Int, h: Int) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.bg_carbon_fiber)
        if (drawable != null) {
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
        } else {
            canvas.drawColor(Color.parseColor("#0A0A0A"))
        }
    }

    private fun drawCheckeredFlag(canvas: Canvas, x: Float, y: Float) {
        val s = 10f
        val p = Paint()
        for (row in 0..1) {
            for (col in 0..1) {
                p.color = if ((row + col) % 2 == 0) Color.WHITE else Color.BLACK
                canvas.drawRect(x + col * s, y + row * s, x + (col + 1) * s, y + (row + 1) * s, p)
            }
        }
    }

    private fun circuitBounds(points: List<GeoPoint>): FloatArray {
        var minLat = points[0].latitude.toFloat()
        var maxLat = points[0].latitude.toFloat()
        var minLon = points[0].longitude.toFloat()
        var maxLon = points[0].longitude.toFloat()
        points.forEach {
            minLat = min(minLat, it.latitude.toFloat())
            maxLat = max(maxLat, it.latitude.toFloat())
            minLon = min(minLon, it.longitude.toFloat())
            maxLon = max(maxLon, it.longitude.toFloat())
        }
        return floatArrayOf(minLat, maxLat, minLon, maxLon)
    }

    private fun toCanvas(
        point: GeoPoint,
        bounds: FloatArray,
        w: Int,
        h: Int,
        padding: Float
    ): Pair<Float, Float> {
        val latSpan = (bounds[1] - bounds[0]).coerceAtLeast(0.0001f)
        val lonSpan = (bounds[3] - bounds[2]).coerceAtLeast(0.0001f)
        val x = padding + ((point.longitude.toFloat() - bounds[2]) / lonSpan) * (w - 2 * padding)
        val y = padding + (1f - (point.latitude.toFloat() - bounds[0]) / latSpan) * (h - 2 * padding)
        return x to y
    }

    private fun sectorColor(faster: Boolean?): Int = when (faster) {
        true -> Color.GREEN
        false -> Color.RED
        null -> Color.GRAY
    }

    companion object {
        const val COMPOSITE_CELL_PX = 512
        const val ROW_CELL_PX = 640
    }
}
